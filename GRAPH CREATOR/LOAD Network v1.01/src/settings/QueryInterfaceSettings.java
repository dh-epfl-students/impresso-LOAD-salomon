package settings;

import java.text.DecimalFormat;

/**
 * Query settings for the LOAD graph query interface
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class QueryInterfaceSettings {
    
    /* Internal settings for the query interface to the LOAD model
     * PROCEED WITH CAUTION! Except for the first value, it is unlikely that 
     * these settings have to be adapted.
     */

    // input folder containing the LOAD graph data (this is the construction output folder)
    public static String LOADGraphInputFolder  = LOADmodelSettings.outfolder;
    
    // date precision flags
    public static int precDAY = 10;
    public static int precMONTH = 7;
    public static int precYEAR = 4;
    
    // parameter settings for program start
    public static int start_numberOfResults = 10;            // initial number of results shown
    public static int start_datePrecision = precYEAR;        // initial date precision
    public static boolean start_useSubQueries = false;    // do not use subqueries by default
    public static int start_density = 1;                    // initial density / cohesion requirement for query terms
    
    // output formatting parameters
    public static DecimalFormat doubleformat = new java.text.DecimalFormat("0.00000");
    
    // string for displaying options at program start
    public static String interfaceOptionsString =" Enter a query by typing: <TYPE_T> <TYPE_1> <SOURCE_1> <TYPE_2> <SOURCE_2>...\n"
           +"    <TYPE_T> is the type of the target search term to rank [LOC, ORG, ACT, DAT, TER]\n"
           +"    <TYPE_x> is the type of the x-th query term [LOC, ORG, ACT, DAT, TER]\n"
           +"    <SOURCE_x> is the x-th query term used for ranking and has type <TYPE_Sx>\n"
           +" Note that the target type <TYPE_T> != <TYPE_Sx> for all x is necessary (for now).\n"
           +" Possible types for queries:\n"
           +"    LOC: Location name (e.g. united states)\n"
           +"    ORG: Organization name (e.g. fbi, rolling stones)\n"
           +"    ACT: Name of a person (e.g. albert einstein)\n"
           +"    DAT: Date in the format <YYYY-MM-DD>, <YYYY-MM> or <YYYY>\n"
           +"    TER: Any term that does not fall under the above\n"
           +"    SEN: Returns the sentence that is best connected to all specified query terms\n"
           +" Example Query: TER LOC alabama DAT 1901\n"
           +" This will return a ranking of TERMS with respect to the place alabama\n"
           +" and the date 1901. Query terms of the same type may occurr. For example:\n"
           +"    TER ACT albert einstein ACT robert oppenheimer\n"
           +"    Returns terms that are relevant to Einstein and Oppenheimer\n\n"
           +" To set the maximum number of query results, enter n=<integer>\n"
           +" To set the date precision of DAT query results, enter p=<precision>\n"
           +"    where <precision> = day | month | year\n"
           +" To toggle the use of subqueries for LOC and ACT enter s=<boolean>\n"
           +"    where <boolean> = true | false. Subqueries split names of places and persons\n"
           +"    into components that are also used as search terms, such as first and last name.\n"
           +" Type <help> or <?> to display this list again\n"
           +" Type <exit> or <quit> or <q> to quit.";

    // console string
    public static String consoleString ="LOAD>";
}
