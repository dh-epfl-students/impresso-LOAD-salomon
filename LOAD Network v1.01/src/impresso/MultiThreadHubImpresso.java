package impresso;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.json.JSONObject;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.cache.Cache;

import settings.LOADmodelSettings;

import static settings.SystemSettings.DEBUG_PROMPT;
import static settings.SystemSettings.VERBOSE;

/**
 * Coordinates the work of LOAD graph construction workers on a by-document basis
 * Synchronizes graph node labeling across worker threads.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class MultiThreadHubImpresso {
    private int[][] pageIDs;
    private int currentYearID;
    private int currentPageID;
    private ArrayList<TObjectIntHashMap<String>> valueToIdMaps;
    private int[] currentIDs;
    private BufferedWriter edgeWriter;
    public CountDownLatch latch;
	
    //Properties
    private Properties prop;
    
    private AmazonS3 S3Client;
    
    private Cache<String, JSONObject> newspaperCache;
    private Cache<String, JSONObject> entityCache;
    
    // notifier variables
    private int nextpromille;
    private double promillecount;
    DecimalFormat dform = new DecimalFormat("##.#");
    
    //Map that keeps track of string pageids and int page ids
    private HashMap<Integer, String> contentIdtoInt;
    
    // statistics variables
    private int validAnnotations;
    private long unaggregatedEdges;
    private int failedSentences;
    private int annotationsWithInvalidType;
    private HashSet<String> invalidTypes;
    private int[] count_ValidAnnotationsByType;
    private int negativeOffsetCount;
    
    public MultiThreadHubImpresso(int[][] pageIDs, ArrayList<TObjectIntHashMap<String>> valueToIdMaps, int[] currentIDs,
                                  AmazonS3 S3Client, Cache<String, JSONObject> newspaperCache, Cache<String,
                                  JSONObject> entityCache, Properties prop, HashMap<Integer, String> contentIdtoPageId,
                                  BufferedWriter ew, int nThreads) {
        this.pageIDs = pageIDs;
        this.valueToIdMaps = valueToIdMaps;
        this.currentIDs = currentIDs;
        this.edgeWriter = ew;
        this.prop = prop;
        //Initializing hashmap
        this.contentIdtoInt = contentIdtoPageId;
        
        //S3Client
        this.S3Client = S3Client;
        
        //This Cache
        this.newspaperCache = newspaperCache;
        this.entityCache = entityCache;
        
        // notifier variables
        nextpromille = pageIDs.length / 1000;
        promillecount = 0;
        
        // statistics variables
        validAnnotations = 0;
        unaggregatedEdges = 0;
        failedSentences = 0;
        annotationsWithInvalidType = 0;
        invalidTypes = new HashSet<String>();
        count_ValidAnnotationsByType = new int [LOADmodelSettings.nANNOTATIONS];
        negativeOffsetCount = 0;
        
        // count down latch
        latch = new CountDownLatch(nThreads);

        addYearCache(pageIDs[0][0]);

    }
    
    private void addYearCache(Integer pageID) {
    	String contentId = contentIdtoInt.get(pageID);
    	String newspaperId = contentId.split("-")[0];
    	String year = contentId.split("-")[1];
    	try {
			S3Reader reader = new S3Reader(newspaperId, year, prop, S3Client, newspaperCache, entityCache);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }

    public synchronized Integer getContentItemID(){

        if(currentPageID >= pageIDs[currentYearID].length && currentYearID + 1 >= pageIDs.length){
            if(VERBOSE)
                System.out.println("All page ids have been read.");
            return null;
        } else if(currentPageID >= pageIDs[currentYearID].length){
            if(DEBUG_PROMPT)
                System.out.println(String.format("All pages for year id %d have been read. Starting new year with id %d",  currentYearID, currentYearID + 1));
            currentPageID = 0;
            currentYearID ++;

            addYearCache(pageIDs[currentYearID][currentPageID]);
            return pageIDs[currentYearID][currentPageID];
        } else {
            currentPageID++;
            return pageIDs[currentYearID][currentPageID - 1];
        }
    }

    // returns ID of next page or null if all pages are done
  /*  public synchronized Integer getPageID_old() {
        Integer selectedID;
        System.out.println(pageIDs.length);

        if(nextYearID < pageIDs.length) {
            System.out.println("A1");
        	nextPageID = 0;
        	nextYearID ++;
        	selectedID = pageIDs[nextYearID][nextPageID];
        	//Function for reading in cache for next year
        	addYearCache(selectedID);
        }else if (nextPageID < pageIDs[nextYearID].length) {
            System.out.println("A2");
            selectedID= pageIDs[nextYearID][nextPageID];
            nextPageID++;
        }else {
            System.out.println("A3");
            selectedID = null;
        }
        return selectedID;
    }
    */

    // get a node id from a node-type value to id map or create one of it does not exist
    public int getAnnotationID(char type, String value) {
        int annotationID;
        TObjectIntHashMap<String> map = valueToIdMaps.get(type);

        synchronized (map) {
            if (map.containsKey(value)) {
                annotationID = map.get(value);
            } else {
                annotationID = currentIDs[type]++;
                map.put(value, annotationID);
            }
        }
        
        return annotationID;
    }
    
    // write resulting edges to file
    public synchronized void writeEdges(ArrayList<String> edgeList) throws Exception {
        for (String s : edgeList) {
            edgeWriter.append(s);
        }
    }
    
    // update the individual thread statistics
    public synchronized void updateStatistics(int validAnnotations, long unaggregatedEdges, int failedSentences,
                                              int annotationsInvalidType, HashSet<String> invalidTypes, int[] validAnnotationsByType,
                                              int negativeOffsetCount) {
        
        this.validAnnotations += validAnnotations;
        this.unaggregatedEdges += unaggregatedEdges;
        this.failedSentences += failedSentences;
        this.annotationsWithInvalidType += annotationsInvalidType;
        this.invalidTypes.addAll(invalidTypes);
        for (int i=0; i<validAnnotationsByType.length; i++) {
            this.count_ValidAnnotationsByType[i] += validAnnotationsByType[i];
        }
        this.negativeOffsetCount += negativeOffsetCount;
    }
    
    public synchronized int getValidAnnotations() { return validAnnotations; }
    
    public synchronized long getUnaggregatedEdges() { return unaggregatedEdges; }
    
    public synchronized int getFailedSentences() { return failedSentences; }
    
    public synchronized int getAnnotationsWithInvalidType() { return annotationsWithInvalidType; }
    
    public synchronized HashSet<String> getInvalidTypes() { return invalidTypes; }

    public synchronized int[] getValidAnnotationsByType() { return count_ValidAnnotationsByType; }
    
    public synchronized int getNegativeOffsetCount() { return negativeOffsetCount; }
}
