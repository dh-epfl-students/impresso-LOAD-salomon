package tools;

import static settings.LOADmodelSettings.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// mongoDB imports
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Reads a LOAD graph from the internally use file structure and stores it in a mongoDB.
 * This output is not used anywhere in the LOAD system and serves as convenient external data access only.
 * 
 * Note: adjusting this piece of code to write a five columns edge list in the format
 * <node1 id> <node2 id> <node1 type> <node 2 type> <edge weight>
 * to a file instead of a mongoDB is a simple matter. The output happens around line 152
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class MoveLOADNetworkToMongoDB {

    // input folder with graph data
    private static String folder ="<LOAD graph folder>"; // e.g. /home/username/LOAD/graph_output/
    
    // mongoDB login settings
    public static String MongoAdress ="<mongo server adress>";
    public static int MongoPort = 27017;
    public static String password ="password";
    public static String username ="username";
    public static String auth_db ="name of authentication DB";
    //public static MongoCredential mongocred = MongoCredential.createCredential(username, auth_db, password.toCharArray());
    public static MongoCredential mongocred = null;
        
    // mongoDB database and collection names
    public static String MongoDBname ="<name of mongo Db database>";
    public static String MongoCollectionEdges ="edges";
    public static String MongoCollectionNodes ="nodes";
    
    // insertion bulk size for mongo DB
    public static int bulkSize = 1000;
    
    // mongoDB field identifier
    public static String mongoIdentNodeType ="nodeType";
    public static String mongoIdentNodeID ="nodeID";
    public static String mongoIdentNodeLabel ="nodeLabel";
    public static String mongoIdentWDLabel ="WDlabel";
    public static String mongoIdentDegDAT ="degDAT";
    public static String mongoIdentDegLOC ="degLOC";
    public static String mongoIdentDegACT ="degACT";
    public static String mongoIdentDegORG ="degORG";
    public static String mongoIdentDegTER ="degTER";
    public static String mongoIdentDegPAG ="degPAG";
    public static String mongoIdentDegSEN ="degSEN";
    
    public static String mongoIdentSourceType ="sourceType";
    public static String mongoIdentTargetType ="targetType";
    public static String mongoIdentSourceID ="sourceID";
    public static String mongoIdentTargetID ="targetID";
    public static String mongoIdentEdgeWeight ="weight";
    
    // read the number of nodes for each set from the meta data file
    public static int[] getSetSizes() {
        
        HashMap<String,Integer> setNamesToIndexMap = new HashMap<String,Integer>();
        for (int i=0; i < nANNOTATIONS; i++) {
            setNamesToIndexMap.put(setNames[i], i);
        }
        int[] nNodes = new int[nANNOTATIONS];
        
        try {            
            // read meta data from file to obtain number of vertices of different types
            String line;
            String filename = folder + metaFileName; 
            System.out.println("Reading graph meta data");
            BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;
                
                String[] splitline = line.split(sepChar); 
                Integer set = setNamesToIndexMap.get(splitline[0]);
                if (set != null) {
                    nNodes[set] = Integer.parseInt(splitline[1]);
                } else {
                    System.out.println("Unknown line specificer in metadata file:");
                    System.out.println(line);
                }
            }
            br.close();
            
            // sanity check: are any vertex sets of size 0?
            for (int i=0; i < nANNOTATIONS; i++) {
                if (nNodes[i] == 0) {
                    System.out.println("Warning! Number of nodes is 0 for" + setNames[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nNodes;
    }
    
    // write nodes to mongoDB and ensure that an index exists
    public static void writeNodesToMongo(MongoCollection<Document> cNodes) {
            
        int[] nNodes = getSetSizes();
        
        try {
            // read vertex information
            for (int i=0; i < nANNOTATIONS; i++) {
                String filename = folder + vertexFileNames[i];
                System.out.print("Reading nodes of type" + setNames[i]);
                int index = 0;
                int steps = nNodes[i]/100;
                if (steps == 0) steps = 1;
                String line;
                ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);

                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename),"UTF-8"));
                while ((line = br.readLine()) != null) {
                    if (index % steps == 0) {
                        int progress = (int) (100.0*index/nNodes[i]);
                        System.out.print("\rReading nodes of type" + setNames[i] +":"+ String.format("%1$"+ 3 +"s", progress) +"%");
                    }
                    String[] splitline = line.split(sepChar);
                    
                    String value = splitline[0];
                                        
                    int degDAT = Integer.parseInt(splitline[1]);
                    int degLOC = Integer.parseInt(splitline[2]);
                    int degACT = Integer.parseInt(splitline[3]);
                    int degORG = Integer.parseInt(splitline[4]);
                    int degTER = Integer.parseInt(splitline[5]);
                    int degPAG = Integer.parseInt(splitline[6]);
                    int degSEN = Integer.parseInt(splitline[7]);
                    
                    // writing an edge list to a file instead of a mongo DB can be implemented here
                    
                    Document node = new Document().append(mongoIdentNodeType, setNames[i])
                                                  .append(mongoIdentNodeID, index)
                                                  .append(mongoIdentNodeLabel, value)
                                                  .append(mongoIdentDegDAT, degDAT)
                                                  .append(mongoIdentDegLOC, degLOC)
                                                  .append(mongoIdentDegACT, degACT)
                                                  .append(mongoIdentDegORG, degORG)
                                                  .append(mongoIdentDegTER, degTER)
                                                  .append(mongoIdentDegPAG, degPAG)
                                                  .append(mongoIdentDegSEN, degSEN);

                    index++;
                    insertList.add(node);
                    if (insertList.size() == bulkSize) {
                        cNodes.insertMany(insertList);
                        insertList.clear();
                    }
                }
                cNodes.insertMany(insertList);
                insertList.clear();
                br.close();
                
                System.out.println("\rReading nodes of type" + setNames[i] +": done");
            }
            
        } catch (Exception e) {
            System.out.println("Unable to read graph from files");
            e.printStackTrace();
        }
    }
    
    // write nodes to mongoDB and ensure that an index exists
    public static void writeEdgesToMongo(MongoCollection<Document> cEdges) {
        
        int[] nNodes = getSetSizes();
        
        try {
            // read node information
        
            for (int i=0; i < nANNOTATIONS; i++) {
                String filename = folder + edgeFileNames[i];
                System.out.print("Reading edges of type" + setNames[i]);
                int index = 0;
                int steps = nNodes[i]/100;
                if (steps == 0) steps = 1;
                
                @SuppressWarnings("unused")
                String line;
                
                ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);
                
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename),"UTF-8"));
                do {
                    if (index % steps == 0) {
                        int progress = (int) (100.0*index/nNodes[i]);
                        System.out.print("\rReading edges of type" + setNames[i] +":"+ String.format("%1$"+ 3 +"s", progress) +"%");
                    }
                    line = br.readLine();

                    for (int k=0; k < nANNOTATIONS; k++) {
                        String[] splitline = br.readLine().split(sepChar);
                        
                        String sourceType = setNames[i];
                        String targetType = setNames[k];
                        
                        for (int lineIndex=1; lineIndex<splitline.length; ) {
                            int targetID = Integer.parseInt(splitline[lineIndex++]);
                            double weight = Float.parseFloat(splitline[lineIndex++]);
                            int sourceID = index;
                            
                            

                            Document edge = new Document().append(mongoIdentSourceType, sourceType)
                                                            .append(mongoIdentTargetType, targetType)
                                                            .append(mongoIdentSourceID, sourceID)
                                                            .append(mongoIdentTargetID, targetID)
                                                            .append(mongoIdentEdgeWeight, weight);
                            
                            insertList.add(edge);
                            if (insertList.size() == bulkSize) {
                                cEdges.insertMany(insertList);
                                insertList.clear();
                            }
                        }
                    }
                    ++index;
                } while ((line = br.readLine()) != null);
                   cEdges.insertMany(insertList);
                   insertList.clear();
                
                System.out.println("\rReading edges of type" + setNames[i] +": done");
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void createIndices(MongoCollection<Document> cEdges, MongoCollection<Document> cNodes) {
        
        // create indices
        System.out.println("Creating index for node collection.");
        cNodes.createIndex(new Document(mongoIdentNodeType, 1).append(mongoIdentNodeID, 1));
        
        // create indices for edge collection
        System.out.println("Creating index for edge collection.");
        cEdges.createIndex(new Document(mongoIdentSourceType, 1).append(mongoIdentTargetType, 1).append(mongoIdentSourceID, 1));
    }
    
    
    public static void main(String[] args) {
        
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
        MongoCollection<Document> cEdges = db.getCollection(MongoCollectionEdges);
        MongoCollection<Document> cNodes = db.getCollection(MongoCollectionNodes);
        
        // remove existing collections
        cEdges.drop();
        cNodes.drop();

        // read nodes from file and write to MongoDB
        writeNodesToMongo(cNodes);
        
        // read edges from file and write to MongoDB
        writeEdgesToMongo(cEdges);
        
        // build indices for the final data bases
        createIndices(cEdges, cNodes);
        
        mongoClient.close();        
    }

}
