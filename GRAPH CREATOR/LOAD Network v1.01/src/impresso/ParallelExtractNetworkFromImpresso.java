package impresso;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.*;

import com.google.common.collect.Range;
import externalsort.ParallelDiskMergeSort;

import java.awt.font.NumericShaper;
import java.io.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;

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

// trove library imports
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import wikidatademo.dbcopy.MoveCompleteLOADNetworkToMongoDB;

/**
 * Creation of a LOAD network from a document collection with named entity annotations stored in a Solr DB 
 * 
 * Run main method to build a LOAD graph from a document collection. Change settings in package settings.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class ParallelExtractNetworkFromImpresso {
	
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
            try {
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
                            try {
                                int targetId1 = Integer.parseInt(s1[3]);
                                int targetId2 = Integer.parseInt(s2[3]);
                                return targetId1 - targetId2;
                            } catch (NumberFormatException nfe) {
                                int targetId1 = Integer.parseInt(s1[3].split(idInfoSepChar)[3]);
                                int targetId2 = Integer.parseInt(s2[3].split(idInfoSepChar)[3]);
                                return targetId1 - targetId2;
                            }
                        }
                    }
                }
            } catch (Exception e){
                System.out.println("Bla");
                return -1;
            }
        }
    };
    
    // make sure that the output directories exist and are empty
    public static void setUpFolders() {
        
        File theDir = new File(outfolder);
        if (!theDir.exists()) {
            theDir.mkdir();
            if(DEBUG_PROMPT)
                System.out.println("Created folder:"  + outfolder);
        } else {
            for(File file: theDir.listFiles()) {
                file.delete();
            }
            if(DEBUG_PROMPT)
                System.out.println("Folder already existed / Emptied folder:"  + outfolder);
        }

        theDir = new File(tmpfolder);
        if (!theDir.exists()) {
            theDir.mkdir();
            if(VERBOSE)
                System.out.println("Created folder:"  + tmpfolder);
        } else {
            for(File file: theDir.listFiles()) {
                file.delete();
            }
            if(DEBUG_PROMPT)
                System.out.println("Folder already existed / Emptied folder:"  + tmpfolder);
        }
    }
    

    // read the list of all page IDs from an input file that are used to build the network
    //NOTE: what local files and what structure
    public int[][] readLocalContentIDs(HashMap<Integer, String> contentIdtoPageId) {
        ArrayList<int[]> pageIDs = new ArrayList<>();
        //int[][] pageIDs = new int[0][];
        try {
            if(VERBOSE)
                System.out.println("Reading page IDs (for each newspaper and for each year) from files in folder :"  + ID_FOLDER );
            File fileFolder = new File(ID_FOLDER);

        	int contentItemCount = 0;

        	//For each newspaper in the file folder, loop through and create an array of ids for
            for (File newspaper: fileFolder.listFiles()) {
        		if(newspaper.isDirectory()) { ;
                    TIntArrayList ids = new TIntArrayList();
                    for(File year: newspaper.listFiles()) {
                        if(year.getName().endsWith(".txt")){
                            if(DEBUG_PROMPT)
                                System.out.println("Reading file :"  +  year);
                            BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(year),"UTF-8"));

                            String line;
                            while ((line = bf.readLine()) != null) {
                                contentIdtoPageId.put(contentItemCount, line);
                                ids.add(contentItemCount);
                                contentItemCount ++;
                            }
                            bf.close();
                            if(DEBUG_PROMPT)
                                System.out.println("File read and closed");
                            pageIDs.add(ids.toArray());
                        }
                    }
        		}
        	}
            
            // convert to array of ints
            
        } catch (Exception e) {
            System.out.println("Problem reading page IDs from file.");
            e.printStackTrace();
            System.out.println(e);
        }
        int[][] output = new int[pageIDs.size()][];
        return pageIDs.toArray(output);
    }

    public int[][] readContentIDs(HashMap<Integer, String> contentIdtoPageId, String[] newspapers, String[][] years, SolrReader reader) throws IOException {
        ArrayList<int[]> pageIDs = new ArrayList<>();

        if(VERBOSE)
            System.out.println("Reading page IDs (for each newspaper and for each year) from Solr index");

        int id_cnt = 0;
        for(int i=0; i < newspapers.length; i++){
            if(DEBUG_PROMPT)
                System.out.println("Newspaper : " + newspapers[i]);
            String paper = newspapers[i];
            for(int j=0; j < years[i].length; j++){
                if(DEBUG_PROMPT)
                    System.out.println(sepChar + "Year : " + years[i][j]);
                String year = years[i][j];
                String folder_path = ID_FOLDER + paper + "-ids";
                File newspaper_folder = new File(folder_path);
                File year_file = new File(folder_path + "/" + year + ".txt");
                List<String> paper_year_ids;
                if (newspaper_folder.exists() && year_file.exists()) {
                    if(DEBUG_PROMPT)
                        System.out.println("IDS FOR " + year + " already exist. Reading from file");
                    paper_year_ids = FileUtils.readLines(year_file, "UTF-8");
                } else {
                    if(DEBUG_PROMPT)
                        System.out.println("Pulling ids from Solr for year " + year);
                    paper_year_ids = reader.getContentItemIDs(paper, year, true);
                }
                if(paper_year_ids.size() > 0) {
                    TIntArrayList ids = new TIntArrayList();
                    for (String id : paper_year_ids) {
                        contentIdtoPageId.put(id_cnt, id);
                        ids.add(id_cnt);
                        id_cnt++;
                    }
                    pageIDs.add(ids.toArray());
                }
            }
            if(DEBUG_PROMPT)
                System.out.println("Newspaper done");
        }

        int[][] output = new int[pageIDs.size()][];

        return pageIDs.toArray(output);
    }

    /* Main Routine: if readIdsFromFile = false;
     * Extraction of co-occurrences from the documents and building of nodes and (unaggregated) edges
     */
    public ParallelExtractNetworkFromImpresso(){
       this(new String[0], new String[0][]);
    }

    public ParallelExtractNetworkFromImpresso(String[] newspapers, String[][] years) {

        try {
            BufferedWriter ew = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfile),"UTF-8"), bufferSize);
            
            //Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
            //mongoLogger.setLevel(Level.WARNING);

          //Loads the property file
    		Properties prop = new Properties();
    		FileInputStream inputStream;
    		if(VERBOSE)
    		    System.out.println("Loading properties file :"  + PROP_PATH);
    		try {
    		    //NOTE: Input stream best solution?
    			inputStream = new FileInputStream(PROP_PATH);
    			prop.load(inputStream);
    			inputStream.close();
    		} catch (IOException e1) {
    			e1.printStackTrace();
    		}
    		if(VERBOSE)
    		    System.out.println("File was read and is now closed");
    		
            String accessKey = System.getenv("S3_ACCESS_KEY");
    		String secretKey = System.getenv("S3_SECRET_KEY");

    		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    		
    		// Set S3 Client Endpoint
            AwsClientBuilder.EndpointConfiguration switchEndpoint = new AwsClientBuilder.EndpointConfiguration(
                    prop.getProperty("s3BaseName"),"");
            if(DEBUG_PROMPT)
                System.out.println("S3 endpoint setup for the S3 base :"  + prop.getProperty("s3BaseName"));

        	// Set signer type and http scheme
            ClientConfiguration conf = new ClientConfiguration();
            	    conf.setSignerOverride("S3SignerType");
            	    conf.setSocketTimeout(TIMEOUT); //doubles default timeout
    		        conf.setProtocol(Protocol.HTTPS);
                    
            AmazonS3 S3Client = AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(switchEndpoint)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withClientConfiguration(conf)
                    .withPathStyleAccessEnabled(true)
                    .build();

            if(VERBOSE)
                System.out.println("S3 client created");

            //Instantiating SolrReader
            SolrReader solrReader = new SolrReader(prop);
            if(VERBOSE)
                System.out.println("Solr reader created");

            if(VERBOSE)
                System.out.println("\nGET ID OF ALL ARTICLES\n");

            // get the IDs of all pages of interest
            HashMap<Integer, String> contentIdtoPageId = new HashMap<Integer, String>();

            int[][] pageIDs;
            System.out.println("Generating page IDs from database.");
            if(newspapers.length == 0)
                pageIDs = readLocalContentIDs(contentIdtoPageId);
            else {
                 pageIDs = readContentIDs(contentIdtoPageId, newspapers, years, solrReader);
            }
            count_Articles = contentIdtoPageId.size();
            if(VERBOSE)
                System.out.println("Number of pages with annotations (i.e number of articles):"  + contentIdtoPageId.size());
            System.gc();

            //Create HashMap based on known number of annotations
            valueToIdMaps = new ArrayList<>();
            for (int i=0; i<nANNOTATIONS; i++) {
                valueToIdMaps.add(new TObjectIntHashMap<>());
            }
            currentIDs = new int[nANNOTATIONS];

            //Caches of tokens and entities created with size limits
        	Cache<String, JSONObject> newspaperCache = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).build(); //NOTE: Add max if necessary
        	Cache<String, JSONArray> entityCache = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).build();

        	if(VERBOSE) {
                System.out.println("Newspaper cache and Entity cache created");
/*                if (DEBUG_PROMPT)
                    System.out.println("Cache max size ="  + MAX_CACHE_SIZE);
*/          }

            if(VERBOSE)
                System.out.println("\nPARSING ANNOTATIONS AND EXTRACTING NETWORK\n");

            HashSet<String> invalidTypes;
            MultiThreadHubImpresso hub = new MultiThreadHubImpresso(pageIDs, valueToIdMaps, currentIDs, S3Client, newspaperCache, entityCache, prop, contentIdtoPageId, ew, nThreads);
            if(DEBUG_PROMPT)
                System.out.println(String.format("Multi thread hub created for %s threads",  nThreads));

            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            for (int i=0; i<nThreads; i++) {
                MultiThreadWorkerImpresso w = new MultiThreadWorkerImpresso(hub, prop, solrReader, newspaperCache, entityCache, contentIdtoPageId, i);
                executor.execute(w);
                if(DEBUG_PROMPT)
                    System.out.println(String.format("Thread %d executed",  i));
            }
            executor.shutdown();
            
            while (true) {
                try {
                    hub.latch.await();
                    break;
                } catch (InterruptedException e) {
                    //System.out.println();
                    System.out.println("Waiting was interrupted (main)");
                }
            }

            ew.close();
            //System.out.println();
            
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
            System.out.print("  Invalid Types:");
            for (String s : invalidTypes) {
                System.out.print(""  + s);
            }
            //System.out.println();
            
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

            w.append("# OPTION - maximum distance in sentences for edge creation: " + maxDistanceInSentences +"\n");
            w.append("# Number of pages with annotations: " + count_Articles +"\n");
            w.append("# Number of sentences: " + count_Sentences +"\n");
            w.append("# Number of sentences with annotations: " + count_ValidAnnotationsByType[SEN] +"\n");
            w.append("# Number of annotations: " + count_Annotations +"\n");
            w.append("# Number of valid annotations: " + count_ValidAnnotations +"\n");
            w.append("# Number of unaggregated edges: "  + count_unaggregatedEdges +"\n");
            for (int i=0; i<4; i++) {
                w.append("# Valid annotations of type "  + setNames[i] +" :" + count_ValidAnnotationsByType[i]+"\n");
            }

            w.append(metaHeader);
            for (int i=0; i<nANNOTATIONS; i++) {
                w.append(setNames[i] + sepChar + valueToIdMaps.get(i).size() +"\n");
            }
            w.close();
            
            // write temporary node data to get rid of the valueToIdMaps
            System.out.println("Writing temporary node data");
            for (int i=0; i<nANNOTATIONS; i++) {
                w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfolder +"tmp_" + vertexFileNames[i]),"UTF-8"), bufferSize);
                for (TObjectIntIterator<String> it = valueToIdMaps.get(i).iterator(); it.hasNext(); ) {
                    it.advance();
                    String s = it.value() + sepChar + it.key() +"\n";
                    w.append(s);
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
        try {
            BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(sortedtmpfile),"UTF-8"));
            
            ArrayList<BufferedWriter> out = new ArrayList<BufferedWriter>();
            for (int i=0; i<nANNOTATIONS; i++) {
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfolder +"tmp_" + edgeFileNames[i]),"UTF-8"), bufferSize);
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
                    System.out.print("\rRead"  + dform.format(promillecount) +"% of unaggregated edges." );
                }
                
                splitline = line.split(sepChar);
                char n1 = splitline[0].charAt(0);
                char n2 = splitline[1].charAt(0);
                int n3 = Integer.parseInt(splitline[2]);
                int n4;
                try {
                    n4 = Integer.parseInt(splitline[3]);
                } catch (NumberFormatException nfe){
                    n4 = Integer.parseInt(splitline[3].split(idInfoSepChar)[3]);
                }
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
                        out.get(sourceType).append(sourceType + sepChar + targetType + sepChar + sourceId + sepChar + targetId + sepChar + (int)weight +"\n");
                        out.get(targetType).append(targetType + sepChar + sourceType + sepChar + targetId + sepChar + sourceId + sepChar + (int)weight +"\n");
                    } else {
                        out.get(sourceType).append(sourceType + sepChar + targetType + sepChar + sourceId + sepChar + targetId + sepChar + df.format(weight) +"\n");
                        out.get(targetType).append(targetType + sepChar + sourceType + sepChar + targetId + sepChar + sourceId + sepChar + df.format(weight) +"\n");
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
            //System.out.println();
            
            // write last active edge
            if (targetType >= TER) {
                out.get(sourceType).append(sourceType + sepChar + targetType + sepChar + sourceId + sepChar + targetId + sepChar + (int)weight +"\n");
                out.get(targetType).append(targetType + sepChar + sourceType + sepChar + targetId + sepChar + sourceId + sepChar + (int)weight +"\n");
            } else {
                out.get(sourceType).append(sourceType + sepChar + targetType + sepChar + sourceId + sepChar + targetId + sepChar + df.format(weight) +"\n");
                out.get(targetType).append(targetType + sepChar + sourceType + sepChar + targetId + sepChar + sourceId + sepChar + df.format(weight) +"\n");
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
            
            String inputfile = tmpfolder +"tmp_" + edgeFileNames[i];
            String outputfile = tmpfolder +"tmp_sorted_" + edgeFileNames[i];
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
                
                String inFile = tmpfolder +"tmp_sorted_" + edgeFileNames[type];
                BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(inFile),"UTF-8"));
                String outFile = outfolder + edgeFileNames[type];
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile),"UTF-8"), bufferSize);
                
                String line = bf.readLine();
                
                int currentSource = 0;
                char targetType = 0;
                w.append(0 +"\n");
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
                        w.append("\n\n" + currentSource +"\n");
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
                System.out.print("Rewriting nodes for"  + setNames[i] +".");
                String[] nodeNames = new String[setSizes[i]];
                String inFile = tmpfolder +"tmp_" + vertexFileNames[i];
                BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(inFile),"UTF-8"));
                String line;
                while ((line = bf.readLine()) != null) {
                    String[] splitline = line.split(sepChar);
                    int id = Integer.parseInt(splitline[0]);
                    String name ="" ;
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
            if(PRINT_TO_FILE){
                File file = new File("log.txt");
                System.out.println("From now on "+file.getAbsolutePath()+" will be your console");
                //Instantiating the PrintStream class
                PrintStream stream = new PrintStream(file);
                System.setOut(stream);
            }
            long start_time = System.currentTimeMillis();
            // make sure that working folders are up and clean
            if(VERBOSE)
                System.out.println("\nSETTING UP THE FOLDERS FOR THE ENVIRONMENT\n");
            setUpFolders();

            if(VERBOSE) {
                System.out.println("Setting up the folders: DONE");
                System.out.println("===============================");
            }

            // read the input data from DB and write temporary edge information of unaggregated edge lists
            if(VERBOSE)
                System.out.println("\nIMPORT THE DATA FROM IMPRESSO AND S3\n");

            System.gc();

            ParallelExtractNetworkFromImpresso enfm;

            if(!readIDsFromFile){
                String[] newspapers = args[0].split(",");
                String[][] years = new String[newspapers.length][];

               if(args.length == 1){
                    for(int i = 0; i < newspapers.length; i++)
                        years[i] = IntStream.range(MIN_YEAR, MAX_YEAR).mapToObj(String::valueOf).toArray(String[]::new);
                } else if(args.length != newspapers.length + 1){
                    throw new IllegalArgumentException("You must have as many year lists as you have newspapers");
                } else {
                   for (int i = 1; i < args.length; i++)
                       years[i - 1] = args[i].split(",");
               }
               enfm = new ParallelExtractNetworkFromImpresso(newspapers, years);
            } else {
                enfm = new ParallelExtractNetworkFromImpresso();

            }
            System.gc();



            if(VERBOSE)
                System.out.println("Data imported, entities extracted, edges created.");
            // write temporary node information to clear memory

            if(VERBOSE)
                System.out.println("\nWRITING NODES TO FILE (to free up space)\n");
            enfm.writeTemporaryNodesToFiles();
            System.gc();
            
            // sort the list of unaggregated (duplicate) edge lists with external merge sort
            if(VERBOSE)
                System.out.println("\nSORT EDGE LIST\n");
            enfm.sortUnaggregatedEdgelistExternally();
            System.gc();
            
            // aggregate the edge lists to remove parallel edges and split them into individual files
            // NOTE: reciprocal edges are introduced in this step (i.e. edges appear in two files!)
            if(VERBOSE)
                System.out.println("\nAGGREGATE EDGES (remove parallel edges) SPLIT EDGES INTO FILES\n");
            enfm.aggregateEdgesAndSplitIntoIndividualFiles();
            System.gc();
            
            // sort the aggregated edge lists to prepare for writing them to adjacency lists
            if(VERBOSE)
                System.out.println("\nSORT EDGE FILES\n");
            enfm.sortIndividualEdgeFiles();
            System.gc();
            
            // compute degrees, then finalize nodes and edges
            enfm.finalizeNodesAndEdges();
            long end_time = System.currentTimeMillis();
            System.out.println("Done. Runtime: " + (end_time - start_time) + "ms \n");
            System.out.println("Output dir:" + folder);
            if(BUILD_MONGO_DB) {
                System.out.println("BUILDING GRAPH TO MONGODB");
                MoveCompleteLOADNetworkToMongoDB.loadGraphToDB();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
