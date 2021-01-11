package construction;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

import settings.LOADmodelSettings;

/**
 * Coordinates the work of LOAD graph construction workers on a by-document basis
 * Synchronizes graph node labeling across worker threads.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class MultiThreadHub {
    private int[] pageIDs;
    private int nextPageID;
    private ArrayList<TObjectIntHashMap<String>> valueToIdMaps;
    private int[] currentIDs;
    private BufferedWriter edgeWriter;
    public CountDownLatch latch;
    
    // notifier variables
    private int nextpromille;
    private double promillecount;
    DecimalFormat dform = new DecimalFormat("##.#");
    
    // statistics variables
    private int validAnnotations;
    private long unaggregatedEdges;
    private int failedSentences;
    private int annotationsWithInvalidType;
    private HashSet<String> invalidTypes;
    private int[] count_ValidAnnotationsByType;
    private int negativeOffsetCount;
    
    public MultiThreadHub(int[] pageIDs, ArrayList<TObjectIntHashMap<String>> valueToIdMaps, int[] currentIDs, BufferedWriter ew, int nThreads) {
        this.pageIDs = pageIDs;
        nextPageID = 0;
        this.valueToIdMaps = valueToIdMaps;
        this.currentIDs = currentIDs;
        this.edgeWriter = ew;
        
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
    }
    
    // returns ID of next page or null if all pages are done
    public synchronized Integer getPageID() {
        Integer retval;
        if (nextPageID < pageIDs.length) {
            retval = pageIDs[nextPageID];
            nextPageID++;
            
            if (nextPageID == nextpromille) {
                nextpromille += pageIDs.length / 1000;
                promillecount += 0.1;
                System.out.print("\rRead"  + dform.format(promillecount) +" % of pages.   " );
            }
            
        } else {
            retval = null;
        }
        return retval;
    }
    
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
