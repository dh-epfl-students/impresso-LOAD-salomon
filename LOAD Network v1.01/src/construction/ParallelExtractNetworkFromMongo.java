package construction;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.*;
import externalsort.ParallelDiskMergeSort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

// mongoDB imports
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import java.util.logging.Level;
import java.util.logging.Logger;

// trove library imports
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Creation of a LOAD network from a document collection with named entity annotations stored in a Solr DB 
 * 
 * Run main method to build a LOAD graph from a document collection. Change settings in package settings.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class ParallelExtractNetworkFromMongo {
    
    // hash maps for assigning IDs to nodes (consecutive for each type of entity)
    private ArrayList<TObjectIntHashMap<String>> valueToIdMaps;
    private int[] currentIDs;
    
    // Define output variables and  counters
    private int count_Articles;
    private int count_Sentences;
    private int count_Annotations;
    private int count_ValidAnnotations;
    private int[] count_ValidAnnotationsByType = new int[nANNOTATIONS];
    private long count_unaggregatedEdges = 0;
    private static int[] setSizes = new int[nANNOTATIONS];
    private static long[] aggregatedEdgeCounts = new long[nANNOTATIONS];
    
    /* Define a comparator for edges in edges lists that are created as intermediary output. Sort by:
     *   1. source type
     *   2. source id
     *   3. target type
     *   4. target id
     * This order is necessary so that all edges from a given nodes (and node set) are
     * processed in the correct order in the aggregation and splitting step (which read
     * edges line by line.
     */
    private static Comparator<String> edgecomparator = new Comparator<String>() {
        @Override
        public int compare(String r1, String r2) {
            String[] s1 = r1.split(sepChar);
            int sourceType1 = s1[0].charAt(0);

            String[] s2 = r2.split(sepChar);
            int sourceType2 = s2[0].charAt(0);

            int rv = sourceType1 - sourceType2;
            if (rv != 0) {
                return rv;
            } else {
                int sourceId1 = Integer.parseInt(s1[2]);
                int sourceId2 = Integer.parseInt(s2[2]);
                rv = sourceId1 - sourceId2;
                if (rv != 0) {
                    return rv;
                } else {
                    int targetType1 = s1[1].charAt(0);
                    int targetType2 = s2[1].charAt(0);
                    rv = targetType1 - targetType2;
                    if (rv != 0) {
                        return rv;
                    } else {
                        int targetId1 = Integer.parseInt(s1[3]);
                        int targetId2 = Integer.parseInt(s2[3]);
                        return targetId1 - targetId2;
                    }
                }
            }
            
        }
    };
    
    // make sure that the output directories exist and are empty
    public static void setUpFolders() {
        
        File theDir = new File(outfolder);
        if (!theDir.exists()) {
            theDir.mkdir();
        } else {
            for(File file: theDir.listFiles()) {
                file.delete();
            }
        }
        
        theDir = new File(tmpfolder);
        if (!theDir.exists()) {
            theDir.mkdir();
        } else {
            for(File file: theDir.listFiles()) {
                file.delete();
            }
        }
    }
    
    // read in and prepare the list of stop words in a hash set for look-up
    public HashSet<String> readStopWords() {
        HashSet<String> stopwords = new HashSet<String>();
        try {
            stopwords = new HashSet<String>();
            BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(stopwordlist),"UTF-8"));
            String line;
            while ((line = bf.readLine()) != null) {
                stopwords.add(line.toLowerCase().trim());
            }            
            bf.close();
        } catch (Exception e) {
            System.out.println("Problem reading stopwords. Using no stopwords.");
            e.printStackTrace();
        }
        return stopwords;
    }
    
    // read the list of all page IDs from an input file that are used to build the network
    public int[] readPageIDs() {
        int[] pageIDs = new int[0];
        try {
            TIntArrayList ids = new TIntArrayList();
            BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(pageIDList),"UTF-8"));
            String line;
            while ((line = bf.readLine()) != null) {
                ids.add(Integer.parseInt(line));
            }            
            bf.close();
            
            // convert to array of ints
            pageIDs = ids.toArray();
            
        } catch (Exception e) {
            System.out.println("Problem reading page IDs from file.");
            e.printStackTrace();
        }
        return pageIDs;
    }
    
    // extract a list of all page IDs from the data set instead, then store it for later use
    public int[] generatePageIDs() {
        int[] pageIDs = new int[0];
        
        try {
            TIntHashSet ids = new TIntHashSet();
            
            Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
            mongoLogger.setLevel(Level.WARNING);
            
            ServerAddress address = new ServerAddress(MongoAdress, MongoPort);
            MongoClient mongoClient;
            if (mongocred != null) {
                mongoClient = new MongoClient(address, Arrays.asList(mongocred));
            } else {
                mongoClient = new MongoClient(address);
            }
            MongoDatabase db = mongoClient.getDatabase(MongoDBname);
            
            // getting IDs from annotations
            MongoCollection<Document> cANN = db.getCollection(MongoCollectionAnnotations);
            long nAnnotations = cANN.count();
            System.out.println("Found"  + nAnnotations +"  annotations. Searching for page IDs.");
            long nextpercent = nAnnotations / 1000;
            long currentAnnotation = 0;
            double percentcount = 0.1;
            DecimalFormat dform = new DecimalFormat("##.#");

            MongoCursor<Document> annotationCursor = cANN.find().noCursorTimeout(true).iterator();
            
            while (annotationCursor.hasNext()) {
                
                if (++currentAnnotation == nextpercent) {
                    nextpercent += nAnnotations / 1000;
                    percentcount += 0.1;
                    System.out.print("\rSearched"  + dform.format(percentcount) +" % of annotations.    " );
                }
                
                Document obj = annotationCursor.next();
                ids.add(obj.getInteger(mongoIdentAnnotation_pageId));
            }
            System.out.println();
            annotationCursor.close();
            
            // convert to list, make sure its (deterministically) shuffled and convert to array
            TIntArrayList idlist = new TIntArrayList(ids);
            
            // ensure deterministic randomization of the page IDs
            idlist.sort();
            idlist.shuffle(new Random(shuffleRandomSeed));
            
            // convert to an array and sort
            pageIDs = ids.toArray();

            // write list to file for later use in re-runs
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pageIDList),"UTF-8"), bufferSize);
            for (int pid : pageIDs) {
                bw.append(pid +" \n");
            }
            bw.close();
            mongoClient.close();
        } catch (Exception e) {
            System.out.println("Problem generating page IDs from database.");
            e.printStackTrace();
        }
        
        return pageIDs;
    }
    
    /* Main Routine
     * Extraction of co-occurrences from the documents and building of nodes and (unaggregated) edges
     */
    public ParallelExtractNetworkFromMongo() {
        
        // get the IDs of all pages of interest
        int[] pageIDs;
        if (readIDsFromFile) {
            System.out.println("Reading page IDs from file.");
            pageIDs = readPageIDs();
        } else {
            System.out.println("Generating page IDs from database.");
            pageIDs = generatePageIDs();
        }
        System.out.println("Number of pages with annotations:"  + pageIDs.length);
        count_Articles = pageIDs.length;
        System.gc();
        
        valueToIdMaps = new ArrayList<TObjectIntHashMap<String>>();
        for (int i=0; i<nANNOTATIONS; i++) {
            valueToIdMaps.add(new TObjectIntHashMap<String>());
        }
        currentIDs = new int[nANNOTATIONS];
        
        try {
            BufferedWriter ew = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfile),"UTF-8"), bufferSize);
            
            Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
            mongoLogger.setLevel(Level.WARNING);
            
            ServerAddress address = new ServerAddress(MongoAdress, MongoPort);
            MongoClient mongoClient;
            if (mongocred != null) {
                mongoClient = new MongoClient(address, Arrays.asList(mongocred));
            } else {
                mongoClient = new MongoClient(address);
            }
            MongoDatabase db = mongoClient.getDatabase(MongoDBname);
            MongoCollection<Document> cANN = db.getCollection(MongoCollectionAnnotations);
            MongoCollection<Document> cSEN = db.getCollection(MongoCollectionSentences);
            
            System.out.println("Number of sentences overall:"  + cSEN.count());
            count_Sentences = (int) cSEN.count();
            System.out.println("Number of annotations overall:"  + cANN.count());
            count_Annotations = (int) cANN.count();
            
            System.out.println("Parsing annotations and extracting network");
            
            HashSet<String> stopwords = readStopWords();
            HashSet<String> invalidTypes = new HashSet<String>();
            
            MultiThreadHub hub = new MultiThreadHub(pageIDs, valueToIdMaps, currentIDs, ew, nThreads);
            
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            for (int i=0; i<nThreads; i++) {
                MultiThreadWorker w = new MultiThreadWorker(hub, cANN, cSEN, stopwords);
                executor.execute(w);
            }
            executor.shutdown();
            
            while (true) {
                try {
                    hub.latch.await();
                    break;
                } catch (InterruptedException e) {
                    System.out.println();
                    System.out.println("Waiting was interrupted (main)");
                }
            }
                        
            ew.close();
            mongoClient.close();
            System.out.println();
            
            count_ValidAnnotations = hub.getValidAnnotations();
            int failedSentences = hub.getFailedSentences();
            int invalidAnnotationCount = hub.getAnnotationsWithInvalidType();
            invalidTypes = hub.getInvalidTypes();
            count_unaggregatedEdges = hub.getUnaggregatedEdges();
            count_ValidAnnotationsByType = hub.getValidAnnotationsByType();
            int negativeOffsetCount = hub.getNegativeOffsetCount();
            
            System.out.println("Number of unaggregated edges:"  + count_unaggregatedEdges);
            System.out.println("Errors occurred for"  + failedSentences +"  sentences.");
            System.out.println("Found"  + count_ValidAnnotations +"  valid annotations.");
            System.out.println("Found"  + invalidAnnotationCount +"  annotations with invalid type.");
            System.out.println("Found"  + negativeOffsetCount +"  annotations with negative offset.");
            System.out.print(" Invalid Types:");
            for (String s : invalidTypes) {
                System.out.print(""  + s);
            }
            System.out.println();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void writeTemporaryNodesToFiles() {
        // write information to files
        System.out.println("\nWriting data to file");
        
        try {
            
            // write metadata
            System.out.println("Writing metadata");
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfolder + metaFileName),"UTF-8"), bufferSize);
            w.append(metaHeaderVersion);
            
            w.append("# OPTION - maximum distance in sentences for edge creation:" + maxDistanceInSentences +"\n");
            w.append("# Number of pages with annotations:" + count_Articles +"\n");
            w.append("# Number of sentences:" + count_Sentences +"\n");
            w.append("# Number of sentences with annotations:" + count_ValidAnnotationsByType[SEN] +"\n");
            w.append("# Number of annotations:" + count_Annotations +"\n");
            w.append("# Number of valid annotations:" + count_ValidAnnotations +"\n");
            w.append("# Number of unaggregated edges:"  + count_unaggregatedEdges +"\n");
            for (int i=0; i<4; i++) {
                w.append("# Valid annotations of type"  + setNames[i] +" :" + count_ValidAnnotationsByType[i]+"\n");
            }

            w.append(metaHeader);
            for (int i=0; i<nANNOTATIONS; i++) {
                w.append(setNames[i] + sepChar + valueToIdMaps.get(i).size() +" \n");
            }
            w.close();
            
            // write temporary node data to get rid of the valueToIdMaps
            System.out.println("Writing temporary node data");
            for (int i=0; i<nANNOTATIONS; i++) {
                w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfolder +" tmp_" + vertexFileNames[i]),"UTF-8"), bufferSize);
                for (TObjectIntIterator<String> it = valueToIdMaps.get(i).iterator(); it.hasNext(); ) {
                    it.advance();
                    w.append(it.value() + sepChar + it.key() +" \n");
                }
                w.close();
            }
            
            // store the number of nodes in each set then clear the map of nodes to free memory
            // this is required for building the degree sequences later on after edges aggregation
            for (int i=0; i<nANNOTATIONS; i++) {
                setSizes[i] = valueToIdMaps.get(i).size();
            }            
            valueToIdMaps.clear();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void sortUnaggregatedEdgelistExternally() {
        System.out.println("Sorting unaggregated edges.");
        
        // compute number of lines per file
        int nLinesPerFile = (int) Math.ceil((double) count_unaggregatedEdges / (double) maxTempFiles);
        
        // initialize sorter
        ParallelDiskMergeSort dms = new ParallelDiskMergeSort(count_unaggregatedEdges, nLinesPerFile, bufferSize, edgecomparator);
        
        // make sure that the temporary folder exists
        File tempFileStore = new File(tmpSortingDirectory);
        if (!tempFileStore.exists()) {
            tempFileStore.mkdir();
        } else {
            for(File file: tempFileStore.listFiles()) {
                file.delete();
            }
        }
        
        dms.sortFile(new File(tmpfile), new File(sortedtmpfile), tempFileStore);
        
        // remove the temporary folder
        tempFileStore.delete();
    }
    
    public void aggregateEdgesAndSplitIntoIndividualFiles() {
        System.out.println("Aggregating edgelist file and splitting into individual files");
        
        try {
            BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(sortedtmpfile),"UTF-8"));
            
            ArrayList<BufferedWriter> out = new ArrayList<BufferedWriter>();
            for (int i=0; i<nANNOTATIONS; i++) {
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfolder +" tmp_" + edgeFileNames[i]),"UTF-8"), bufferSize);
                out.add(w);
            }
            
            long linecount = 0;
            long nextpromille = count_unaggregatedEdges / 1000;
            double promillecount = 0.1;
            DecimalFormat dform = new DecimalFormat("##.#");
            String line;
            String[] splitline;
            
            // read the first line
            line = bf.readLine();
            linecount++;
            splitline = line.split(sepChar);
            char sourceType = splitline[0].charAt(0);
            char targetType = splitline[1].charAt(0);
            int sourceId = Integer.parseInt(splitline[2]);
            int targetId = Integer.parseInt(splitline[3]);
            int weight_int = Integer.parseInt(splitline[4]);
            
            float weight;
            if (targetType >= TER) {
                weight = default_weight;
            } else {
                weight = weightFunctionExponential(weight_int);
            }
            
            /* Read all following edges (which are sorted) and decide:
             * a) if it is the same edge: aggregate with active edge
             * b) if it is a different edge, write to file and set new edge as active
             */
            while ((line = bf.readLine()) != null) {
                if (++linecount == nextpromille) {
                    nextpromille += count_unaggregatedEdges / 1000;
                    promillecount += 0.1;
                    System.out.print("\rRead"  + dform.format(promillecount) +" % of unaggregated edges.  " );
                }
                
                splitline = line.split(sepChar);
                char n1 = splitline[0].charAt(0);
                char n2 = splitline[1].charAt(0);
                int n3 = Integer.parseInt(splitline[2]);
                int n4 = Integer.parseInt(splitline[3]);
                weight_int = Integer.parseInt(splitline[4]);
                
                float weight2;
                if (n2 >= TER) {
                    weight2 = default_weight;
                } else {
                    weight2 = weightFunctionExponential(weight_int);
                }
                
                if (sourceType==n1 && targetType==n2 && sourceId==n3 && targetId==n4) {
                    weight += weight2;
                } else {
                    if (targetType >= TER) {
                        out.get(sourceType).append(sourceType + sepChar + targetType + sepChar + sourceId + sepChar + targetId + sepChar + (int)weight +" \n");
                        out.get(targetType).append(targetType + sepChar + sourceType + sepChar + targetId + sepChar + sourceId + sepChar + (int)weight +" \n");
                    } else {
                        out.get(sourceType).append(sourceType + sepChar + targetType + sepChar + sourceId + sepChar + targetId + sepChar + df.format(weight) +" \n");
                        out.get(targetType).append(targetType + sepChar + sourceType + sepChar + targetId + sepChar + sourceId + sepChar + df.format(weight) +" \n");
                    }
                    
                    // increase edge counts
                    aggregatedEdgeCounts[sourceType]++;
                    aggregatedEdgeCounts[targetType]++;
                    
                    sourceType = n1;
                    targetType = n2;
                    sourceId = n3;
                    targetId = n4;
                    weight = weight2;
                }
            }
            System.out.println();
            
            // write last active edge
            if (targetType >= TER) {
                out.get(sourceType).append(sourceType + sepChar + targetType + sepChar + sourceId + sepChar + targetId + sepChar + (int)weight +" \n");
                out.get(targetType).append(targetType + sepChar + sourceType + sepChar + targetId + sepChar + sourceId + sepChar + (int)weight +" \n");
            } else {
                out.get(sourceType).append(sourceType + sepChar + targetType + sepChar + sourceId + sepChar + targetId + sepChar + df.format(weight) +" \n");
                out.get(targetType).append(targetType + sepChar + sourceType + sepChar + targetId + sepChar + sourceId + sepChar + df.format(weight) +" \n");
            }
            
            // increase edge counts
            aggregatedEdgeCounts[sourceType]++;
            aggregatedEdgeCounts[targetType]++;
                            
            bf.close();
            for (int i=0; i<nANNOTATIONS; i++) {
                out.get(i).close();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void sortIndividualEdgeFiles() {
        System.out.println("Sorting individual edgelist files");
        
        // make sure that the temporary folder exists
        File tempFileStore = new File(tmpSortingDirectory);
        if (!tempFileStore.exists()) {
            tempFileStore.mkdir();
        } else {
            for(File file: tempFileStore.listFiles()) {
                file.delete();
            }
        }
        
        for (int i=0; i<nANNOTATIONS; i++) {
            
            System.out.println("Sorting edges for"  + setNames[i]);
            
            // compute number of lines per file
            int nLinesPerFile = (int) Math.ceil((double) aggregatedEdgeCounts[i] / (double) maxTempFiles);
            
            // initialize sorter
            ParallelDiskMergeSort dms = new ParallelDiskMergeSort(aggregatedEdgeCounts[i], nLinesPerFile, bufferSize, edgecomparator);
            
            String inputfile = tmpfolder +" tmp_" + edgeFileNames[i];
            String outputfile = tmpfolder +" tmp_sorted_" + edgeFileNames[i];
            dms.sortFile(new File(inputfile), new File(outputfile), tempFileStore);
        }
        
        // remove the temporary folder
        tempFileStore.delete();
    }
    
    public void finalizeNodesAndEdges() {
        
        try {
            // allocate memory for the degree sequences of nodes
            System.out.println("Allocating memory for degrees");
            int[][][] degrees = new int[nANNOTATIONS][][];
            for (int i=0; i<nANNOTATIONS; i++) {
                degrees[i] = new int[setSizes[i]][nANNOTATIONS];
            }
            
            // rewrite edges in adjacency format
            System.out.println("Rewriting Edges and counting degrees");
            for (char type=0; type<nANNOTATIONS; type++) {
                System.out.println("Processing edges for"  + setNames[type]);
                
                String inFile = tmpfolder +" tmp_sorted_" + edgeFileNames[type];
                BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(inFile),"UTF-8"));
                String outFile = outfolder + edgeFileNames[type];
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile),"UTF-8"), bufferSize);
                
                String line = bf.readLine();
                
                int currentSource = 0;
                char targetType = 0;
                w.append(0 +" \n");
                w.append(setNames[0]);

                char edge_sourceType;
                char edge_targetType;
                int edge_sourceId;
                int edge_targetId;
                String edge_weight;
                
                while (line != null) {
                    
                    String[] splitline = line.split(sepChar);
                    edge_sourceType = splitline[0].charAt(0);
                    edge_targetType = splitline[1].charAt(0);
                    edge_sourceId = Integer.parseInt(splitline[2]);
                    edge_targetId = Integer.parseInt(splitline[3]);
                    edge_weight = splitline[4];
                    
                    // if a new source node comes up, this is the end of the current block
                    if (edge_sourceId > currentSource) {
                        // if not all lines for target types have been written yet, write those
                        while (++targetType < nANNOTATIONS) {
                            w.append("\n" + setNames[targetType]);
                        }
                        targetType = 0;
                        // then end the block
                        currentSource = edge_sourceId;
                        w.append("\n\n" + currentSource +" \n");
                        w.append(setNames[0]);
                        
                    // if a new target type comes up, this is the end of a current line
                    // note: iterate over target types one by one, since they might be empty!
                    } else if (edge_targetType > targetType) {
                        targetType++;
                        w.append("\n" + setNames[targetType]);
                        
                    // otherwise write edge target and edge weight and proceed
                    } else {
                        w.append(sepChar + edge_targetId);
                        w.append(sepChar + String.format(edge_weight));
                        
                        // increase respective degree (only in one direction!!!)
                        degrees[edge_sourceType][edge_sourceId][edge_targetType]++;
                        
                        // more to next edge
                        line = bf.readLine();
                    }
                }
                // if not all lines for target types have been written yet for the last node, write those
                while (++targetType < nANNOTATIONS) {
                    w.append("\n" + setNames[targetType]);
                }
                
                bf.close();
                w.close();
            }

            // rewrite nodes and include degrees
            for (int i=0; i<nANNOTATIONS; i++) {
                int emptyNameCount = 0;
                System.out.print("Rewriting nodes for"  + setNames[i] +" .");
                String[] nodeNames = new String[setSizes[i]];
                String inFile = tmpfolder +" tmp_" + vertexFileNames[i];
                BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(inFile),"UTF-8"));
                String line;
                while ((line = bf.readLine()) != null) {
                    String[] splitline = line.split(sepChar);
                    int id = Integer.parseInt(splitline[0]);
                    String name =" " ;
                    if (splitline.length > 1) {
                        name = splitline[1];
                    } else {
                        emptyNameCount++;
                    }
                    nodeNames[id] = name;
                }
                bf.close();
                
                String outFile = outfolder + vertexFileNames[i];
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile),"UTF-8"), bufferSize);
                for (int j=0; j<nodeNames.length; j++) {
                    w.append(nodeNames[j]);
                    for (int k=0; k<nANNOTATIONS; k++) {
                        w.append(sepChar + degrees[i][j][k]);
                    }
                    w.append("\n");
                }
                w.close();
                System.out.println(" (Nodes with no name:"  + emptyNameCount +" )");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
            
    }
    
    
    public static void main(String[] args) {
        try {
            
            // make sure that working folders are up and clean
            setUpFolders();
            
            // read the input data from DB and write temporary edge information of unaggregated edge lists
            ParallelExtractNetworkFromMongo enfm = new ParallelExtractNetworkFromMongo();
            System.gc();
            
            // write temporary node information to clear memory
            enfm.writeTemporaryNodesToFiles();
            System.gc();
            
            // sort the list of unaggregated (duplicate) edge lists with external merge sort
            enfm.sortUnaggregatedEdgelistExternally();
            System.gc();
            
            // aggregate the edge lists to remove parallel edges and split them into individual files
            // NOTE: reciprocal edges are introduced in this step (i.e. edges appear in two files!)
            enfm.aggregateEdgesAndSplitIntoIndividualFiles();
            System.gc();
            
            // sort the aggregated edge lists to prepare for writing them to adjacency lists
            enfm.sortIndividualEdgeFiles();
            System.gc();
            
            // compute degrees, then finalize nodes and edges
            enfm.finalizeNodesAndEdges();
            
            System.out.println("Done.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
