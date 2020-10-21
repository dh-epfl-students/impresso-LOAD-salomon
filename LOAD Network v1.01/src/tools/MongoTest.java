package tools;


import static settings.SystemSettings.*;

// mongoDB imports
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uses information stored in SystemSettings to test the connection to the mongoDB.
 * Use this to validate your DB before attempting to run the LOAD construction.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class MongoTest {

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
            
            // list all databases
            System.out.println("Databases:");
            ListDatabasesIterable<Document> dl = mongoClient.listDatabases(); 
            for (Document d : dl) {
                System.out.println(d.toString());
            }
            System.out.println();
            
            // connect to the specified database and list all collections
            MongoDatabase db = mongoClient.getDatabase(MongoDBname);
            ListCollectionsIterable<Document> cl = db.listCollections();
            System.out.println("Collections in Database:");
            for (Document c : cl) {
                System.out.println(c.toString());
            }
            System.out.println();
            
            // print an item from the collection sentences
            MongoCollection<Document> collS = db.getCollection(MongoCollectionSentences);
            System.out.println("One item from collection sentences:");
            System.out.println(collS.find().skip(1).first());
            System.out.println();
            
            // list indexes for collection sentences
            ListIndexesIterable<Document> indexS = collS.listIndexes();
            System.out.println("Indexes for collection sentences:");
            for (Document i : indexS) {
                System.out.println(i.toString());
            }
            System.out.println();
            
            // print an item from the collection annotations
            MongoCollection<Document> collA = db.getCollection(MongoCollectionAnnotations);
            System.out.println("One item from collection annotations:");
            System.out.println(collA.find().skip(1).first());
            System.out.println();
            
            // list indexes for collection sentences
            ListIndexesIterable<Document> indexA = collA.listIndexes();
            System.out.println("Indexes for collection annotations:");
            for (Document i : indexA) {
                System.out.println(i.toString());
            }
            
            // close database connection
            mongoClient.close();
                        
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
