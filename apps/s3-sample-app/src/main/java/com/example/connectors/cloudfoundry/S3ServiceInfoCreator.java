package com.example.connectors.cloudfoundry;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator;
import org.springframework.cloud.cloudfoundry.Tags;

import com.example.connectors.S3ServiceInfo;

public class S3ServiceInfoCreator extends CloudFoundryServiceInfoCreator<S3ServiceInfo> {

	private static final Tags tags = new Tags("S3", "s3");
	
	
	public S3ServiceInfoCreator() {
		super(tags, "s3");
	}

	public boolean accept(Map<String, Object> serviceData) {
		return tagsMatch(serviceData) || tagsMatchInCredentials(serviceData);
	}

	@SuppressWarnings("unchecked")
	protected boolean tagsMatch(Map<String, Object> serviceData) {
		List<String> serviceTags = (List<String>) serviceData.get("tags");
		return tags.containsOne(serviceTags);
	}
	
	@SuppressWarnings("unchecked")
	protected boolean tagsMatchInCredentials(Map<String, Object> serviceData) {
		List<String> serviceTags = (List<String>) getCredentials(serviceData).get("tags");
		return tags.containsOne(serviceTags);
	}
	
	public S3ServiceInfo createServiceInfo(Map<String, Object> serviceData) {
		Map<String, Object> credentials = getCredentials(serviceData);
			
		return new S3ServiceInfo(getId(serviceData), String.valueOf(credentials.get("aws-key-id")), 
				String.valueOf(credentials.get("aws-key-secret")),
				String.valueOf(credentials.get("aws-region")),
				String.valueOf(credentials.get("default-bucket")));
	}
	@SuppressWarnings("unchecked")
	protected Map<String, Object> getCredentials(Map<String, Object> serviceData) {
		return (Map<String, Object>) serviceData.get("credentials");
	}
	protected String getId(Map<String, Object> serviceData) {
		return (String) serviceData.get("name");
	}

}
