package impresso;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.cache.Cache;

import construction.Annotation;

import static com.mongodb.client.model.Filters.and;
import static settings.SystemSettings.act;
import static settings.SystemSettings.loc;


/**
 * Creates a LOAD subgraph from a single document
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class MultiThreadWorkerImpresso implements Runnable {
    
    private MultiThreadHubImpresso hub;
    private Cache<String, JSONObject> newspaperCache;
    private Cache<String, JSONObject> entityCache;
    private HashMap<Integer, String> contentIdtoPageId;
    private Properties prop;
    private SolrReader solrReader;
    
    // internal variables
    HashSet<String> invalidTypes;
    private int[] count_ValidAnnotationsByType;
    private long count_unaggregatedEdges;
    private int failedCount;
    private int negativeOffsetCount;
    private static Pattern pattern = Pattern.compile(datepattern);
    private Integer threadNum;

    public MultiThreadWorkerImpresso(MultiThreadHubImpresso hub, Properties prop, SolrReader solrReader,
                                     Cache<String, JSONObject> newspaperCache, Cache<String, JSONObject> entityCache,
                                     HashMap<Integer, String> contentIdtoPageId, int threadNum) {
        this.hub = hub;
        this.prop = prop;
        this.newspaperCache = newspaperCache;
        this.entityCache = entityCache;
        this.contentIdtoPageId = contentIdtoPageId;
        this.solrReader = solrReader;
        // internal variables
        invalidTypes = new HashSet<String>();
        count_ValidAnnotationsByType = new int[nANNOTATIONS];
        count_unaggregatedEdges = 0;
        failedCount = 0;
        negativeOffsetCount = 0;
        this.threadNum = threadNum;
    }

    @Override
    public void run() {
        if(VERBOSE)
            System.out.println(String.format("Running thread number %d",  threadNum));
        ArrayList<Annotation> annotationsPage = new ArrayList<Annotation>();     	//Change the annotationsPage to contentItems
        ArrayList<Annotation> annotationsSentence = new ArrayList<Annotation>();	//Will have to be with an artificial sentences
        ArrayList<Annotation> annotationsTerms = new ArrayList<Annotation>();		//These will come from the tokens
        
        HashSet<String> invalidTypes = new HashSet<String>();
        ArrayList<String> edges = new ArrayList<String>();
        
        int invalidAnnotationCount = 0;
        int annotationCounter = 0;
        Integer newspaper_year_id;

        while ( (newspaper_year_id = hub.getContentItemID()) != null) {
        	//Get the pageId from the hashmap
            if(DEBUG_PROMPT)
                System.out.println(String.format("Thread %d: got newspaper year id %d",  threadNum, newspaper_year_id));
        	String articleID = contentIdtoPageId.get(newspaper_year_id);

        	if(DEBUG_PROMPT)
        	    System.out.println("Page id :"  + articleID);

        	//using content id create impressocontentitem and use this to read from solr, then inject tokens
        	//Each worker will read from a single newspaper/year coordinated by the Hub
        	        	
            annotationsPage.clear();
            edges.clear();

            try {
                //For each id, create a content item
                ImpressoContentItem contentItem = solrReader.getContentItem(articleID);
                if(DEBUG_PROMPT)
                    System.out.println("Solr item created from pageID");
                //Inject the tokens into the content item and create the sentences
                contentItem = injectLingusticAnnotations(contentItem);
                if(DEBUG_PROMPT)
                    System.out.println("Linguistic annotations added to Impresso item");

                //Sort all of the tokens so that they are in order of offset
                contentItem.sortTokens();

                if(DEBUG_PROMPT) {
                    System.out.println("Tokens for item:");
                    for(Token tok: contentItem.getTokens())
                        System.out.println(tok);
                }

                List<Token> tokens = contentItem.getTokens();

                int sentence_id = 0; //Increments as the sentence increases

                for(int i = 0; i <  tokens.size(); i += SENTENCE_SIZE) {
                    annotationsSentence.clear();
                    annotationsTerms.clear();
                    //Create an artificial sentence composed of at most 7 tokens
                    List<Token> sentence = tokens.subList(i, Math.min(tokens.size(), i + SENTENCE_SIZE));
                    int min_offset = sentence.get(0).getOffset();
                    Token last_word = sentence.get(sentence.size() - 1);
                    int max_offset = last_word.getOffset() + last_word.getSurface().length();

                    boolean hasAnnotations = false;

                    for(Token token : sentence) { // if there are annotations in the sentence
                        if(token instanceof Entity){
                            // DATES:
                            // iterate over temporal annotations and extract them to create nodes
                            String annotationType_str = ((Entity) token).getType();

                            char annotationType;
                            if (annotationType_str.equalsIgnoreCase(loc)) {
                                if(DEBUG_PROMPT)
                                    System.out.println(String.format("Token %s is a LOC entity", token));
                                annotationType = LOC;
                            } else if (annotationType_str.equalsIgnoreCase(act)) {
                                if(DEBUG_PROMPT)
                                    System.out.println(String.format("Token %s is an ACT entity", token));
                                annotationType = ACT;
                            } else {
                                if(DEBUG_PROMPT)
                                    System.out.println(String.format("Token %s is an invalid entity", token));
                                invalidTypes.add(annotationType_str);
                                invalidAnnotationCount++;
                                continue;
                            }

                            /* Dates are not part of our data set
                            if (annotationType == DAT) {
                                String timexValue = obj.getString(mongoIdentAnnotation_normalized);
                                Matcher m = pattern.matcher(timexValue);
                                if (m.matches()) {
                                    count_ValidAnnotationsByType[DAT]++;

                                    // mark portion of the sentence that is covered by the date for deletion
                                    int begin = (Integer) obj.get(mongoIdentAnnotation_start);
                                    int end = (Integer) obj.get(mongoIdentAnnotation_end);
                                    for (int p=begin; p<end; p++) {
                                        mask[p] = replaceableChar;
                                    }

                                    // extract dates and make sure the completeness condition is satisfied
                                    // i.e. for dates YYYY-MM-DD also include YYYY-MM and YYYY, ...
                                    String date =" ";
                                    for (int i=1; i<=m.groupCount(); i++) {
                                        if (m.group(i) != null) {
                                            date += m.group(i);
                                            int annId = hub.getAnnotationID(annotationType, date);

                                            // add annotation to list for later edge creation
                                            Annotation ann = new Annotation(date, annId, annotationType, sentence_id);
                                            annotationsSentence.add(ann);
                                        }
                                    }

                                    hasAnnotations = true;
                                    annotationCounter++;
                                } */

                            if (annotationType == LOC || annotationType == ACT) {
                                Entity ent = (Entity) token;
                                String value = NODES_AS_IDS ? ent.getEntityId() : ent.getLemma();
                                // get an id
                                int annId = hub.getAnnotationID(annotationType, value);

                                // add annotation to list for later edge creation
                                Annotation ann = new Annotation(value, annId, annotationType, sentence_id);
                                if(DEBUG_PROMPT)
                                    System.out.println("Annotation created from valid entity.");
                                annotationsSentence.add(ann);

                                hasAnnotations = true;
                                annotationCounter++;
                                count_ValidAnnotationsByType[annotationType]++;

                            }
                    }
                 }
                // add this sentence and the corresponding page to the graph if it had valid annotations
                if (hasAnnotations) {

                    // add sentence to the map
                    String sentenceId = articleID + idInfoSepChar + min_offset + idInfoSepChar + max_offset;
                    int annotationID = hub.getAnnotationID(SEN, sentenceId);
                    sentenceId += idInfoSepChar + annotationID;
                    count_ValidAnnotationsByType[SEN]++;

                    // add page / document to the map
                    int pageId = hub.getAnnotationID(PAG, articleID);
                    count_ValidAnnotationsByType[PAG]++;

                    //If there are annotations in the sentence, turn all other tokens into term annotations
                    for(Token term: sentence) {
                        if(!(term instanceof Entity)) {
                            int annId = hub.getAnnotationID(TER, term.getLemma());
                            Annotation ann = new Annotation(term.getLemma(), annId, TER, sentence_id);
                            annotationsSentence.add(ann);
                        }

                    }

                    // turn list of annotations into edges by pairwise comparison
                    // add edge between sentence and page
                    edges.add(PAG + sepChar + SEN + sepChar + pageId + sepChar + sentenceId + sepChar + 0 +"\n");
                    if(DEBUG_PROMPT)
                        System.out.println("Edges created from this token : " + PAG + sepChar + SEN + sepChar + pageId + sepChar + sentenceId + sepChar + 0);
                    count_unaggregatedEdges++;

                    for (int j=0; j<annotationsSentence.size(); j++) {
                        Annotation an = annotationsSentence.get(j);

                        // NOTE connecting entities to the sentence is enough (sentences are connected to pages)
                        // add edge between annotation and page
                        // ew.append(an.type + sepChar + PAG + sepChar + an.id + sepChar + pageId + sepChar + 0 +"\n");
                        // count_unaggregatedEdges++;

                        // add edge between annotation and sentence
                        edges.add(an.type + sepChar + SEN + sepChar + an.id + sepChar + sentenceId + sepChar + 0 +"\n");
                        if(DEBUG_PROMPT)
                            System.out.println("Edges created between annotation and sentence : " + an.type + sepChar + SEN + sepChar + an.id + sepChar + sentenceId + sepChar + 0);

                        count_unaggregatedEdges++;
                        annotationsPage.add(an);
                    }

                    for (int j=0; i<annotationsTerms.size(); j++) {
                        Annotation t = annotationsTerms.get(j);

                        // NOTE connecting terms to the sentence is enough (sentences are connected to pages)
                        // add edge between term and page
                        // ew.append(t.type + sepChar + PAG + sepChar + t.id + sepChar + pageId + sepChar + 0 +"\n");
                        // count_unaggregatedEdges++;

                        // add edge between term and sentence
                        edges.add(t.type + sepChar + SEN + sepChar + t.id + sepChar + sentenceId + sepChar + 0 +"\n");
                        if(DEBUG_PROMPT)
                            System.out.println("Edges created between term and sentence : " + t.type + sepChar + SEN + sepChar + t.id + sepChar + sentenceId + sepChar + 0);

                        count_unaggregatedEdges++;

                        // add pairwise edges between terms and annotations in the same sentence (but only in one direction)
                        for (int h=0; h<annotationsSentence.size(); h++) {
                            Annotation an = annotationsSentence.get(h);
                            edges.add(an.type + sepChar + t.type + sepChar + an.id + sepChar + t.id + sepChar + 0 +"\n");
                            if(DEBUG_PROMPT)
                                System.out.println("Edges created between term and annotations in the same sentence : " + an.type + sepChar + t.type + sepChar + an.id + sepChar + t.id + sepChar + 0);

                            count_unaggregatedEdges++;
                        }

                    }
                }
                sentence_id ++;
            }
                //System.out.println();
            } catch (Exception e) {
                e.printStackTrace();
                failedCount++;
            }
            
            // sort all annotations on a page by sentence ID for easier pairwise comparison
            // in the following, it is assumed that annotations with smaller sentence ID come first
            Collections.sort(annotationsPage);
                
            // turn list of annotations on the entire page into edges by pairwise comparison
            for (int i=0; i<annotationsPage.size(); i++) {
                Annotation an1 = annotationsPage.get(i);
                    
                // add pairwise edges between all annotations (but only in one direction)
                // ORDER: lower entity type first (if this is equal, lower ID first)
                for (int j=i+1; j<annotationsPage.size(); j++) {
                    Annotation an2 = annotationsPage.get(j);
                    
                    // compute the distance in sentences between the two annotations. Since annotations
                    // are ordered non-decreasingly by sentenceID, if this distance is larger than the
                    // maximum distance, we can skip the rest of the list.
                    int weight = an2.sentenceID - an1.sentenceID;
                    if (weight > maxDistanceInSentences) {
                        System.gc();
                        break;
                    }
                        
                    if (an1.type != an2.type) { // connections between entity types
                        if (an1.type < an2.type) {
                            edges.add(an1.type + sepChar + an2.type + sepChar + an1.id + sepChar + an2.id + sepChar + weight +"\n");
                            count_unaggregatedEdges++;
                            if(DEBUG_PROMPT)
                                System.out.println(String.format("Edge created from %s to %s (2 different entity types):", an1.value, an2.value) + an1.type + sepChar + an2.type + sepChar + an1.id + sepChar + an2.id + sepChar + weight);
                        } else {
                            edges.add(an2.type + sepChar + an1.type + sepChar + an2.id + sepChar + an1.id + sepChar + weight +"\n");
                            count_unaggregatedEdges++;
                            if(DEBUG_PROMPT)
                                System.out.println(String.format("Edge created from %s to %s (2 different entity types):", an2.value, an1.value) + an2.type + sepChar + an1.type + sepChar + an2.id + sepChar + an1.id + sepChar + weight);
                        }
                    } else if (an1.type == LOC || an1.type == ACT || an1.type == ORG) { // connections within entity types
                        if (an1.id < an2.id) {
                            edges.add(an1.type + sepChar + an2.type + sepChar + an1.id + sepChar + an2.id + sepChar + weight +"\n");
                            count_unaggregatedEdges++;
                            if(DEBUG_PROMPT)
                                System.out.println(String.format("Edge created from %s to %s (Same type):", an1.value, an2.value) + an1.type + sepChar + an2.type + sepChar + an1.id + sepChar + an2.id + sepChar + weight);
                        } else if (an1.id > an2.id) {
                            edges.add(an2.type + sepChar + an1.type + sepChar + an2.id + sepChar + an1.id + sepChar + weight +"\n");
                            count_unaggregatedEdges++;
                            if(DEBUG_PROMPT)
                                System.out.println(String.format("Edge created from %s to %s (Same type):", an2.value, an1.value) + an2.type + sepChar + an1.type + sepChar + an2.id + sepChar + an1.id + sepChar + weight);
                        }
                        // the case where an1.id == an2.id is ignored since we do not want self loops in the network
                    }
                }
            }
            try {
                hub.writeEdges(edges);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.gc();
        }
        
        // update the total statistics for summing up over all threads
        hub.updateStatistics(annotationCounter, count_unaggregatedEdges, failedCount, invalidAnnotationCount, invalidTypes,
                             count_ValidAnnotationsByType, negativeOffsetCount);
        
        hub.latch.countDown();
    }
    
	private ImpressoContentItem injectLingusticAnnotations(ImpressoContentItem contentItem) {
        boolean inNews = false;
        boolean inEnt = false;
		String tempId = contentItem.getId();
		//System.out.println(tempId);

		JSONObject newspaperJson = newspaperCache.getIfPresent(tempId);
		if(newspaperJson != null){
            JSONArray sents = newspaperJson.getJSONArray("sents");

            int length = sents.length();
            int totalOffset = 0; //Keeps track of the total offset
            for(int j=0; j<length; j++) {
                JSONObject sentence = sents.getJSONObject(j);
                //This is where the injectTokens of a ImpressoContentItem
                totalOffset += contentItem.injectTokens(sentence.getJSONArray("tok"), sentence.getString("lg"), true, totalOffset);
            }
        } else {
		    inNews = false;
        }

		/*
		 * WHILE THE ENTITIES ARE BEING DUMPED TO THE S3 BUCKET
		 * SHOULD NOT EXIST IN THE FINAL IMPLEMENTATION
		 */
		
		newspaperJson = entityCache.getIfPresent(tempId);
		if(newspaperJson != null){
            JSONArray mentions = newspaperJson.getJSONArray("mentions");
            //This is where the injectAnnotations of a ImpressoContentItem
            contentItem.injectTokens(mentions, null, false, 0);
        } else {
		    inEnt = false;
        }

		if(inEnt != inNews){
		    System.out.println("News :"  + inNews);
		    System.out.println("Entities :"  + inEnt);
        }

		return contentItem;
	}
}
