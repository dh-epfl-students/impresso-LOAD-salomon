package graphQuery;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.*;
import static settings.QueryInterfaceSettings.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

// snowball stemmer imports (porter stemmer)
import org.tartarus.snowball.SnowballStemmer;

// mongoDB imports
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
//import org.bson.types.ObjectId;   // required for mongoDb object IDs for sentences

import graphQuery.LOADGraph;

/**
 * Console-based LOAD graph query interface
 * 
 * Run main method to start the interface. Change settings in package settings.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class GraphQueryInterface {
    public static boolean DEBUG_INTERFACE = true;
    // stemmer for terms. Note that this is NOT thread safe!
    public static SnowballStemmer stemmer = getStemmer(stemmerLanguage);
    
    // graph representation of the LOAD network
    public LOADGraph g;
    
    // constructor that reads the graph data from file
    public GraphQueryInterface(String graphFolder) {
        g = new LOADGraph(LOADGraphInputFolder);
    }
    
    public ArrayList<RankItem> querySet(String queryName, int querySet, int targetSet, int n, int datePrecision) {
        ArrayList<RankItem> temp = new ArrayList<RankItem>();
        Integer queryId = g.namesToIdMaps.get(querySet).get(queryName);
        if (queryId != null) {
            GraphNode source = g.nodeLists[querySet][queryId];
            for (int i=0; i<source.degrees[targetSet]; i++) {
                int targetId = source.adjacency[targetSet][i];
                GraphNode target = g.nodeLists[targetSet][targetId]; 
                
                double weight = source.weights[targetSet][i];
                int degree = target.degrees[querySet];
                int setSize = g.nNodes[querySet];
                
                // compute graph tf-idf value
                double idf = setSize / (double) degree;
                double tfidf = weight * Math.log(idf);
                
                temp.add(new RankItem(source, target, tfidf));    
            }
        }
        
        // rank neighbours by graph tf-idf score
        Collections.sort(temp);
        
        // for dates, make sure to give only output dates with at least the required precision
        ArrayList<RankItem> result;
        if (targetSet == DAT) {
            result = new ArrayList<RankItem>();
            for (RankItem r : temp) {
                if (result.size() == n) {
                    break;
                } else if (r.target.value.length() >= datePrecision) {
                    result.add(r);
                }
            }
        // everything else: just add the top n results to the output
        } else {
            if (n <= 0) {
                result = temp;
            } else {
                result = new ArrayList<RankItem>();
                for (RankItem r : temp) {
                    if (result.size() == n) {
                        break;
                    } else {
                        result.add(r);
                    }
                }
            }
        }
        
        if (!result.isEmpty()) {
            double maxRank = result.get(0).weight;
            if (maxRank == 0) maxRank = 1;
            for (RankItem r : result) {
                r.weight = r.weight / maxRank;
            }
        }
        
        return result;
    }
    
    // process a query for a sentence by finding the sentence that is best linked
    // to the provided query values
    public ArrayList<RankItem> sentenceQuery(QueryItem q, int n) {
        HashMap<RankItem,RankItem> results = new HashMap<RankItem,RankItem>();
        int maxCount = 0;
        
        for (int i=0; i<q.sourceSets.length; i++) {
            int targetSet = q.targetSet;
            int sourceSet = q.sourceSets[i];
            String[] sourceTerms = q.sourceTerms[i];
            
            for (String s : sourceTerms) {
                maxCount++;
                Integer sourceId = g.namesToIdMaps.get(sourceSet).get(s);
                if (sourceId != null) {
                    GraphNode source = g.nodeLists[sourceSet][sourceId];
                    for (int j=0; j<source.degrees[targetSet]; j++) {
                        int targetId = source.adjacency[targetSet][j];
                        GraphNode target = g.nodeLists[targetSet][targetId]; 
                        
                        double weight = 1.0;
                        RankItem r = new RankItem(source, target, weight);
                        if (results.containsKey(r)) {
                            results.get(r).weight += weight;
                        } else {
                            results.put(r, r);
                        }
                    }
                }
            }
        }
        if (maxCount == 0) maxCount = 1;
            
        ArrayList<RankItem> retlist = new ArrayList<RankItem>(results.values());
        Collections.sort(retlist);
        
        ArrayList<RankItem> result;
        if (n <= 0) {
            for (RankItem r : retlist) {
                r.weight = r.weight / maxCount;
            }
            result = retlist;
        } else {
            result = new ArrayList<RankItem>();
            for (RankItem r : retlist) {
                if (result.size() == n) {
                    break;
                } else {
                    r.weight = r.weight / maxCount;
                    result.add(r);
                }
            }
        }
        
        return result;
    }
    
    // process a query for a page by finding the page that is best linked
    // to the provided query values
    public ArrayList<RankItem> pageQuery(QueryItem q, int n) {
        
        HashMap<RankItem,RankItem> results = new HashMap<RankItem,RankItem>();
        
        q.targetSet = SEN;
        ArrayList<RankItem> sentences = sentenceQuery(q, -1);
        
        for (RankItem r : sentences) {
            GraphNode page = g.nodeLists[PAG][r.target.adjacency[PAG][0]];
            r.target = page;
            
            if (results.containsKey(r)) {
                results.get(r).weight += r.weight;
            } else {
                results.put(r, r);
            }
        }
        
        ArrayList<RankItem> retlist = new ArrayList<RankItem>(results.values());
        Collections.sort(retlist);
        
        ArrayList<RankItem> result;
        if (n <= 0) {
            result = retlist;
        } else {
            result = new ArrayList<RankItem>();
            for (RankItem r : retlist) {
                if (result.size() == n) {
                    break;
                } else {
                    result.add(r);
                }
            }
        }
        
        if (!result.isEmpty()) {
            double maxRank = result.get(0).weight;
            if (maxRank == 0) maxRank = 1;
            for (RankItem r : result) {
                r.weight = r.weight / maxRank;
            }
        }
        
        return result;
    }
    

    // process a query with multiple input values by splitting it into the individual queries
    // and then combining the results
    public ArrayList<RankItem> multiSetQuery(QueryItem q, int n, int datePrecision, int density) {
        
        HashMap<GraphNode,Integer> denseNeighbours = new HashMap<GraphNode,Integer>();
        for (int i=0; i<q.sourceSets.length; i++) {
            int targetSet = q.targetSet;
            int sourceSet = q.sourceSets[i];
            String[] sourceTerms = q.sourceTerms[i];
            for (String s : sourceTerms) {
                Integer sourceId = g.namesToIdMaps.get(sourceSet).get(s);
                if (sourceId != null) {
                    GraphNode source = g.nodeLists[sourceSet][sourceId];
                    for (int j=0; j<source.degrees[targetSet]; j++) {
                        int targetId = source.adjacency[targetSet][j];
                        GraphNode target = g.nodeLists[targetSet][targetId];
                        if (denseNeighbours.containsKey(target)) {
                            denseNeighbours.put(target, denseNeighbours.get(target)+1);
                        } else {
                            denseNeighbours.put(target, 1);
                        }
                    }
                }
            }
        }
        
        // remove results that do not appear for enough of the query terms
        Iterator<Entry<GraphNode,Integer>> it = denseNeighbours.entrySet().iterator();
        while (it.hasNext()) {
            Entry<GraphNode,Integer> e = it.next();
            if (e.getValue() <= density) {
                it.remove();
            }
        }
        
        HashMap<RankItem,RankItem> results = new HashMap<RankItem,RankItem>();
        int validSourceTypes = 0;
        
        for (int i=0; i<q.sourceSets.length; i++) {
            int targetSet = q.targetSet;
            int sourceSet = q.sourceSets[i];
            String[] sourceTerms = q.sourceTerms[i];
            
            if (sourceTerms.length == 1) {
                ArrayList<RankItem> list = querySet(sourceTerms[0], sourceSet, targetSet, -1, datePrecision);
                
                boolean validType = false;
                for (RankItem r : list) {
                    if (denseNeighbours.containsKey(r.target)) {
                        validType = true;
                        if (results.containsKey(r)) {
                            RankItem inList = results.get(r);
                            inList.weight += r.weight;
                        } else {
                            results.put(r,r);
                        }
                    }
                }
                if (validType) {
                    validSourceTypes++;
                }
            } else {
                HashMap<RankItem,RankItem> tmpResults = new HashMap<RankItem,RankItem>();
                int validSourceTerms = 0;
                for (String s : sourceTerms) {
                    ArrayList<RankItem> list = querySet(s, sourceSet, targetSet, -1, datePrecision);
                    if (list.size() > 0) {
                        validSourceTerms++;
                    }
                    for (RankItem r : list) {
                        if (tmpResults.containsKey(r)) {
                            RankItem inList = tmpResults.get(r);
                            inList.weight += r.weight;
                        } else {
                            tmpResults.put(r,r);
                        }
                    }
                }
                if (validSourceTerms == 0) validSourceTerms = 1;
                
                boolean validType = false;
                for (RankItem r : tmpResults.values()) {
                    if (denseNeighbours.containsKey(r.target)) {
                        validType = true;
                        r.weight = r.weight / validSourceTerms;
                        if (results.containsKey(r)) {
                            RankItem inList = results.get(r);
                            inList.weight += r.weight;
                        } else {
                            results.put(r,r);
                        }
                    }
                }
                if (validType) {
                    validSourceTypes++;
                }
            }
        }
        if (validSourceTypes == 0) validSourceTypes = 1;
        
        ArrayList<RankItem> retlist = new ArrayList<RankItem>();
        for (RankItem r : results.values()) {
            r.weight = r.weight / validSourceTypes;
            retlist.add(r);
        }
        
        Collections.sort(retlist);
        
        ArrayList<RankItem> result;
        if (n <= 0) {
            result = retlist;
        } else {
            result = new ArrayList<RankItem>();
            for (RankItem r : retlist) {
                if (result.size() == n) {
                    break;
                } else {
                    result.add(r);
                }
            }
        }
        
        return result;
    }
    
    // endless lopp checking for console user input
    public void loopConsoleInput() {
        
        // options
        int numberOfResults = start_numberOfResults;
        int datePrecision = start_datePrecision;
        boolean useSubQueries = start_useSubQueries;
        int density = start_density;
        
        // type lookup
        HashMap<String,Integer> typeLookup = new HashMap<String,Integer>();
        for (int i=0; i<nANNOTATIONS; i++) {
            typeLookup.put(setNames[i], i);
        }
        
        Scanner scanner = new Scanner(System.in);
        System.out.println(interfaceOptionsString);
        System.out.print(consoleString);
        
        while (true) {
            try {
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine();
                    
                    // check if quit command is given
                    if (input.equals("exit") || input.equals("quit") || input.equals("q")) {
                        break;
                        
                    // check is help is requested
                    } else if (input.equals("help") || input.equals("h") || input.equals("?")) {
                        System.out.println(interfaceOptionsString);
                        
                    // check if the number of results per query is changed
                    } else if (input.startsWith("n=")) {
                        input = input.substring(2);
                        try {
                            numberOfResults = Integer.parseInt(input);
                        } catch (Exception e) {
                            System.out.println("unable to parse: "  + input +"  as an integer.");
                        }
                        
                    // check if the date precision is changed
                    } else if (input.startsWith("p=")) {
                        input = input.substring(2);
                        try {
                            if (input.equals("day")) {
                                datePrecision = precDAY;
                            } else if (input.equals("month")) {
                                datePrecision = precMONTH;
                            } else if (input.equals("year")) {
                                datePrecision = precYEAR;
                            } else {
                                System.out.println("Unknown date precision specified:"  + input);
                            }
                        } catch (Exception e) {
                            System.out.println("unable to parse: "  + input +"  as an integer.");
                        }
                        
                    // check if subqueries are toggled on or off
                    } else if (input.startsWith("s=")) {
                        input = input.substring(2);
                        try {
                            if (input.equals("true") || input.equals("1")) {
                                useSubQueries = true;
                            } else if (input.equals("false") || input.equals("0")) {
                                useSubQueries = false;
                            } else {
                                System.out.println("Unknown value specified for sub query usage:"  + input);
                            }
                        } catch (Exception e) {
                            System.out.println("unable to parse:"  + input +"  as an integer.");
                        }
                        
                    // otherwise: parse the input as a query
                    } else {
                        try {
                            String[] splitline = input.split(" " );
                            
                            int targetType = typeLookup.get(splitline[0]);
                            ArrayList<Integer> queryTypes = new ArrayList<Integer>();
                            ArrayList<ArrayList<String>> queryValues = new ArrayList<ArrayList<String>>(); 
                            
                            // parse input and add the types and values of query entities to a list
                            int index = 1;
                            while (index < splitline.length) {
                                queryTypes.add(typeLookup.get(splitline[index]));
                                index++;
                                queryValues.add(new ArrayList<String>());
                                while (index < splitline.length && !typeLookup.containsKey(splitline[index])) {
                                    queryValues.get(queryTypes.size()-1).add(splitline[index]);
                                    index++;
                                }
                            }
                            
                            // transform query entities and types into a query
                            int[] queryTypesArray = new int[queryTypes.size()];
                            String[][] queryValueArray = new String[queryTypes.size()][];
                            for (int i=0; i<queryTypes.size(); i++) {
                                queryTypesArray[i] = queryTypes.get(i);
                                
                                // for queryValues with type TERM, apply stemming
                                if (queryTypesArray[i] == TER) {
                                    ArrayList<String> tmp = new ArrayList<String>();
                                    for (String s : queryValues.get(i)) {
                                        s = s.trim();//toLowerCase().trim();
                                        stemmer.setCurrent(s);
                                        stemmer.stem();
                                        s = stemmer.getCurrent().trim();
                                        if (s.length() > 0) {
                                            tmp.add(s);
                                        }
                                    }
                                    queryValueArray[i] = new String[tmp.size()];
                                    for (int j=0; j<tmp.size(); j++) {
                                        queryValueArray[i][j] = tmp.get(j);
                                    }
                                    
                                // for locations and persons, apply the inclusion condition to find more
                                // possible candidates if that option is is activated
                                } else if (useSubQueries && (queryTypesArray[i] == LOC || queryTypesArray[i] == ACT)) {
                                    String st =" ";
                                    for (String s : queryValues.get(i)) {
                                        st +=" "  + s.trim();//toLowerCase().trim();
                                    }
                                    st = st.trim();
                                    
                                    ArrayList<String> tmp = new ArrayList<String>();
                                    tmp.add(st);
                                    for (String s : queryValues.get(i)) {
                                        s = s.trim();//toLowerCase().trim();
                                        if (s.length() > 0) {
                                            tmp.add(s);
                                        }
                                    }
                                    queryValueArray[i] = new String[tmp.size()];
                                    for (int j=0; j<tmp.size(); j++) {
                                        queryValueArray[i][j] = tmp.get(j);
                                    }                                    
                                
                                // all other types can just be added
                                } else {
                                    String st =" ";
                                    for (String s : queryValues.get(i)) {
                                        st +=" "  + s.trim();//toLowerCase().trim();
                                    }
                                    st = st.trim();
                                    queryValueArray[i] = new String[1];
                                    queryValueArray[i][0] = st;
                                }
                            }
                            
                            // create a query item for processing
                            QueryItem q = new QueryItem(targetType, queryTypesArray, queryValueArray);
                            
                            // check to make sure that the query types and target types do not overlap
                            boolean skip = false;
                            for (int t : queryTypesArray) {
                                if (t == targetType) {
                                    skip = true;
                                }
                            }
                            
                            if (skip) {
                                System.out.println("The target entity type must not appear in the query.");
                                System.out.println("Bad command:"  + input);
                                
                            // if the target of the query is a page, apply page method
                            } else if (q.targetSet == PAG) {
                                ArrayList<RankItem> list = pageQuery(q, numberOfResults);
                                
                                System.out.println("Top"  + list.size() +" "  + setNames[PAG] +"  for:"  + input.substring(4) +"\n");
                                if (list.size() > 0) {
                                    String pageURL =" https://en.wikipedia.org/?curid=";
                                    // find length of longest result string
                                    int maxlength = 0;
                                    for (RankItem s : list) {
                                        if (s.target.value.length() > maxlength) maxlength = s.target.value.length();
                                    }
                                    maxlength += pageURL.length();
                                    
                                    System.out.println("rank       "  + String.format("%1$-" + (maxlength-5) +" s", setNames[PAG]) +" confidence\n");
                                    int counter = 1;
                                    for (RankItem s : list) {
                                        System.out.print(String.format("%1$-" + 5 +" s", Integer.toString(counter++)));
                                        System.out.print(String.format("%1$-" + (maxlength+2) +" s", pageURL + s.target.value));
                                        System.out.println(String.format("%1$" + 9 +" s", doubleformat.format(s.weight)));
                                    }
                                    //System.out.println();
                                }
                            
                            // if the target of the query is a sentence, apply sentence method
                            } else if (q.targetSet == SEN) {
                            // otherwise, apply default query method
                            } else {
                                ArrayList<RankItem> list;
                                
                                // if there is just one query value, simply find the best match
                                if (q.sourceSets.length == 1 && q.sourceTerms[0].length == 1) {
                                    list = querySet(q.sourceTerms[0][0], q.sourceSets[0], q.targetSet, numberOfResults, datePrecision);
                                
                                // if there are multiple query values, combine the results
                                } else {
                                    list = multiSetQuery(q, numberOfResults, datePrecision, density);
                                }
                                System.out.println("Top "  + list.size() +" "  + setNames[q.targetSet] +"  for: "  + input.substring(4) +"\n");
                                if (list.size() > 0) {
                                    // find length of longest result string
                                    int maxlength = 0;
                                    for (RankItem s : list) {
                                        if (s.target.value.length() > maxlength) maxlength = s.target.value.length();
                                    }            
                                    
                                    System.out.println("rank  "  + String.format("%1$s " + maxlength +"s ", setNames[q.targetSet]) +"    weight\n");
                                    int counter = 1;
                                    for (RankItem s : list) {
                                        System.out.print(String.format("%1$s " + 5 +"s ", Integer.toString(counter++)));
                                        System.out.print(String.format("%1$s " + (maxlength+2) +"s ", s.target.value));
                                        System.out.println(String.format("%1$s " + 9 +"s ", doubleformat.format(s.weight)));
                                    }
                                    //System.out.println();
                                }
                            }
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Unable to parse command:"  + input);
                        }
                        
                    }
                    System.out.print(consoleString);
                }
            } catch (Exception e){ }
            
            try {
                Thread.sleep(500);
            } catch (Exception e){ }
        }
    }

    public static void main(String[] args) {
        
        GraphQueryInterface qg = new GraphQueryInterface(LOADGraphInputFolder);
        
        // start loop to read from console and process queries
        qg.loopConsoleInput();
                
    }

}
