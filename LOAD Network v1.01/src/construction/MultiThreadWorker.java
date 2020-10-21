package construction;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// mongoDB imports
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

// Porter stemmer library imports
import org.tartarus.snowball.SnowballStemmer;

/**
 * Creates a LOAD subgraph from a single document
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class MultiThreadWorker implements Runnable {
    
    private MultiThreadHub hub;
    private MongoCollection<Document> cANN;
    private MongoCollection<Document> cSEN;
    HashSet<String> stopwords;
    
    // internal variables
    HashSet<String> invalidTypes;
    private int[] count_ValidAnnotationsByType;
    private SnowballStemmer stemmer;
    private long count_unaggregatedEdges;
    private int failedCount;
    private int negativeOffsetCount;
    private static Pattern pattern = Pattern.compile(datepattern);
    
    public MultiThreadWorker(MultiThreadHub hub, MongoCollection<Document> cANN, MongoCollection<Document> cSEN, HashSet<String> stopwords) {
        this.hub = hub;
        this.cANN = cANN;
        this.cSEN = cSEN;
        this.stopwords = stopwords;
        
        // internal variables
        invalidTypes = new HashSet<String>();
        count_ValidAnnotationsByType = new int[nANNOTATIONS];
        count_unaggregatedEdges = 0;
        failedCount = 0;
        negativeOffsetCount = 0;
        
        // stemmer used for stemming terms
        stemmer = getStemmer(stemmerLanguage);
    }
     
    @Override
    public void run() {
        
        ArrayList<Annotation> annotationsPage = new ArrayList<Annotation>();
        ArrayList<Annotation> annotationsSentence = new ArrayList<Annotation>();
        ArrayList<Annotation> annotationsTerms = new ArrayList<Annotation>();
        
        HashSet<String> invalidTypes = new HashSet<String>();
        ArrayList<String> edges = new ArrayList<String>();
        
        int invalidAnnotationCount = 0;
        int annotationCounter = 0;
        Integer page_id = null;
        
        
        while ( (page_id = hub.getPageID()) != null ) {
        	
        	//using content id create impressocontentitem and use this to read from solr, then inject tokens
        	
            annotationsPage.clear();
            edges.clear();
                
            // ensure that cursor cannot time out during write operations
            MongoCursor<Document> sentenceCursor = cSEN.find(new Document(mongoIdentSentence_pageId, page_id))
                                                            .noCursorTimeout(true).iterator();
            while (sentenceCursor.hasNext()) {
                annotationsSentence.clear();
                annotationsTerms.clear();
                    
                try {
                    Document objSEN = sentenceCursor.next();
                    String pageId_str = Integer.toString(page_id);
                    String sentence_mongoid_str = objSEN.get(mongoIdentSentence_id).toString();
                    String sentenceContent = objSEN.getString(mongoIdentSentence_content);
                    int sentence_id = objSEN.getInteger(mongoIdentSentence_sentenceId);
    
                    // get list of all annotations in the sentence
                    MongoCursor<Document> annotationCursor = cANN.find(and(eq(mongoIdentAnnotation_pageId, page_id),
                                                                           eq(mongoIdentAnnotation_sentenceId, sentence_id)
                                                                          )
                                                                      ).noCursorTimeout(true).iterator();
                        
                    if (annotationCursor.hasNext()) { // if there are annotations in the sentence
                        boolean hasAnnotations = false;
                        
                        // create copy for deletion of annotated portions
                        char[] mask = sentenceContent.toCharArray();
                        
                        // DATES: 
                        // iterate over temporal annotations and extract them to create nodes
                        while (annotationCursor.hasNext()) {
                            Document obj = annotationCursor.next();
                            String annotationType_str = (String)obj.get(mongoIdentAnnotation_neClass);
                            
                            char annotationType;
                            if (annotationType_str.equals(dat)) {
                                annotationType = DAT;
                            } else if (annotationType_str.equals(loc)) {
                                annotationType = LOC;
                            } else if (annotationType_str.equals(act)) {
                                annotationType = ACT;
                            } else if (annotationType_str.equals(org)) {
                                annotationType = ORG;
                            } else {
                                invalidTypes.add(annotationType_str);
                                invalidAnnotationCount++;
                                continue;
                            }
                                
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
                                }

                            } else if (annotationType == LOC || annotationType == ACT || annotationType == ORG) {

                                int begin = (Integer) obj.get(mongoIdentAnnotation_start);
                                int end = (Integer) obj.get(mongoIdentAnnotation_end);

                                // get covered text and clean it up
                                String value = (String) obj.get(mongoIdentAnnotation_coveredText);
                                value = replaceAndTrimNames(value).toLowerCase();
                                
                                // INSTEAD, ONLY FOR WIKIDATA ENTITIES: (do not perform any changes to the value)
                                //String value =" Q" + obj.get(mongoIdentAnnotation_normalized).toString();
                                    
                                // get an id
                                int annId = hub.getAnnotationID(annotationType, value);
    
                                // add annotation to list for later edge creation
                                Annotation ann = new Annotation(value, annId, annotationType, sentence_id);
                                annotationsSentence.add(ann);

                                // WORKAROUND / HEURISTIC
                                // In some cases, there is an overlap between NEs and sentences. If an NE is assigned to
                                // a sentence but starts before the sentence, pretend that it starts at the first character
                                // of the sentence for the purpose of creating the bitmask
                                if (begin < 0) {
                                    begin = 0;
                                    negativeOffsetCount++;                                    
                                }
                                
                                // mark portion of the sentence that is covered for deletion
                                for (int p=begin; p<end; p++) {
                                    mask[p] = replaceableChar;
                                }
                                    
                                hasAnnotations = true;
                                annotationCounter++;
                                count_ValidAnnotationsByType[annotationType]++;

                            }
                        }
                            
                        // add this sentence and the corresponding page to the graph if it had valid annotations
                        if (hasAnnotations) {
                                
                            // add sentence to the map
                            int sentenceId = hub.getAnnotationID(SEN, sentence_mongoid_str);
                            count_ValidAnnotationsByType[SEN]++;
                                
                            // add page / document to the map
                            int pageId = hub.getAnnotationID(PAG, pageId_str);
                            count_ValidAnnotationsByType[PAG]++;

                            // remove marked parts of the sentence and turn the rest into Terms
                            String content = new String(mask);
                            content = content.replaceAll(replaceableString," ");    // remove annotations that were marked for deletion
                                
                            String[] wordsBag = content.split("" );
                            for (String s : wordsBag) {
                                s = replaceAndTrimTerms(s).toLowerCase();
                                    
                                if (!stopwords.contains(s)) {
                                    stemmer.setCurrent(s);
                                    stemmer.stem();
                                    s = stemmer.getCurrent();
                                        
                                    if (s.length() >= minWordLength) {
                                        int annId = hub.getAnnotationID(TER, s);
                                        Annotation ann = new Annotation(s, annId, TER, sentence_id);
                                        annotationsTerms.add(ann);
                                    }
                                }
                            }
                                
                            // turn list of annotations into edges by pairwise comparison
                            // add edge between sentence and page
                            edges.add(PAG + sepChar + SEN + sepChar + pageId + sepChar + sentenceId + sepChar + 0 +" \n");
                            count_unaggregatedEdges++;
                                
                            for (int i=0; i<annotationsSentence.size(); i++) {
                                Annotation an = annotationsSentence.get(i);
                                    
                                // NOTE connecting entities to the sentence is enough (sentences are connected to pages)
                                // add edge between annotation and page
                                // ew.append(an.type + sepChar + PAG + sepChar + an.id + sepChar + pageId + sepChar + 0 +" \n");
                                // count_unaggregatedEdges++;
                                
                                // add edge between annotation and sentence
                                edges.add(an.type + sepChar + SEN + sepChar + an.id + sepChar + sentenceId + sepChar + 0 +" \n");
                                count_unaggregatedEdges++;
                                    
                                annotationsPage.add(an);
                            }
                                
                            for (int i=0; i<annotationsTerms.size(); i++) {
                                Annotation t = annotationsTerms.get(i);
                                    
                                // NOTE connecting terms to the sentence is enough (sentences are connected to pages)
                                // add edge between term and page
                                // ew.append(t.type + sepChar + PAG + sepChar + t.id + sepChar + pageId + sepChar + 0 +" \n");
                                // count_unaggregatedEdges++;
                                    
                                // add edge between term and sentence
                                edges.add(t.type + sepChar + SEN + sepChar + t.id + sepChar + sentenceId + sepChar + 0 +" \n");
                                count_unaggregatedEdges++;
                                    
                                // add pairwise edges between terms and annotations in the same sentence (but only in one direction) 
                                for (int j=0; j<annotationsSentence.size(); j++) {
                                    Annotation an = annotationsSentence.get(j);
                                    edges.add(an.type + sepChar + t.type + sepChar + an.id + sepChar + t.id + sepChar + 0 +" \n");
                                    count_unaggregatedEdges++;
                                }
                                    
                            }
                        }                        
                    }
                    annotationCursor.close();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    failedCount++;
                }
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
                        break;
                    }
                        
                    if (an1.type != an2.type) { // connections between entity types
                        if (an1.type < an2.type) {
                            edges.add(an1.type + sepChar + an2.type + sepChar + an1.id + sepChar + an2.id + sepChar + weight +" \n");
                            count_unaggregatedEdges++;
                        } else {
                            edges.add(an2.type + sepChar + an1.type + sepChar + an2.id + sepChar + an1.id + sepChar + weight +" \n");
                            count_unaggregatedEdges++;
                        }
                    } else if (an1.type == LOC || an1.type == ACT || an1.type == ORG) { // connections within entity types
                        if (an1.id < an2.id) {
                            edges.add(an1.type + sepChar + an2.type + sepChar + an1.id + sepChar + an2.id + sepChar + weight +" \n");
                            count_unaggregatedEdges++;
                        } else if (an1.id > an2.id) {
                            edges.add(an2.type + sepChar + an1.type + sepChar + an2.id + sepChar + an1.id + sepChar + weight +" \n");
                            count_unaggregatedEdges++;
                        }
                        // the case where an1.id == an2.id is ignored since we do not want self loops in the network
                    }
                }
            }
            sentenceCursor.close();
            try {
                hub.writeEdges(edges);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // update the total statistics for summing up over all threads
        hub.updateStatistics(annotationCounter, count_unaggregatedEdges, failedCount, invalidAnnotationCount, invalidTypes,
                             count_ValidAnnotationsByType, negativeOffsetCount);
        
        hub.latch.countDown();
        
    }
    
    
}
