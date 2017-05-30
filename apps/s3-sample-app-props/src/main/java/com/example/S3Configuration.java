package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import amazon.s3.AmazonProperties;
import amazon.s3.AmazonS3Template;

@Configuration
@EnableConfigurationProperties(AmazonProperties.class)
public class S3Configuration {

    @Autowired
    private AmazonProperties amazonProperties;

    @Bean
    AmazonS3Template amazonS3Template() {
        return new AmazonS3Template(amazonProperties);
    }
}