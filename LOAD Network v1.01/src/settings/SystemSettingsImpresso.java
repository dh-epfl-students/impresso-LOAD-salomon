package settings;

import com.mongodb.MongoCredential;

/**
 * System Settings for both LOAD graph construction and query interface.
 * Change system dependent parameters here.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class SystemSettingsImpresso {
    
    /* System dependent settings for using the model
     * Adapt these settings to your system or environment.
     */
    
    // The program requires a working directory for writing temporary and final output
    public static String folder ="<LOAD graph output folder>"; // e.g. /home/username/LOAD/
 
    //Here originally was stop-text to be removed, however the impresso will be using lemmitimization and choosing the relevant parts of speech so
    //this is unnecessary
    
    // The program is designed to only process pages (documents) that contain annotations.
    // To do this, it will scan all annotations and extract the IDs of pages. They will also be stored
    // in a file so that this process does not have to be repeated when the program is run again on
    // the same data. In this case, set the parameter to TRUE.
    public static boolean readIDsFromFile = false;
    
    // number of threads that are used for network construction
    // since the program is bounded by the speed of the database, setting this above the number
    // of cores on your system has no beneficial effect.
    public static int nThreads = 20;
    
    // mongoDB login settings. if your mongoDB has no authentication data, you can skip entering a password,
    // username and authentication DB and just set mongocred to NULL.
  /*  public static String MongoAdress ="<mongo Db server address>";        // server name or IP address of the mongo DB
    public static int MongoPort = 27017;                                // port of the mongoDB
    public static String password ="password";
    public static String username ="username";
    public static String auth_db ="name of authentication DB";
    //public static MongoCredential mongocred = MongoCredential.createCredential(username, auth_db, password.toCharArray());
    public static MongoCredential mongocred = null;
    
    // mongoDB database and collection names
    public static String MongoDBname ="<name of mongo Db database>";        // database that contains the annotation data
    public static String MongoCollectionSentences ="<sentences>";        // collection of sentences
    public static String MongoCollectionAnnotations ="<annotations>";    // collection of annotations */
    
    // Solr database, single database with both the Words and Annotations
    public static String SolrDBname ="https://solrdev.dhlab.epfl.ch/solr/impresso_dev";
    
    // names of names entity classes in the mongoDB collection annotation
    // For the impresso DB we will be using PER and LOC
    public static final String loc ="LOC";
    public static final String act ="PER";
        
    //S3 DB value identifiers(article id ?)
    public static String s3IdentWord_id ="_id"; //id of the word in entire collection
    public static String s3IdentWord_pageId ="doc_id";          // handle for document IDs
    public static String s3IdentWord_wordId ="word_number_id";  // handle for sentenceID by page
    public static String s3IdentWord_content ="content"; //Need to think of the distance relation based on word vs sentence and implications of this
    //the original content variable was called"handle for sentence content" not sure if this is still relevant without sentences
    
    /*
    // mongoDB collection value identifiers in collection annotations
    public static String mongoIdentAnnotation_id ="_id";
    public static String mongoIdentAnnotation_pageId ="WP_page_id";
    public static String mongoIdentAnnotation_coveredText ="coveredText";
    public static String mongoIdentAnnotation_normalized ="normalized";
    public static String mongoIdentAnnotation_sentenceId ="sen_id";
    public static String mongoIdentAnnotation_start ="start_sen";
    public static String mongoIdentAnnotation_end ="end_sen";
    public static String mongoIdentAnnotation_neClass ="neClass";
    public static String mongoIdentAnnotation_tool ="tool";
    */
    // language for stemmer to be used for stemming terms. By default, an English stemmer is used.
    // However, other language versions are available / included and can be used.
    // Implementation of the Porter stemmer from http://snowball.tartarus.org/
    // NOTE: french stemmer possibly bugged in the original implementation and may require fixing.
    public static String stemmerLanguage ="en";
    
    // Buffer size for file reader / writer. Setting this too ow will impact performance
    public static int bufferSize = 8 * 1024 * 1024 * 4; // 4MB
    
    // maximum number of temporary files for sorting
    // setting this too low may create memory problems. Setting it too high may collide with
    // the maximum number of allowed open files on your machine.
    public static int maxTempFiles = 50;


}
