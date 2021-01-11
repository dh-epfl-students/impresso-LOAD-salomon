package impresso;

import java.io.*;
import java.util.*;

import com.amazonaws.services.s3.model.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.cache.Cache;

import static settings.SystemSettings.*;

public class S3Reader {
	private static Properties prop;
	private String year = null;
	private static String bucketName;
	private static boolean mentions = true;

	public S3Reader(String newspaperID, String year, Properties prop, AmazonS3 S3Client, Cache<String, JSONObject> newspaperCache, Cache<String, JSONArray> entityCache, Collection<String> ids) throws IOException {
		bucketName = prop.getProperty("s3BucketName"); //Name of the bucket
        String prefix = prop.getProperty("s3Prefix"); //Name of prefix for S3
        String keySuffix = prop.getProperty("s3KeySuffix"); //Suffix of each BZIP2 
        
        try{
        	File year_file = new File(ID_FOLDER + newspaperID + "-ids/" + year + ".txt");
        	if(year != null && year_file.exists()) {
    	        String newspaperKey = prefix + newspaperID +"-" + year + keySuffix;
    	        String entityKey ="mysql-mention-dumps/" + newspaperID + "/" + newspaperID + "-"+ year +"-mentions.jsonl.bz2";
    	        System.gc();
                populateCache(newspaperKey, entityKey, S3Client, newspaperCache, entityCache, ids);
        	} else {
        		String curPrefix = prefix+newspaperID; //Creates the prefix to search for
        		ObjectListing listing = S3Client.listObjects(bucketName, curPrefix);
        		List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        		for(S3ObjectSummary summary:summaries) {
        			String key = summary.getKey();
        			System.out.println(key);
					try {
						FileWriter myWriter = new FileWriter("last_key.txt");
						myWriter.write(key + "\n");
						myWriter.close();
					} catch (IOException e) {
						System.out.println("An error occurred.");
						e.printStackTrace();
					}
                    populateCache(key, null, S3Client, newspaperCache, entityCache, ids);
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
	
	private static void populateCache(String newspaperKey, String entityKey, AmazonS3 S3Client, Cache<String, JSONObject> newspaperCache, Cache<String, JSONArray> entityCache, Collection<String> ids) throws IOException {
		GetObjectRequest object_request = new GetObjectRequest(bucketName, newspaperKey);
		GetObjectMetadataRequest meta = new GetObjectMetadataRequest(bucketName, newspaperKey);
		ObjectMetadata metad = S3Client.getObjectMetadata(meta);

		long total_time = System.currentTimeMillis();
		S3Object fullObject = S3Client.getObject(object_request);
		if(RUNTIME_PROMPT) {
			System.out.println("GET OBJECT : " + (System.nanoTime() - total_time) + " ns");
		}
		S3ObjectInputStream stream = fullObject.getObjectContent();

		int cnt = 0;
		try (Scanner fileIn = new Scanner(new BZip2CompressorInputStream(stream))) {
			//First download the key
			// Read the text input stream one line at a as a json object and parse this object into contentitems
			if(VERBOSE)
				System.out.println("Get all the annotated words for " + newspaperKey);
			if (null != fileIn) {
				while (fileIn.hasNext()) {
					if(RUNTIME_PROMPT)
						System.out.println("NEW SENTENCE");

					System.gc();
					String line = fileIn.nextLine();
					long start_time = System.nanoTime();
					try {
						cnt += 1;
						JSONObject jsonObj = new JSONObject(line);
						if(ids.contains(jsonObj.getString("id")))
							newspaperCache.put(jsonObj.getString("id"), jsonObj);
						if(RUNTIME_PROMPT){
							System.out.println("STORE LINE : " + (System.nanoTime() - start_time) + " ns");
							JSONArray sentences = jsonObj.getJSONArray("sents");
							System.out.println("NUM SENTENCES : " + sentences.length());
							int words = 0;
							for (Object sentence : sentences)
								words += ((JSONObject) sentence).getJSONArray("tok").length();
							System.out.println("NUM WORDS : " + words);
						}
					} catch (Exception e) {
						System.out.println(line);
					}

				}
				if(RUNTIME_PROMPT) {
					System.out.println("NUMBER OF READ LINES : " + cnt);
					System.out.println("SIZE OF FILE (BYTES) : " + metad.getContentLength());
				}
			}
			if(VERBOSE)
				System.out.println("Annotated words queried, read and stored in " + (System.currentTimeMillis() - total_time) + " ms");
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
		if(TRANSFER_DUMP && entityKey != null) {
			if(VERBOSE)
				System.out.println("Get all the mentioned entities for " + newspaperKey);
			mentions = false;
			FileInputStream fin =  null;
			S3ObjectInputStream S3fin = null;
			object_request = new GetObjectRequest("TRANSFER", entityKey);
			fullObject = S3Client.getObject(object_request);
			S3fin = fullObject.getObjectContent();

	  	    try (Scanner fileIn = new Scanner(new  BZip2CompressorInputStream(TRANSFER_DUMP ? S3fin : fin))) {
	  	    	long start_time = System.currentTimeMillis();
	  	    	//First download the key
				// Read the text input stream one line at a as a json object and parse this object into contentitems
				if (null != fileIn) {
					while (fileIn.hasNext()) {
						System.gc();
						String line = fileIn.nextLine();
						try {
							JSONObject jsonObj = new JSONObject(line);
							if (ids.contains(jsonObj.getString("id")))
								entityCache.put(jsonObj.getString("id"), ((JSONArray) jsonObj.get("mentions")));
						} catch (Exception e){
							System.out.println(line);
						}
					}
				}
				if(VERBOSE)
					System.out.println("Entities queried, read and stored in " + (System.currentTimeMillis() - start_time) + " ms");
	  	    }
	  	    finally {
		            // To ensure that the network connection doesn't remain open, close any open input streams.
		            if (fullObject != null) {
		                fullObject.close();
		            }

	  	    }
		}
		
    }
	
}
