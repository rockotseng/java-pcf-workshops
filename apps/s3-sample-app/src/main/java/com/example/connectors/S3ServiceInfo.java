package com.example.connectors;

import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.cloud.service.ServiceInfo.ServiceProperty;

import amazon.s3.AmazonProperties;

@ServiceInfo.ServiceLabel("s3")
public class S3ServiceInfo extends BaseServiceInfo {

	private AmazonProperties aws;
	
	public S3ServiceInfo(String id, String accessKey, String secret, String region, String defaultBucket)  {
		super(id);
		
		aws = new AmazonProperties();
		aws.setAws(new AmazonProperties.Aws(accessKey, secret, region));
		aws.setS3(new AmazonProperties.S3(defaultBucket));
		
	}

	public AmazonProperties getAWS() {
		return aws;
	}

	@ServiceProperty(category = "aws", name = "access-key-id")
	public String getAccessKeyId() {
		return aws.getAws().getAccessKeyId();
	}
	@ServiceProperty(category = "aws", name = "access-key-secret")
	public String getAccessKeySecret() {
		return aws.getAws().getAccessKeySecret();
	}
	
	
}
