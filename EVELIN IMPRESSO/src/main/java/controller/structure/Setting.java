package controller.structure;

import java.io.FileReader;
import java.util.Properties;

/**
 * Created by Satya Almasian on 10/01/17.
 */
public class Setting {
    private Properties prop;        // properties for storing the program options

    /**
     * Standard constructor
     * @param filename name of main.settings file

     */
    public Setting(String filename) {

        prop = new Properties();

        /* try to load program main.settings. If nothing can be loaded, create an empty
         * Properties object so that default values are used when reading them.
         */
        try {
            prop.load(new FileReader(filename));
        } catch (Exception e) {
            String err = "Error: Unable to load main.settings from file. Using default values.";
            System.out.println(err);
        }
    }
    /**
     * Get the number of the running port
     * @return the number of the running port
     */
    public int getRunningPort() {

        try{
            return Integer.parseInt(prop.getProperty("RunningPort"));
        }
        catch (Exception e) {
            String err = "The port number is not a valid int ! switching to defult 80.";
            System.out.println(err);
            return 80;
        }
    }

    /**Get the name of the ssh host
     * @return name of the ssh host
     */
    public String getSSHHost() {

        return prop.getProperty("SSHHost");
    }
    /**
     * Get the name of the host
     * @return name of the host
     */
    public String getDBHost() {

        return prop.getProperty("DBHost");
    }
    /**
     * Get the name of LDAP Username
     * @return name of LDAP Username
     */
    public String getLDAPUsername() {

        return prop.getProperty("LDAPUsername");
    }

    /**
     * Get the name of LDAP Password
     * @return name of LDAP Password
     */
    public String getLDAPPassword() {

        return prop.getProperty("LDAPPassword");
    }

    /**
     * Get the MongoDB Port number
     * @return MongoDB Port number
     */
    public int getMongoDBPort() {

        return Integer.parseInt(prop.getProperty("MongoDBPort"));
    }

    /**
     * Get the Mongo Authentication DB
     * @return Mongo Authentication DB
     */
    public String getMongoAuthenticationDB() {

        return prop.getProperty("MongoAuthenticationDB");
    }
    /**
     * Get the MongoDB Username
     * @return MongoDB Username
     */
    public String getMongoDBUsername() {

        return prop.getProperty("MongoDBUsername");
    }
    /**
     * Get the MongoDB Password
     * @return MongoDB Password
     */
    public String getMongoDBPassword() {

        return prop.getProperty("MongoDBPassword");
    }
    /**
     * Get the Database Name for the wikipedia databse
     * @return the database Name
     */
    public String getWikiDBName() {

        return prop.getProperty("WikiDBName");
    }
    /**
     * Get the Database Name for the news articles databse
     * @return the database Name
     */
    public String getArticleDBName() {

        return prop.getProperty("ArticleDBName");
    }
    /**
     * Get verbose status of his program (whether stack traces are written to console)
     * @return verbose status
     */
    public boolean getVerbose() {
            // try to get this option from properties, if it does not exist, use default
        boolean  retval = Boolean.parseBoolean(prop.getProperty("Verbose", "false"));
        return retval;
    }
    /**
     * If it is true then the program will use an ssh connection to connect to mongodb
     * @return sshtunnel status
     */
    public boolean getuseSSHtunnel() {
        // try to get this option from properties, if it does not exist, use default
        boolean  retval = Boolean.parseBoolean(prop.getProperty("useSSHtunnel", "false"));
        return retval;
    }
    /**
     * maximum query duration before timeout in milliseconds
     * @return the duration in milliseconds
     */
    public long getmaxQueryDuration() {

        return Long.parseLong(prop.getProperty("maxQueryDuration"));
    }
    /**
     * maximum number of active queries per user
     * @return the max number of available queries per user
     */
    public int getmaxQueryCount() {

        return Integer.parseInt(prop.getProperty("maxQueryCount"));
    }
    /**
     * cohesion factor for multi-entity queries
     * @return cohesion
     */
    public int getcohesion() {

        return Integer.parseInt(prop.getProperty("cohesion"));
    }
    /**
     * maximum number of relevant terms to consider for each query entity in sentence queries
     * @return the max number of terms
     */
    public int getsentenceTermLimit() {

        return Integer.parseInt(prop.getProperty("sentenceTermLimit"));
    }
    /**
     * maximum number of neighbours to consider per type for each query entity in subgraph queries
     * @return max number of negibours to consider
     */
    public int getgraphNeighbourLimit() {

        return Integer.parseInt(prop.getProperty("graphNeighbourLimit"));
    }
    /**
     * minimum weight for edges to be considered in a subgraph
     * @return max weight
     */
    public double getsubgraphEdgeWeightLimit() {

        return Double.parseDouble(prop.getProperty("subgraphEdgeWeightLimit"));
    }
    /**
     * maximum sentence length in characters
     * @return max sentence lenght
     */
    public int getmaxSentenceLength() {

        return Integer.parseInt(prop.getProperty("maxSentenceLength"));
    }

    /**
     * path to logfile
     * @return path to logfile
     */
    public String getlogfileName() {

        return prop.getProperty("logfileName");
    }
    /**
     * return the maxium  Number Of QueryEntities
     * @return maximum number
     */
    public int getmaxNumberOfQueryEntities() {

        return Integer.parseInt(prop.getProperty("maxNumberOfQueryEntities"));
    }
}
