package settings;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

//porter stemmer library imports
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

/**
 * Internal model settings for both LOAD graph construction and query interface.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class LOADmodelSettings {
    
    /* Internal settings for the LOAD model
     * PROCEED WITH CAUTION! It is unlikely that these settings have to be adapted.
     */
    
    // data variables
    public static final int nANNOTATIONS = 7;        // number of different types of annotations
    public static final char DAT = 0;                // dates
    public static final char LOC = 1;                // locations
    public static final char ACT = 2;                // actors (or persons)
    public static final char ORG = 3;                // organizations
    public static final char TER = 4;                // terms
    public static final char PAG = 5;                // pages (or documents)
    public static final char SEN = 6;                // sentences
    
    // type abbreviations (in an ordering that corresponds to the IDs assigned above)
    public static String[] setNames = {"DAT", "LOC", "ACT", "ORG", "TER", "PAG", "SEN"};
    
    // filenames and settings (inside the working folder) 
    public static String outfolder = SystemSettings.folder +"graph_output/";
    public static String tmpfolder = outfolder +"temp/";
    public static String pageIDList = SystemSettings.folder +"input_PageIDs.txt";
    public static String tmpfile = tmpfolder +"unaggregatedEdgelists.txt";
    public static String sortedtmpfile = tmpfolder +"sorted_unaggregatedEdgelists.txt";
    public static String tmpSortingDirectory = outfolder +"tmpSorting/";
    public static String metaFileName ="metaData.txt";
    public static String[] vertexFileNames = {"vDAT.txt",  "vLOC.txt",  "vACT.txt",  "vORG.txt",  "vTER.txt",  "vPAG.txt",  "vSEN.txt"};
    public static String[] edgeFileNames = {"eDAT.txt",  "eLOC.txt",  "eACT.txt",  "eORG.txt",  "eTER.txt",  "ePAG.txt",  "eSEN.txt"};
    
    // encoding settings for the output data
    public static String commentChar ="#";            // char to signify comments
    public static String sepChar ="\t";            // column separator char
    public static String replaceableString ="@";    // deleteable character used for overwriting in bitmaps in term-detection
    public static char replaceableChar = replaceableString.charAt(0);
    public static String metaHeaderVersion ="############################################################################\n"
                                           +"# Multipartite Graph from the collection" + SystemSettings.MongoDBname +"\n"
                                           +"# Extraction started"
                                           + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime()) +"\n"
                                           +"############################################################################\n";
    public static String metaTimeStamp ="# Extraction process started";
    public static String metaHeader ="############################################################################\n"
                                    +"# Contents of this file: number of vertices for the different vertex sets\n"
                                    +"# vertex files: labels of each vertex, followed by degrees with:\n"
                                    +"#   dates, locations, actors, organizations, terms, pages, sentences\n"
                                    +"# edge files: blocks of 7 lines each (one line per vertex set). Per line:\n"
                                    +"#   <neighbour type> <target node 1> <edge weight 1> <target node 2> ...\n";
    
    // minimum length of words that are added to the output
    public static int minWordLength = 3;
    
    // regular expressions used for cleaning Terms
    public static String replaceExpressionTerms ="[^\\p{L}\\p{Nd}-'§$€%& ]+";
    
    // regular expressions used for cleaning names (actors, locations, organizations)
    public static String replaceExpressionNames ="[^\\p{L}\\p{Nd}-'& ]+";
    
    // regular expression for removing whitespaces
    public static String replaceExpressionWhitespace ="[\t\n\r_]+";
    
    // patterns for recognizing dates. By default, this is YYYY-MM-DD for Heideltime
    // for different temporal taggers, this may have to be adjusted.
    public static String datepattern ="(\\d{4})(-\\d{2})?(-\\d{2})?.*";
    
    // maximum sentence distance between annotations to still be used for edge creation
    public static int maxDistanceInSentences = 5;
    
    // default weights for unweighted edges (pages, sentences)
    public static int default_weight = 1;
    
    // decimal format without trailing zeroes
    public static DecimalFormat df = new DecimalFormat("#.###");
    
    // random seed used for shuffling the document IDs.
    // Shuffling document IDs makes sense for heterogeneous document collections where
    // documents with smaller (or larger9 ID tend to have large file sizes s compared
    // to other documents in the collection.
    public static int shuffleRandomSeed = 42;
    
    // Euler's number and exponentiation function
    private static double base = Math.E;
    
    // Similarity function for edges weights:
    // transform a distance in sentences to a similarity of the involved entities
    public static float weightFunctionExponential(float f) {
        return (float) Math.pow(base, -f);
    }
    
    // replacement of special characters and trimming for Terms (non-entities)
    public static String replaceAndTrimTerms(String in) {
        String out = in.replaceAll(replaceExpressionWhitespace,"").trim();
        out = out.replaceAll(replaceExpressionTerms,"");
        return out;
    }
    
    // replacement of special characters and trimming for Names (entities)
    public static String replaceAndTrimNames(String in) {
        String out = in.replaceAll(replaceExpressionWhitespace,"").trim();
        out = out.replaceAll(replaceExpressionNames,"");
        return out;
    }
    
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
}
