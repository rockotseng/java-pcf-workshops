package amazon.s3;

import java.io.File;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;

/**
 * This class is a client for interacting with Amazon S3 bucket resources.
 *
 * 
 */
@Component
public class AmazonS3Template {

//	private final Logger log = LoggerFactory.getLogger(AmazonS3Template.class);
	
    private AmazonProperties.Aws aws;
    private AmazonProperties.S3 s3;
    private Credentials sessionCredentials;

    public AmazonS3Template(AmazonProperties properties) {
    	this.aws = properties.getAws();
    	this.s3 = properties.getS3();
    	System.out.println("===== " + aws.getAccessKeyId()  + " " + aws.getAccessKeySecret() );
    }
    

    public AmazonProperties.Aws getAws() {
    	return aws;
    }
    public AmazonProperties.S3 getS3() {
    	return s3;
    }
    
    /**
     * Save a file using authenticated session credentials
     *
     * @param key  is the name of the file to save in the bucket
     * @param file is the file that will be saved
     * @return an instance of {@link PutObjectResult} containing the result of the save operation
     */
    public PutObjectResult save(String key, File file) {
        return getAmazonS3Client().putObject(new PutObjectRequest(s3.getDefaultBucket(), key, file));
    }

    /**
     * Get a file using the authenticated session credentials
     *
     * @param key is the key of the file in the bucket that should be retrieved
     * @return an instance of {@link S3Object} containing the file from S3
     */
    public S3Object get(String key) {
        return getAmazonS3Client().getObject(s3.getDefaultBucket(), key);
    }

    /**
     * Gets an Amazon S3 client from basic session credentials
     *
     * @return an authenticated Amazon S3 client
     */
    public AmazonS3 getAmazonS3Client() {
    	BasicSessionCredentials basicSessionCredentials = getBasicSessionCredentials();

        // Create a new S3 client using the basic session credentials of the service instance
        return new AmazonS3Client(basicSessionCredentials);
       }

    /**
     * Get the basic session credentials for the template's configured IAM authentication keys
     *
     * @return a {@link BasicSessionCredentials} instance with a valid authenticated session token
     */
    private BasicSessionCredentials getBasicSessionCredentials() {

        // Create a new session token if the session is expired or not initialized
        if (sessionCredentials == null || sessionCredentials.getExpiration().before(new Date()))
            sessionCredentials = getSessionCredentials();

        // Create basic session credentials using the generated session token
        return new BasicSessionCredentials(sessionCredentials.getAccessKeyId(),
                sessionCredentials.getSecretAccessKey(),
                sessionCredentials.getSessionToken());
    }
    
    

    /**
     * Creates a new session credential that is valid for 12 hours
     *
     * @return an authenticated {@link Credentials} for the new session token
     */
    private Credentials getSessionCredentials() {
        // Create a new session with the user credentials for the service instance
    	
//    	AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClient.builder().withCredentials(
//    			new AWSStaticCredentialsProvider(new BasicAWSCredentials(aws.getAccessKeyId(), aws.getAccessKeySecret()))).
//    			withRegion(aws.getRegion()).build(); 
    	
        AWSSecurityTokenServiceClient stsClient =
                new AWSSecurityTokenServiceClient(new BasicAWSCredentials(aws.getAccessKeyId(), aws.getAccessKeySecret()));

        // Start a new session for managing a service instance's bucket
        GetSessionTokenRequest getSessionTokenRequest =
                new GetSessionTokenRequest().withDurationSeconds(aws.getTimeout());

        // Get the session token for the service instance's bucket
        sessionCredentials = stsClient.getSessionToken(getSessionTokenRequest).getCredentials();

        return sessionCredentials;
    }
    
    public String getDefaultBucket() {
    	return s3.getDefaultBucket();
    }
    


}
