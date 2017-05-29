package com.example.connectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import amazon.s3.AmazonProperties;
import amazon.s3.AmazonS3Template;

@Configuration
@Profile("cloud")
public class S3CloudConfiguration {

	@Bean
	public Cloud cloud() {
		return new CloudFactory().getCloud();
	}

	@Bean
	@ConfigurationProperties(AmazonProperties.PREFIX)
	public AmazonS3Template s3() {
		return cloud().getSingletonServiceConnector(AmazonS3Template.class, null);
	}

}
