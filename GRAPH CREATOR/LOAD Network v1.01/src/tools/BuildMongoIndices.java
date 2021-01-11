package tools;


import static settings.SystemSettings.*;

// mongoDB imports
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Can be used to build the indices on the collections" sentences" and" annotations"
 * that are required to run the LOAD approach efficiently.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class BuildMongoIndices {

    public static void main(String[] args) {
        
        try {
            
            // set logger output level to only display warnings and errors
            Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
            mongoLogger.setLevel(Level.WARNING);
            
            // connect to mongodb server
            ServerAddress address = new ServerAddress(MongoAdress, MongoPort);
            MongoClient mongoClient;
            if (mongocred != null) {
                mongoClient = new MongoClient(address, Arrays.asList(mongocred));
            } else {
                mongoClient = new MongoClient(address);
            }
            
            // connect to the specified database
            MongoDatabase db = mongoClient.getDatabase(MongoDBname);
            MongoCollection<Document> collS = db.getCollection(MongoCollectionSentences);
            MongoCollection<Document> collA = db.getCollection(MongoCollectionAnnotations);
            
            // build indices
            System.out.println("Ensuring indices:");
            collA.createIndex(new Document(mongoIdentAnnotation_pageId, 1).append(mongoIdentAnnotation_sentenceId, 1));
            collS.createIndex(new Document(mongoIdentSentence_pageId, 1));
            
            // close database connection
            mongoClient.close();
                        
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
