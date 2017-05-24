Buildpacks
===

- What they are - instructions on how to build an application image
- When they are used - *push* -> **staging** -> *run*
- How they work - 3 scripts:  detect, compile and release
- Where dependencies come from? - offline vs online
- As a Java developer, how do I customize it? Configure vs Extend
- ***The buildpack API is open-ended. If you can script it, you can do it***.

# Introduction

**Reference documentation**:
- [Custom Buildpacks](http://docs.pivotal.io/pivotalcf/buildpacks/custom.html)

Only administrators are allowed to manage build packs. This means adding new ones, update them, change the order, and delete them. We can check what build packs are available by running `cf buildpacks`.

However, developers can specify the URI to a git repository where it resides a custom build pack. Although administrators can disable this option too.

There are 2 ways of customizing build packs so that they produce the application's image we need:
 - Pass some settings to the `cf push` command that overrides the default values provided by the buildpack
 - Create a custom version of a build pack and use that buildpack to push applications rather than the standard one

# Examples of how to configure JBP

We can override the default configuration used by the JBP. Every component in the buildpack has a yml configuration file stored under https://github.com/cloudfoundry/java-buildpack/config. If we open the file `open_jdk_jre.yml` we see the following content:

```
---
jre:
  version: 1.8.0_+
  repository_root: "{default.repository.root}/openjdk/{platform}/{architecture}"
jvmkill_agent:
  version: 1.+
  repository_root: "{default.repository.root}/jvmkill/{platform}/{architecture}"
memory_calculator:
  version: 3.+
  repository_root: "{default.repository.root}/memory-calculator/{platform}/{architecture}"
  stack_threads: 300
```

To override this configuration we set an environment variable
	1. the variable name should match the configuration file we wish to override minus the `.yml` extension and with a prefix of `JBP_CONFIG`.
	2. the variable value should be a valid inline yaml compliant with the yaml expected by the configuration we are overriding.

Say we want to force a given JRE version and/or hint the memory calculator to use 200 threads to calculate the amount of memory allocated to the stack. The name of the variable would be `JBP_CONFIG_OPEN_JDK_JRE`.

`cf set-env my-application JBP_CONFIG_OPEN_JDK_JRE '{ jre: { version: 1.8.0_+ }, memory_calculator: { stack_threads: 200 } }'`

Another example of configuring the JBP is to pass some arguments to the *Java Main program*.

`cf set-env my-application JBP_CONFIG_JAVA_MAIN '{ arguments: "-server.port=\$PORT -foo=bar" }' `

Although a simpler way to pass those flags is also thru JAVA_OPTS:
`cf set-env my-application JAVA_OPTS "-foo=bar" `

Another example is to choose the tomcat version we want to use. In this case we are using an application manifest:
```
env:
	JBP_CONFIG_TOMCAT: '{ tomcat: { version: 8.0.+ } }'
```

# Memory Calculations for Java applications done by Java Buildpack

We are used to launch our java applications (either JEE containers or standalone applications) with a number of `-X` flags, like the maximum amount of heap size, perm gem size or metadata space size, and others.

The maximum of heap size is normally configured within the limits of the available physical RAM in the machine. In Cloud Foundry there is no difference, but rather having us to do it, Cloud Foundry does it for us. We only need to specify the amount of memory we want to allocate to the container where the application runs and the Java buildpack calculates the corresponding `-Xmx` and other memory region's sizes.

*Memory regions*:
-	Heap (-Xmx)
-	Metaspace (-XX:MaxMetaspaceSize) (native OS memory since Java 8 where interim String and Java class metadata is stored in chunks, one per class loader)
-	Thread Stacks (-Xss)
-	Direct Memory (-XX:MaxDirectMemorySize)
-	Code Cache (-XX:ReservedCodeCacheSize)
-	Compressed Class Space (-XX:CompressedClassSpaceSize)

PCF/JavaBuildPack will use a set of defaults that it is believed will suffice most applications. It optimizes for all non-heap memory regions and leaves the reminder for the heap. But the amount of metadata allocated for an application is application-dependent and general guidelines do not exist to determine the maximum size.

## Metaspace
We can tune the size of any memory region when we push our applications (`cf set-env <app> JAVA_OPTS ‘-XX:MaxMetaspaceSize=50M’` or thru the manifest file).

By default, the JBP will estimate the number of classes based on the number of class files:
- Estimated metaspace: 5400 * #loadedClasses + 7000000
- Estimated Code cache: 1500 * #loadedClasses + 5000000
- Estimated compressed class space: 700 * #loadedclasses + 750000

However we can indicate how many classes :
`cf set-env my-application JBP_CONFIG_OPEN_JDK_JRE '{ memory_calculator: { class_count: 1000 } }'`

## Heap
Unless we specify the maximum heap size (`-Xmx`), the java build pack sets it according to the total memory available to the container. However, take into account that if we increase the size of other memory regions like “direct memory region” from 10Mb to 20Mb, this ultimately leaves us with less memory to allocate to the heap.

## Thread Stack
We can control how much memory is allocated to stacks by setting the amount of memory per thread (`-Xss`). If we want to be more precise we can indicate how many threads should be used for the calculation of the stack memory:
`cf set-env my-application JBP_CONFIG_OPEN_JDK_JRE '{ memory_calculator: { stack_threads: 200 } }'`

Default (we don’t provide any settings) memory calculation is done (Assuming Java 8):
-	`-XX:MaxDirectMemorySize: 10Mb`
-	`-XX:MaxMetaspaceSize` and `–XX:CompressedClassSpaceSize` based on number of classes that will be loaded
-	`-XX:ReservedCodeCacheSize: 240M`
-	`-Xss: 1M` and determine total memory based on `–Xss` and `300` threads
-	`-Xmx` to total memory minus above values

At this point we can either allocated more memory and/or adjust the size of specific regions.


## Tracking memory calculation done by JBP

To track what memory settings the JBP has chosen for our application we look at these items:

-	Input to the memory calculation are logged while staging the app:
`Loaded Classes: 13974, Threads: 300, JAVA_OPTS: ''`
-	Calculated memory is logged using `cf push` or `cf scale`:
```
  state     since                    cpu    memory       disk         details #0   running   2017-04-10 02:20:03 PM   0.0%   896K of 1G   1.3M of 1G
```
-	And the actual JRE settings are logged too:
```
JVM Memory Configuration: -XX:MaxDirectMemorySize=10M -XX:MaxMetaspaceSize=99199K \     -XX:ReservedCodeCacheSize=240M -XX:CompressedClassSpaceSize=18134K -Xss1M -Xmx368042K
```

## Troubleshooting memory issues

*OutOfMemoryError*: Produced when we cannot allocate an object in the heap and the GC cannot make free up more space and the heap cannot be expanded. It may also occur where there is insufficient native memory to support loading of a Java class. In rare instances, it can be thrown when GC is spending too long and little memory is being freed. So, when we get a java.lang.OutOfMemoryError we need to determine the origin: java heap is full, or native memory is exhausted? The actual exception message indicates the source.

When an OOM happens we loose the thread-dump because a) the container is destroyed and b) the container filesystem may not have enough disk to dump all the applications’s memory to disk.
The solution the JBP took was to use `jvmkill` agent which is a JVMTI (JVM Tool Interface, http://docs.oracle.com/javase/8/docs/technotes/guides/jvmti/) agent that forcibly terminates the JVM when it is unable to allocate memory or create a thread. This agent will dump the java heap using  `–XX:+HeapDumpOnoutOfMemoryError` JVM argument. This agent will be notified and terminate the JVM after the heap dump completes. It dumps a histogram:
```
ResourceExhausted! (1/0) | Instance Count | Total Bytes | Class Name                 | | 17802          | 305280896   | [B                         | | 47937          | 7653024     | [C                         | | 14634          | 1287792     | Ljava/lang/reflect/Method; | | 46718          | 1121232     | Ljava/lang/String;         | | 8436           | 940992      | Ljava/lang/Class;          | ... Memory usage:    Heap memory: init 373293056, used 324521536, committed 327155712, max 331874304    Non-heap memory: init 2555904, used 64398104, committed 66191360, max 377069568 Memory pool usage:    Code Cache: init 2555904, used 15944384, committed 16384000, max 251658240    Metaspace: init 0, used 43191560, committed 44302336, max 106274816    Compressed Class Space: init 0, used 5262320, committed 5505024, max 19136512    PS Eden Space: init 93847552, used 41418752, committed 41418752, max 46137344    PS Survivor Space: init 15204352, used 34072320, committed 36700160, max 36700160    PS Old Gen: init 249036800, used 249034640, committed 249036800, max 249036800
```

## Metaspace GC
Proper monitoring & tuning of the Metaspace will obviously be required in order to limit the frequency or delay of such garbage collections. Excessive Metaspace garbage collections may be a symptom of classes, classloaders memory leak or inadequate sizing for your application. Back in Java 7, when we had –XX:MaxPermSize we could fix the size but it was difficult to tune and it was fixed at startup. Max metaspace size is unlimited by default.

## Developer Diagnostic Tools (https://content.pivotal.io/blog/new-cloud-foundry-java-buildpack-improves-developer-diagnostic-tools)

Note: Only in very edge cases where there is no way to reproduce in a local environment the issues we see when running in PCF, there are diagnostics tools to get more insights of the application’s runtime. The diagnostics tools are:
-	Use JRE developer tools like jmap, jstat, jcmd
-	JMX access from outside PCF
-	Remote debugging from eclipse or other IDE
-	Use a Profiler like YourKit
All these tools require ssh access into the container where application is running and ssh tunneling.
In addition to these tools, there are other application monitoring tools like AppDynamics or NewRelic which permanently monitor our applications thru one their agents.


# Adding functionality

We are going to customize the Java Build pack so that it declares a Java system property `staging.timestamp` with the timestamp when the application was staged.


1. Fork the Cloud Foundry Java buildpack from github
2. Clone your fork
3. Open the buildpack in your preferred editor
4. We add a *framework* component that will set the Java system property. To do that, first create `java-buildpack/lib/java_buildpack/framework/staging_timestamp.rb` and add the following contents:
	```
	require 'java_buildpack/framework'

	module JavaBuildpack::Framework

	  # Adds a system property containing a timestamp of when the application was staged.
	  class StagingTimestamp < JavaBuildpack::Component::BaseComponent
	    def initialize(context)
	      super(context)
	    end

	    def detect
	      'staging-timestamp'
	    end

	    def compile
	    end

	    def release
	      @droplet.java_opts.add_system_property('staging.timestamp', "'#{Time.now}'")
	    end
	  end
	end
	```
5. Next we activate our new component by adding it to `java-buildpack/config/components.yml` as seen here:
	```
	frameworks:
	  - "JavaBuildpack::Framework::AppDynamicsAgent"
	  - "JavaBuildpack::Framework::JavaOpts"
	  - "JavaBuildpack::Framework::MariaDbJDBC"
	  - "JavaBuildpack::Framework::NewRelicAgent"
	  - "JavaBuildpack::Framework::PlayFrameworkAutoReconfiguration"
	  - "JavaBuildpack::Framework::PlayFrameworkJPAPlugin"
	  - "JavaBuildpack::Framework::PostgresqlJDBC"
	  - "JavaBuildpack::Framework::SpringAutoReconfiguration"
	  - "JavaBuildpack::Framework::SpringInsight"
	  - "JavaBuildpack::Framework::StagingTimestamp" #Here's the bit you need to add!
	```
6. Commit your changes and push it to your repo
7. Push an application that uses your build pack and test that the buildpack did its job.
 	- `git checkout load-flights-from-in-memory-db`
 	- `cd apps/flight-availability`
 	- `cf push -f target/manifest.yml -b https://github.com/MarcialRosales/java-buildpack`
 	- `curl https://<your_app_uri/env | jq . | grep "staging.timestamp"`


## Debugging JBP

By default the buildpack is very quiet, only informing you about big things going right.  Behind the scenes however, it logs an incredible amount of information about each attempt at staging. We can enable extra logging during the staging process :

```bash
cf set-env <APP> JBP_LOG_LEVEL DEBUG
```

## How do I test it?

There are 3 ways to test a buildpack:
- Refer to the buildpack repo whehn we push the application however this option may be disabled in your PCF installation by your sys admin
- Use PCFDev and operate like a system administrator, i.e. `cf create-buildpack` or `cf update-buildpack`.
- Run buildpack locally:
	```bash
	JBP_LOG_LEVEL=DEBUG <BUILDPACK-CLONE>/bin/detect .
	JBP_LOG_LEVEL=DEBUG <BUILDPACK-CLONE>/bin/compile . $TMPDIR
	JBP_LOG_LEVEL=DEBUG <BUILDPACK-CLONE>/bin/release .
	```


# Changing functionality

The Java buildpack in particular is highly customizable and there is an environment setting for almost every aspect of the buildpack. However, for those rare occasions, we are going to demonstrate how we can change some functionality such as fixing the JRE version. By default, the Java build pack downloads the latest JRE patch version, i.e. 1.8.0_x.

We will update our build pack to utilize java 1.8.0_25 rather than simply the latest 1.8.

1. Change `java-buildpack/config/open_jdk_jre.yml` as shown:
	```
	repository_root: "{default.repository.root}/openjdk/{platform}/{architecture}"
	version: 1.8.0_+ # becomes 1.8.0_25
	memory_sizes:
	  metaspace: 64m.. # permgen becomes metaspace
	memory_heuristics:
	  heap: 85
	  metaspace: 10 # permgen becomes metaspace
	  stack: 5
	  native: 10
	```
2. Commit and push
3. Push the application again with this build pack and check in the staging logs that we are using JRE 1.8.0_25.
