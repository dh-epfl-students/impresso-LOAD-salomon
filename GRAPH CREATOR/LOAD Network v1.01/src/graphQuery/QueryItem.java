package graphQuery;

/**
 * Item used to represent a query to the LOAD graph
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class QueryItem {
    public int targetSet;
    public int[] sourceSets;
    public String[][] sourceTerms;
    
    public QueryItem(int tSet, int[] sSets, String[][] sTerms) {
        targetSet = tSet;
        sourceSets = sSets;
        sourceTerms = sTerms;
    }
}
