package com.example;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import amazon.s3.AmazonS3Template;

@RestController
@RequestMapping("/s3")
public class AmazonS3Controller {

	private AmazonS3Template amazonS3Template;
    private String bucketName;

    @Autowired
    public AmazonS3Controller(AmazonS3Template amazonS3Template) {
        this.amazonS3Template = amazonS3Template;
        this.bucketName = amazonS3Template.getDefaultBucket();
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<Resource<S3ObjectSummary>> getBucketResources() {

        ObjectListing objectListing = amazonS3Template.getAmazonS3Client()
                .listObjects(new ListObjectsRequest()
                        .withBucketName(bucketName));

        return objectListing.getObjectSummaries()
                .stream()
                .map(a -> new Resource<>(a,
                        new Link(String.format("https://s3.amazonaws.com/%s/%s",
                                a.getBucketName(), a.getKey())).withRel("url")))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "upload", method = RequestMethod.POST)
    public
    @ResponseBody
    String handleFileUpload(@RequestParam("name") String name,
                            @RequestParam("file") MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentType(file.getContentType());

                // Upload the file for public read
                amazonS3Template.getAmazonS3Client().putObject(new PutObjectRequest(bucketName, name, file.getInputStream(), objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));

                return "You successfully uploaded " + name + "!";
            } catch (Exception e) {
                return "You failed to upload " + name + " => " + e.getMessage();
            }
        } else {
            return "You failed to upload " + name + " because the file was empty.";
        }
    }
}
