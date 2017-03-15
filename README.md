PCF Developers workshop
==

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Introduction](#introduction)
- [Pivotal Cloud Foundry Technical Overview](#pivotal-cloud-foundry-technical-overview)
	- [Lab - Run Spring boot app](#run-spring-boot-app)
	- [Lab - Run web site](#run-web-site)
- [Deploying simple apps](#deploy-spring-boot-app)
  - [Lab - Deploy Spring boot app](#deploy-spring-boot-app)
  - [Lab - Deploy web site](#deploy-web-site)
- [Cloud Foundry services](#cloud-foundry-services)
 	- [Load flights from a database](#load-flights-from-a-database)
	- [Retrieve fares from an external application](#retrieve-fares-from-an-external-application)

<!-- /TOC -->
# Introduction

`git clone https://github.com/MarcialRosales/java-pcf-workshops.git`

# Pivotal Cloud Foundry Technical Overview

Reference documentation:
- [Elastic Runtime concepts](http://docs.pivotal.io/pivotalcf/1-9/concepts/index.html)


## Run Spring boot app
We have a spring boot application which provides a list of available flights based on some origin and destination.

You can use the existing code or
1. `git checkout master`
2. `cd java-pcf-workshops/apps/flight-availability`
3. `mvn spring-boot:run`
4. `curl 'localhost:8080?origin=MAD&destination=FRA'`

We would like to make this application available to our clients. How would you do it today?

## Run web site
We also want to deploy the Maven site associated to our flight-availability application so that the team can check the latest java unit reports and/or the javadocs.

1. `git checkout master`
2. `cd java-pcf-workshops/apps/flight-availability`
3. `mvn site:run`
4. Go to your browser, and check out this url `http://localhost:8080`

We would like to make this application available only within our organization, i.e. not publicly available to our clients. How would you do it today?

# Deploying simple apps

Reference documentation:
- [Using Apps Manager](http://docs.pivotal.io/pivotalcf/1-9/console/index.html)
- [Using cf CLI](http://docs.pivotal.io/pivotalcf/1-9/cf-cli/index.html)
- [Deploying Applications](http://docs.pivotal.io/pivotalcf/1-9/devguide/deploy-apps/deploy-app.html)
- [Deploying with manifests](http://docs.pivotal.io/pivotalcf/1-9/devguide/deploy-apps/manifest.html)

## Deploy Spring boot app
Deploy flight availability and make it publicly available on a given public domain

1. `git checkout master`
2. `cd java-pcf-workshops/apps/flight-availability`
3. Build the app  
  `mvn install`
4. Deploy the app
  `cf push flight-availability -p target/flight-availability-0.0.1-SNAPSHOT.jar --random-route`
5. Try to deploy the application using a manifest
6. Check out application's details, whats the url?
  `cf app flight-availability`  
7. Check out the health of the application ([actuator](https://github.com/MarcialRosales/java-pcf-workshops/blob/master/apps/flight-availability/pom.xml#L37-L40)):  
  `curl <url>/health`

## Deploy web site
Deploy Maven site associated to the flight availability and make it internally available on a given private domain

1. `git checkout master`
2. `cd java-pcf-workshops/apps/flight-availability`
3. Build the site
  `mvn site`
4. Deploy the app
  `cf push flight-availability-site -p target/site --random-route`
5. Check out application's details, whats the url?
  `cf app flight-availability-site`  

# Cloud Foundry services

## Load flights from a database

### Code walk-thru


### Build, Deploy and Test the application

We want to load the flights from a relational database (mysql) provisioned by the platform. We are implementing the `FlightService` interface so that we can load them from a `FlightRepository`. We need to convert `Flight` to a *JPA Entity*. We [added](https://github.com/MarcialRosales/java-pcf-workshops/blob/load-flights-from-db/apps/flight-availability/pom.xml#L41-L49) **hsqldb** a *runtime dependency* so that we can run it locally.

1. `git checkout load-flights-from-db`
2. `cd apps/flight-availability`
3. Run the app
  `mvn spring-boot:run`
4. Test it
  `curl 'localhost:8080?origin=MAD&destination=FRA'` shall return `[{"id":2,"origin":"MAD","destination":"FRA"}]`

5. Before we deploy our application to PCF we need to provision a mysql database.
  `cf marketplace`  Check out what services are available
  `cf marketplace -s p-mysql `  Check out the service details like available plans
  `cf create-service p-mysql pre-existing-plan flight-repository`   Create a service instance
  `cf service ...`  Check out the service instance. Is it ready to use?

6. Push the application using the manifest.  
  `cf push flight-availability -f target/manifest.yml`

7. See the manifest and observe we have declare a service:  
  ```
  applications:
  - name: flight-availability
    instances: 1
    memory: 1024M
    path: target/@project.build.finalName@.@project.packaging@
    random-route: true
    services:
    - flight-repository

  ```
7. Check out the database credentials the application is using
  `cf env flight-availability`

8. Test the application. Whats the url?

9. We did not include any jdbc drivers with the application. How could it that it works?

## Retrieve fares from an external application

We are going to extend our flight availability application we implemented in the branch `load-flights-from-db`. This time we are working off branch `load-fares-from-external-app`.

The idea is this: To return flights with fares we retrieve the flights from the db (like we did before) and pass those flights to the fare-service to get the fares before returning them to the caller.
```
		--[1]--(rest api)--->[flight-availability]---[3]-(rest api)---->[fare-service]
																|
																+---[2]-(FlightRepository)-->[MySql db]
```


### Code walk-thru

1. Change the [FlightServiceImpl]() so that it calls the [FareService](). We build a instance of [FareServiceImpl]() from a uniquely identified **RestTemplate**.
	```
	@Service
	public class FareServiceImpl implements FareService {

		private final RestTemplate restTemplate;

		public FareServiceImpl(@Qualifier("fareService") RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
		}

		@Override
		public String[] fares(Flight[] flights) {

			 return restTemplate.postForObject("/", flights, String[].class);

		}

	}
	``` 	
2. We need to configure somehow where the FareService runs and how to connect to it (i.e. credentials). For now, we use spring boot configuration and we have this class in the [FlightAvailabilityApplication]().
	```
	@Configuration
	@ConfigurationProperties(prefix = "fare-service")
	class FareServiceConfig {
		String uri;
		String username;
		String password;
		public String getUri() {
			return uri;
		}
		public void setUri(String uri) {
			this.uri = uri;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}

		@Bean(name = "fareService")
		public RestTemplate fareService(RestTemplateBuilder builder, FareServiceConfig fareService) {
			return builder.basicAuthorization(getUsername(), getPassword()).rootUri(getUri()).build();
		}
	}
	```
  And the settings under `src/main/resources/application.yml`
	```
	fare-service:
	uri: http://localhost:8081
	username: user
	password: password
	```


### Build and Test the application

1. `git checkout load-fares-from-external-app`
2. `cd apps/flight-availability`
3. Run the app from one terminal  
	`mvn spring-boot:run`
4. From another terminal. The fare-service runs on port 8081.  
	`cd apps/fare-service`
	`mvn spring-boot:run`
5. Test it
  `curl 'localhost:8080?origin=MAD&destination=FRA'` shall return `[{"id":2,"origin":"MAD","destination":"FRA"}]`  
  `curl 'localhost:8080/fares?origin=MAD&destination=FRA'` shall return `[{"fare":"0.8255260037921347", "id":2,"origin":"MAD","destination":"FRA"}]`

### Deploy and Test the application

1. Build fare-service  
 	`mvn install`
2. Deploy fare-service using the manifest  
	`cf push fare-service -f target/manifest.yml`  
	`cf app fare-service` Check out the url where it is listening
3. Build flight-availability  
	`mvn install`
4. Deploy flight-availability using the manifest. Do we need to make any changes before we deploy?
	`cf push flight-availability -f target/manifest.yml`
	`cf app flight-availability`  check out the url
	`curl '<url>/fares?origin=MAD&destination=FRA'` does it work?
5. It does not work because flight-availability is using `http://localhost:8081` as the fare-service url.
6. We can fix this issue by setting the proper environment variables in the manifest:
	```
	applications:
	- name: flight-availability
	  instances: 1
	  memory: 1024M
	  path: target/@project.build.finalName@.@project.packaging@
	  random-route: true
	  services:
	  - flight-repository
		env:
		FARE_SERVICE_URI: <url of fare-service>
		FARE_SERVICE_USERNAME: username
		FARE_SERVICE_PASSWORD: password
	```
	And spring boot will convert those environment variables into properties. It works but it not elegant and every application that needs to talk to the FareService needs to be configured with all these credentials. Far from ideal.
7. We can fix this issue we need to do the following:  	
	- Create a **User Provided Service** that encapsulates the uri, username and password required to connect to the FareService.  
		`cf cups fare-service -p '{"uri":"<uri of fare-service app>","username":"username", "password":"password" }'`
	- Create a ServiceInfoCreator that allows us to retrieve the fare-service's credentials from `VCAP_SERVICES` rather than from the `application.yml`
8. Now, we declare this service in the flight-availability application's manifest.
	```
	applications:
	- name: flight-availability
	  instances: 1
	  memory: 1024M
	  path: target/@project.build.finalName@.@project.packaging@
	  random-route: true
	  services:
	  - flight-repository  # has to match the service instance or user provided service you have created in PCF
	  - fare-service

	```
9. Deploy the application and check the credentials of the fare-service
	`cf push flight-availability -f target/manifest.yml`
	`cf env flight-availability`
