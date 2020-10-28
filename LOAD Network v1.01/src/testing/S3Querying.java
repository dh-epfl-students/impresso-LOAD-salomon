package testing;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.util.StringUtils;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompressionType;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InputSerialization;
import com.amazonaws.services.s3.model.JSONInput;
import com.amazonaws.services.s3.model.JSONOutput;
import com.amazonaws.services.s3.model.ExpressionType;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.OutputSerialization;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SelectObjectContentEvent;
import com.amazonaws.services.s3.model.SelectObjectContentEventStream;
import com.amazonaws.services.s3.model.SelectObjectContentEventVisitor;
import com.amazonaws.services.s3.model.SelectObjectContentRequest;
import com.amazonaws.services.s3.model.SelectObjectContentResult;
import com.amazonaws.services.s3.model.SelectRecordsInputStream;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;

public class S3Querying {

	public static void main(String[] args) throws IOException {
		Properties prop=new Properties();
		String propFilePath ="LOAD Network v1.01/resources/config.properties";
		
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(propFilePath);
			prop.load(inputStream);
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String accessKey = System.getenv("S3_ACCESS_KEY");
		String secretKey = System.getenv("S3_SECRET_KEY");

		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		
		// Set S3 Client Endpoint

        AwsClientBuilder.EndpointConfiguration switchEndpoint = new AwsClientBuilder.EndpointConfiguration(
                prop.getProperty("s3BaseName"),"");
        
    	// Set signer type and http scheme
        ClientConfiguration conf = new ClientConfiguration();
        	    conf.setSignerOverride("S3SignerType");
		        conf.setProtocol(Protocol.HTTPS);
                
        AmazonS3 S3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(switchEndpoint)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(conf)
                .withPathStyleAccessEnabled(true)
                .build();
		
        System.out.println("===========================================");
        System.out.println(" Connection to the S3" );
        System.out.println("===========================================\n");
        
        
        S3Object fullObject = null;
        try { 
            /*
            */
	          String bucketName ="processed-canonical-data"; //Name of the bucket
	          String prefix ="linguistic-processing/2020-03-11/";
	          
	          ListObjectsV2Request req = new
	          ListObjectsV2Request().withBucketName(bucketName).withPrefix(prefix).withDelimiter("/").withMaxKeys(10);
	          ListObjectsV2Result result = S3Client.listObjectsV2(req);

	          List<S3ObjectSummary> res = result.getObjectSummaries();
	          String key = res.get(0).getKey();

	          //Testing selecting the object's content of a particular bucket
	          SelectObjectContentRequest request = new SelectObjectContentRequest();
	          request.setBucketName(bucketName);
	          request.setKey(key);
	          request.setExpression("SELECT TOP 1 FROM S3Object s");
	          request.setExpressionType(ExpressionType.SQL);

	          InputSerialization inputSerialization = new InputSerialization();
	          inputSerialization.setJson(new JSONInput());
	          inputSerialization.setCompressionType(CompressionType.BZIP2);
	          request.setInputSerialization(inputSerialization);
	          
	          OutputSerialization outputSerialization = new OutputSerialization();
	          outputSerialization.setJson(new JSONOutput());
	          request.setOutputSerialization(outputSerialization);
	          
	          final AtomicBoolean isResultComplete = new AtomicBoolean(false);

	          //SelectObjectContentResult results = S3Client.selectObjectContent(request);
	          
	          GetObjectRequest object_request = new GetObjectRequest(bucketName, key);
	          fullObject = S3Client.getObject(object_request);
	          
              System.out.println("Content-Type:"  + fullObject.getObjectMetadata().getContentType());
              System.out.println("Content:" );
              displayTextInputStream(fullObject.getObjectContent());

              /*
	          SelectRecordsInputStream resultInputStream = results.getPayload().getRecordsInputStream(new SelectObjectContentEventVisitor() {
		                  @Override
		                  public void visit(SelectObjectContentEvent.StatsEvent event)
		                  {
		                      System.out.println(
		                             " Received Stats, Bytes Scanned:"  + event.getDetails().getBytesScanned()
		                                      + "  Bytes Processed:"  + event.getDetails().getBytesProcessed());
		                  }
		

		                  @Override
		                  public void visit(SelectObjectContentEvent.EndEvent event)
		                  {
		                      isResultComplete.set(true);
		                      System.out.println("Received End Event. Result is complete.");
		                  }
		              }
		      );*/
	          
	          //Select Records from the Input Stream
	          
	          
              /*for (S3ObjectSummary objectSummary : result.getObjectSummaries()) 
              {
            	if(objectSummary.getKey() ==" linguistic-processing/2020-03-11/JDG-1880.ling.annotation.jsonl.bz2") {
            		
            	}
            	  
            	  
                System.out.println(" ---"  + objectSummary.getKey() +""
                +" (size ="  + objectSummary.getSize() +" )" +""
                +" (eTag ="  + objectSummary.getETag() +" )");
                System.out.println();
              }*/
          }
          catch (AmazonServiceException ase)
          {
            System.out.println("Caught an AmazonServiceException, which means your request made it to S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:   "  + ase.getMessage());
            System.out.println("HTTP Status Code:"  + ase.getStatusCode());
            System.out.println("AWS Error Code:"  + ase.getErrorCode());
            System.out.println("Error Type:"  + ase.getErrorType());
            System.out.println("Request ID:"  + ase.getRequestId());
          }
          catch (AmazonClientException ace)
          {
            System.out.println("Caught an AmazonClientException, which means the client encountered"
            +" a serious internal problem while trying to communicate with S3 such as not being able to access the network.");
            System.out.println("Error Message:"  + ace.getMessage());
          } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        finally {
              // To ensure that the network connection doesn't remain open, close any open input streams.
              if (fullObject != null) {
                  fullObject.close();
              }
          }

	}
	private static void displayTextInputStream(InputStream input) throws IOException {
        // Read the text input stream one line at a time and display each line.
		Scanner fileIn = new Scanner(new  BZip2CompressorInputStream(input));
	    if (null != fileIn) {
	        while (fileIn.hasNext()) {
	            System.out.println("Line:"  + fileIn.nextLine());
	        }
	    }
    }
}
