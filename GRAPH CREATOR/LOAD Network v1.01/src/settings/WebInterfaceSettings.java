package settings;

import com.mongodb.MongoCredential;

/**
 * Web Query settings for the EVELIN LOAD graph query interface
 *
 * Originally published Jan 2017 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2017 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class WebInterfaceSettings {

    public static class Builder {
        private String logfileName = "/home/aspitz/LOADdemo.log/";  	// path to logfile. Should be /data/EVELIN/evelin_backend.log on metis
        private String MongoAdress = "127.0.0.1"; 	// address of the server running the mongo DB
        private int MongoPort = 27017;									// port of the mongo DB
        private String password = "";									// mongoDb password
        private String username = "";									// mongoDB username
        private String auth_db = "name of authentication DB";			// authentication DB used for mongoDB
        private String MongoDBName = "IMPRESSO_LOAD_FINAL";//"LOAD_YAGO_Wikipedia_en_20161201"; // database name inside the mongoDB
        private boolean verbose = true;									// if true: print everything to console. If false: only print to logfile
        private long maxQueryDuration = 60000; 							// maximum runtime allowed before query termination in milliseconds
        private int maxQueryCount = 3;									// maximum number of active queries per device
        private int maxNumberOfQueryEntities = 5;						// maximum number of query entities per query
        private int cohesion = 2;										// cohesion factor for multi-entity subgraph queries
        private int sentenceTermLimit = 2;								// number of relevant terms per query entity used for sentence ranking
        private int graphNeighbourLimit = 3;							// maximum number of neighbours to consider per type for each query entity in subgraph queries
        private double subgraphEdgeWeightLimit = 1.0;					// minimum weight for edges to be considered in a subgraph
        private int maxSentenceLength = 300;							// maximum sentence length for sentence ranking
        private boolean useSSHtunnel = false;							// toggle use of SSH tunnel for connecting to mongoDB
        private String SSHHost;
        private int SSHport;
        private String LDAPUsername;
        private String LDAPPassword;

        public Builder() {

        }

        public Builder build() {
            return this;
        }

        public Builder logfileName(String logfileName) {
            this.logfileName = logfileName;
            return this;
        }

        public Builder MongoAdress(String MongoAdress) {
            this.MongoAdress = MongoAdress;
            return this;
        }

        public Builder MongoPort(int MongoPort) {
            this.MongoPort = MongoPort;
            return this;
        }

        public Builder Mongopassword(String password) {
            this.password = password;
            return this;
        }

        public Builder Mongousername(String username) {
            this.username = username;
            return this;
        }

        public Builder auth_db(String auth_db) {
            this.auth_db = auth_db;
            return this;
        }

        public Builder MongoDBName(String MongoDBName) {
            this.MongoDBName = MongoDBName;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder maxQueryDuration(long maxQueryDuration) {
            this.maxQueryDuration = maxQueryDuration;
            return this;
        }

        public Builder cohesion(int cohesion) {
            this.cohesion = cohesion;
            return this;
        }

        public Builder maxNumberOfQueryEntities(int maxNumberOfQueryEntities) {
            this.maxNumberOfQueryEntities = maxNumberOfQueryEntities;
            return this;
        }

        public Builder maxQueryCount(int maxQueryCount) {
            this.maxQueryCount = maxQueryCount;
            return this;
        }
        public Builder sentenceTermLimit(int sentenceTermLimit) {
            this.sentenceTermLimit = sentenceTermLimit;
            return this;
        }
        public Builder graphNeighbourLimit(int graphNeighbourLimit) {
            this.graphNeighbourLimit = graphNeighbourLimit;
            return this;
        }
        public Builder subgraphEdgeWeightLimit(double subgraphEdgeWeightLimit) {
            this.subgraphEdgeWeightLimit = subgraphEdgeWeightLimit;
            return this;
        }
        public Builder maxSentenceLength(int maxSentenceLength) {
            this.maxSentenceLength = maxSentenceLength;
            return this;
        }
        public Builder useSSHtunnel(boolean useSSHtunnel) {
            this.useSSHtunnel = useSSHtunnel;
            return this;
        }
        public Builder SSHHost(String SSHHost) {
            this.SSHHost = SSHHost;
            return this;
        }
        public Builder SSHport(int SSHport) {
            this.SSHport = SSHport;
            return this;
        }
        public Builder LDAPUsername(String LDAPUsername) {
            this.LDAPUsername = LDAPUsername;
            return this;
        }
        public Builder LDAPPassword(String LDAPPassword) {
            this.LDAPPassword = LDAPPassword;
            return this;
        }

    }

    // path to logfile
    public String logfileName;// = "/home/aspitz/LOADdemo.log/"; // should be /data/EVELIN/evelin_backend.log for running on metis

    // mongoDB login settings
    public String MongoAdress;// = "metis.ifi.uni-heidelberg.de"; //"<mongo server adress>";
    public int MongoPort;// = 27020;
    public String password;// = "";
    public String username;// = "";
    public String auth_db;// = "name of authentication DB";
    //public static MongoCredential mongocred = MongoCredential.createCredential(username, auth_db, password.toCharArray());
    public MongoCredential mongocred;// = null;

    // mongoDB database name
    public String MongoDBName;// = "LOAD_YAGO_Wikipedia_en_20161201"; //"LOAD_CivilWar_Wikipedia_en_20160501";

    // print logs only to logfile (false) or also to console (true)?
    public boolean verbose;// = true;

    // maximum query duration before timeout in milliseconds
    public long maxQueryDuration;// = 60000; // 60 seconds

    // maximum number of active queries per user
    public int maxQueryCount;// = 3;

    // cohesion factor for multi-entity queries
    public int cohesion;// = 2;

    // maximum number of allowed query entities per query
    public int maxNumberOfQueryEntities;

    // maximum number of relevant terms to consider for each query entity in sentence queries
    public int sentenceTermLimit;// = 2;

    // maximum number of neighbours to consider per type for each query entity in subgraph queries
    public int graphNeighbourLimit;// = 3;

    // minimum weight for edges to be considered in a subgraph
    public double subgraphEdgeWeightLimit;// = 1.0;

    // maximum sentence length in characters
    public int maxSentenceLength;// = 300;

    // SSH tunnal connection details
    public boolean useSSHtunnel;// = false;

    //details for ssh connection
    //host
    public String SSHHost;
    // port
    public int SSHport;
    //username
    public String LDAPUsername;
    //password
    public String LDAPPassword;


    /* constructor */
    public WebInterfaceSettings(Builder builder) {
        this.logfileName =builder.logfileName;
        this.MongoAdress = builder.MongoAdress;
        this.MongoPort = builder.MongoPort;
        this.password=builder.password;
        this.username = builder.username;
        this.auth_db =builder.auth_db;
        this.MongoDBName = builder.MongoDBName;
        this.verbose =builder.verbose;
        this.maxQueryDuration = builder.maxQueryDuration;
        this.maxQueryCount = builder.maxQueryCount;
        this.cohesion = builder.cohesion;
        this.maxNumberOfQueryEntities = builder.maxNumberOfQueryEntities;
        this.sentenceTermLimit = builder.sentenceTermLimit;
        this.graphNeighbourLimit = builder.graphNeighbourLimit;
        this.subgraphEdgeWeightLimit = builder.subgraphEdgeWeightLimit;
        this.maxSentenceLength = builder.maxSentenceLength;
        this.useSSHtunnel = builder.useSSHtunnel;
        this.SSHHost=builder.SSHHost;
        this.SSHport=builder.SSHport;
        this.LDAPUsername=builder.LDAPUsername;
        this.LDAPPassword=builder.LDAPPassword;
        if(!username.isEmpty()&&!password.isEmpty())
        {
            mongocred=MongoCredential.createCredential(username,auth_db,password.toCharArray());
        }
    }

}