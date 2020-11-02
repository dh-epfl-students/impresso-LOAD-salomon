package impresso;

import java.util.List;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.s3.model.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import static settings.SystemSettings.TRANSFER_AVAILABLE;

public class S3Reader {
	private static Properties prop;
	private String year = null;
	private static String bucketName;
	private static boolean mentions = true;

	public S3Reader(String newspaperID, String year, Properties prop, AmazonS3 S3Client, Cache<String, JSONObject> newspaperCache, Cache<String, JSONObject> entityCache) throws IOException {
		bucketName = prop.getProperty("s3BucketName"); //Name of the bucket
        String prefix = prop.getProperty("s3Prefix"); //Name of prefix for S3
        String keySuffix = prop.getProperty("s3KeySuffix"); //Suffix of each BZIP2 
        
        try{
        	if(year != null) {	
    	        String newspaperKey = prefix + newspaperID +"-" + year + keySuffix;
    	        String entityKey ="mysql-mention-dumps/NZZ/" + newspaperID +"-"+ year +"-mentions.jsonl.bz2";
                populateCache(newspaperKey, entityKey, S3Client, newspaperCache, entityCache);
        	} else {
        		String curPrefix = prefix+newspaperID; //Creates the prefix to search for
        		ObjectListing listing = S3Client.listObjects(bucketName, curPrefix);
        		List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        		for(S3ObjectSummary summary:summaries) {
        			String key = summary.getKey();
        			System.out.println(key);
                    populateCache(key, null, S3Client, newspaperCache, entityCache);
        		}
        	}
        }catch (AmazonServiceException ase)
        {
          System.out.println("Caught an AmazonServiceException, which means your request made it to S3, but was rejected with an error response for some reason.");
          System.out.println("Error Message:   " + ase.getMessage());
          System.out.println("HTTP Status Code:" + ase.getStatusCode());
          System.out.println("AWS Error Code:" + ase.getErrorCode());
          System.out.println("Error Type:" + ase.getErrorType());
          System.out.println("Request ID:" + ase.getRequestId());
        }
        catch (AmazonClientException ace)
        {
          System.out.println("Caught an AmazonClientException, which means the client encountered"
          +"a serious internal problem while trying to communicate with S3 such as not being able to access the network.");
          System.out.println("Error Message:" + ace.getMessage());
        }

	}
/*
	public ImpressoContentItem injectLingusticAnnotations(ImpressoContentItem contentItem, Cache<String, JSONObject> newspaperCache, Cache<String, JSONObject> entityCache) {
		String tempId = contentItem.getId();

		JSONObject jsonObj = newspaperCache.getIfPresent(tempId);
    	JSONArray sents = jsonObj.getJSONArray("sents");
    	int length = sents.length();
    	int totalOffset = 0; //Keeps track of the total offset
    	for(int j=0; j<length; j++) {
    	    JSONObject sentence = sents.getJSONObject(j);
    	    //This is where the injectTokens of a ImpressoContentItem
    	    totalOffset += contentItem.injectTokens(sentence.getJSONArray("tok"), sentence.getString("lg"), true, totalOffset);
    	}
    	
    	jsonObj = entityCache.getIfPresent(tempId);
    	JSONArray mentions = jsonObj.getJSONArray("mentions");
    	//This is where the injectAnnotations of a ImpressoContentItem
    	contentItem.injectTokens(mentions, null, false, 0);
    	
		return contentItem;
	}
*/
	
	private static void populateCache(String newspaperKey, String entityKey, AmazonS3 S3Client, Cache<String, JSONObject> newspaperCache, Cache<String, JSONObject> entityCache) throws IOException {

		GetObjectRequest object_request = new GetObjectRequest(bucketName, newspaperKey);
	    S3Object fullObject = S3Client.getObject(object_request);
		
		try (Scanner fileIn = new Scanner(new BZip2CompressorInputStream(fullObject.getObjectContent()))) {
    	  //First download the key
		  // Read the text input stream one line at a as a json object and parse this object into contentitems	      
		  if (null != fileIn) {
			  while (fileIn.hasNext()) {
				  String line = fileIn.nextLine();
				  try {
					  JSONObject jsonObj = new JSONObject(line);
					  newspaperCache.put(jsonObj.getString("id"), jsonObj);
				  } catch (Exception e){
				  	System.out.println(line);
				  }
			    }
			}
		}
        finally {
            // To ensure that the network connection doesn't remain open, close any open input streams.
            if (fullObject != null) {
                fullObject.close();
            }
        }
        
		/*
		 * WHILE THE ENTITIES ARE BEING DUMPED TO THE S3 BUCKET
		 * SHOULD NOT EXIST IN THE FINAL IMPLEMENTATION
		 */
		if(entityKey != null) {
			mentions = false;
			FileInputStream fin =  null;
			S3ObjectInputStream S3fin = null;
	  	    if(TRANSFER_AVAILABLE){
				object_request = new GetObjectRequest("TRANSFER", entityKey);
			    fullObject = S3Client.getObject(object_request);
			    S3fin = fullObject.getObjectContent();
	  	    } else {
	  	    	fin = new FileInputStream("GDL-mentions/GDL-1890-mentions.jsonl.bz2");
			}
	  	    try (Scanner fileIn = new Scanner(new  BZip2CompressorInputStream(TRANSFER_AVAILABLE ? S3fin : fin))) {
	  	    	//First download the key
				// Read the text input stream one line at a as a json object and parse this object into contentitems
				if (null != fileIn) {
					while (fileIn.hasNext()) {
						JSONObject jsonObj = new JSONObject(fileIn.nextLine());
						entityCache.put(jsonObj.getString("id"), jsonObj);
					}
				}
	  	    }
		        /*finally {
		            // To ensure that the network connection doesn't remain open, close any open input streams.
		            if (fullObject != null) {
		                fullObject.close();
		            }
		        }*/



		}
		
    }
	
}
