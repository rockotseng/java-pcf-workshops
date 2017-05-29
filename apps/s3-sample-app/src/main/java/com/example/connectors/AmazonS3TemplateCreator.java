package com.example.connectors;

import org.springframework.cloud.service.AbstractServiceConnectorCreator;
import org.springframework.cloud.service.ServiceConnectorConfig;

import amazon.s3.AmazonS3Template;

public class AmazonS3TemplateCreator extends AbstractServiceConnectorCreator<AmazonS3Template, S3ServiceInfo> {

	
	public AmazonS3Template create(S3ServiceInfo serviceInfo, ServiceConnectorConfig serviceConnectorConfig) {
		return new AmazonS3Template(serviceInfo.getAWS());
//		return new AmazonS3Template(serviceInfo.getAWS().getS3().getDefaultBucket(),
//				serviceInfo.getAWS().getAws().getAccessKeyId(), serviceInfo.getAWS().getAws().getAccessKeySecret());
	}

}
