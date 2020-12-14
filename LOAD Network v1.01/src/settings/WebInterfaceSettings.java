package settings;

import com.mongodb.MongoCredential;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.text.SimpleDateFormat;

/**
 * Web Query settings for the LOAD graph query interface
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class WebInterfaceSettings {
	
	// logfile path
	public static String logfileName = "/home/aspitz/LOADdemo.log/"; // should be /data/EVELIN/evelin_backend.log

	// local mongoDB settings
	public static String localMongoAddress = "127.0.0.1";
	public static int localMongoPort = 27017;
	public static boolean LOCAL = true;
	// mongoDB login settings
	public static String MongoAdress = "metis.ifi.uni-heidelberg.de"; //"<mongo server adress>";
	public static int MongoPort = 27020;
	public static String password = "password";
	public static String username = "username";
	public static String auth_db = "name of authentication DB";
	//public static MongoCredential mongocred = MongoCredential.createCredential(username, auth_db, password.toCharArray());
	public static MongoCredential mongocred = null;
	
	// mongoDB database and collection names
	public static String MongoName_LOAD = "IMPRESSO_LOAD_1890_GDL_TEST"; //"LOAD_CivilWar_Wikipedia_en_20160501";
	public static String MongoCollection_Meta = "metaData";
	public static String MongoCollection_Edges = "edgesPlusPages";
	public static String MongoCollection_NodeDegrees = "nodesDegrees";
	public static String MongoCollection_Sentences = "nodesSentences";
	public static String MongoCollection_Pages = "nodesPages";
	public static String MongoCollection_Terms = "nodesTerms";
	public static String MongoCollection_Entities = "nodesEntities";

	// COLLECTION NodeDegrees: mongoDB field identifier
	public static String ci_NodeDegrees_id = "node_id";
	public static String ci_NodeDegrees_type = "type";
	public static String ci_NodeDegrees_degrees = "degrees";
	
	// COLLECTION Sentences: mongoDB field identifier
	public static String ci_Sentences_occurence_id = "node_id";
	public static String ci_Sentences_id = "sentence_id";
	public static String ci_Sentences_text = "text";
	public static String ci_Sentences_article_id = "article_id";
	public static String ci_min_offset = "min_offset";
	public static String ci_max_offset = "max_offset";
	
	// COLLECTION Pages: mongoDB field identifier
	public static String ci_Page_id = "node_id";
	public static String ci_Page_title = "title";
	public static String ci_Page_wikiid = "wikipedia_id";
	public static String ci_article_id = "article_id";

	// COLLECTION Terms: mongoDB field identifier
	public static String ci_Terms_id = "node_id";
	public static String ci_Terms_label = "label";
	public static String ci_Terms_senDegree = "sentence_degree";
	
	// COLLECTION OUT Entities: mongoDB field identifier
	public static String ci_Entity_node_id = "node_id";
	public static String ci_Entity_id = "entity_id";
	public static String ci_Entity_type = "type";
	public static String ci_Entity_wikiid = "wikidata_id";
	public static String ci_Entity_senDegree = "sentence_degree";
	public static String ci_Entity_label = "label";
	public static String ci_Entity_description = "description";
	public static String ci_Entity_score = "score";					// this is not in the DB but a text query meta score!
	
	// COLLECTION OUT Edges: mongoDB field identifier
	public static String ci_Edge_SourceType = "s_type";
	public static String ci_Edge_TargetType = "t_type";
	public static String ci_Edge_SourceID = "s_id";
	public static String ci_Edge_TargetID = "t_id";
	public static String ci_Edge_Weight = "weight";
	public static String ci_Edge_TFIDF = "tfidf";		// note that this weight is NOT symmetric and only valid for source -> target
	public static String ci_Edge_PageID = "page_id";	// only present on edges with targetType = SEN (to avoid one more lookup for PAG queries

	// LOAD OUTPUT mongoDB database and collection names
	public static boolean TEST = false;
	public static String target_MongoName_LOAD = TEST ? "IMPRESSO_LOAD_1890_GDL_TEST" : "IMPRESSO_LOAD_FINAL"; //"LOAD_CivilWar_Wikipedia_en_20160501"; //"LOAD_Wikipedia_en_20160501"; "LOAD_Evelin_Demo_Wikipedia_en_20160501";//"<name of mongo Db database>";
	public static String target_MongoCollection_Meta = "metaData";
	public static String target_MongoCollection_Edges = "edgesPlusPages";
	public static String target_MongoCollection_NodeDegrees = "nodesDegrees";
	public static String target_MongoCollection_Sentences = "nodesSentences";
	public static String target_MongoCollection_Pages = "nodesPages";
	public static String target_MongoCollection_Terms = "nodesTerms";
	public static String target_MongoCollection_Entities = "nodesEntities";
	
	// Date formatter
	public static SimpleDateFormat short_dateFormatter10 = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat short_dateFormatter7 = new SimpleDateFormat("yyyy-MM");
	public static SimpleDateFormat long_dateFormatter10 = new SimpleDateFormat("d MMMMM yyyy");
	public static SimpleDateFormat long_dateFormatter7 = new SimpleDateFormat("MMMMM yyyy");

	// language for stemmer to be used for stemming terms. By default, an English stemmer is used.
	// However, other language versions are available / included and can be used.
	// Implementation of the Porter stemmer from http://snowball.tartarus.org/
	// NOTE: french stemmer possibly bugged in the original implementation and may require fixing.
	public static SnowballStemmer getStemmer(String lang) {
		if (lang.equals("en")) {
			return new englishStemmer();
		} else {
			System.out.println("Unknown stemmer language code. Using English stemmer.");
			return new englishStemmer();
		}
	}
	
	// language for stemmer to be used for stemming terms. By default, an English stemmer is used.
	// However, other language versions are available / included and can be used.
	// Implementation of the Porter stemmer from http://snowball.tartarus.org/
	// NOTE: french stemmer possibly bugged in the original implementation and may require fixing.

	// Wikipedia Page URL prefix that can be turned into a proper URL by adding a page ID
	public static String wikipediaPrefixURL = "http://en.wikipedia.org/?curid=";
	
	// Wikipedia Page URL prefix
	public static String wikidataPrefixURL = "http://www.wikidata.org/entity/";
	
	// cohesion factor for multi-entity queries
	public static int cohesion = 2;
	
	// maximum number of relevant terms to consider for each query entity in sentence queries
	public static int sentenceTermLimit = 2;
	
	// maximum number of neighbours to consider per type for each query entity in subgraph queries
	public static int graphNeighbourLimit = 3;
	
	// minimum weight for edges to be considered in a subgraph
	public static double subgraphEdgeWeightLimit = 1.0;
}
