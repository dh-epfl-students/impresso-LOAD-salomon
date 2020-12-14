package wikidatademo.graph;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import impresso.ImpressoContentItem;
import impresso.SolrReader;
import org.bson.Document;
import wikidatademo.logger.QueryException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.PROP_PATH;
import static settings.WebInterfaceSettings.*;

/**
 * LOAD graph for GUI Demo
 * 
 * Originally published November 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class LOADGraphImplDatabase extends LOADGraphAbstr {
	
	private MongoClient mongoClient;
	private MongoDatabase load_db;
	private MongoCollection<Document> colMeta;
	private MongoCollection<Document> colEdges;
	private MongoCollection<Document> colSEN;
	private MongoCollection<Document> colPAG;
	private MongoCollection<Document> colTER;
	private MongoCollection<Document> colENT;
	private MongoCollection<Document> colNodeDegs;
	
	private int[] entitySetSizes;

	private SolrReader reader;
	
	public LOADGraphImplDatabase() {
		
		super(); // parent constructor of abstract class
		
		Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(Level.WARNING);
		
		ServerAddress address = LOCAL ? new ServerAddress(localMongoAddress, localMongoPort) : new ServerAddress(MongoAdress, MongoPort);
		if (mongocred != null) {
			mongoClient = new MongoClient(address, Arrays.asList(mongocred));
		} else {
			mongoClient = new MongoClient(address);
		}
		
		load_db = mongoClient.getDatabase(target_MongoName_LOAD);
		colMeta = load_db.getCollection(MongoCollection_Meta);
		colEdges = load_db.getCollection(MongoCollection_Edges);
		colSEN = load_db.getCollection(MongoCollection_Sentences);
		colPAG = load_db.getCollection(MongoCollection_Pages);
		colTER = load_db.getCollection(MongoCollection_Terms);
		colENT = load_db.getCollection(MongoCollection_Entities);
		colNodeDegs = load_db.getCollection(MongoCollection_NodeDegrees);
		
		readEntitySetSizes();
		String propFilePath =PROP_PATH;
		Properties prop = new Properties();
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(propFilePath);
			prop.load(inputStream);
			inputStream.close();

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		reader = new SolrReader(prop);
	}
	
	public void close() {
		mongoClient.close();
		closeLogger();
	}
	
	/*
	 * Get the sizes of entity sets from mongoDB (required for TF-IDF computations)
	 */
	private void readEntitySetSizes() {
		entitySetSizes = new int[nANNOTATIONS];
		MongoCursor<Document> cursor = colMeta.find().noCursorTimeout(true).iterator();
		Document doc = cursor.next();
		for (int i=0; i<nANNOTATIONS; i++) {
			int degree = doc.getInteger(setNames[i]);
			entitySetSizes[i] = degree;
		}		
		cursor.close();
		
	}
	
	/* write data of successful query to logfile */
	private void logSuccessfulQuery (ArrayList<EntityQueryItem> queryEntities, long ms, String querytype) {
		try {
			String message = "Query successful for " + querytype + " in " + ms + "ms:";
			for (EntityQueryItem q : queryEntities) {
				message += " (" + q.toString() + ")";
			}
			addToLog(message);
		} catch (Exception e) {
			// if we can't log then there's no point in handling this exception
		}
	}

	/* retrieve a ranked list of entities (L, O, A, D) that match an input label
	 * label: input string to be matched
	 * number: max length of the result list */
	public ArrayList<EntityInfoItem> getEntitiesByLabel(String label, int number) throws QueryException {
		
		ArrayList<EntityInfoItem> result = new ArrayList<EntityInfoItem>();

		try {
			MongoCursor<Document> cursor = colENT.find(Filters.text(label))
					                             .projection(Projections.metaTextScore(ci_Entity_score))
					                             .sort(Sorts.orderBy(Sorts.metaTextScore(ci_Entity_score), Sorts.descending(ci_Entity_senDegree)))
					                             .noCursorTimeout(true).iterator();
			
			while (number > 0 && cursor.hasNext()) {
				number--;
				Document doc = cursor.next();
				EntityInfoItem eii = new EntityInfoItem(doc);
				
				if (eii.type.equals(setNames[DAT])) {
					try {
						if (eii.label.length() == 10) {
							Date d = short_dateFormatter10.parse(eii.label);
							eii.description = long_dateFormatter10.format(d);
						} else if (eii.label.length() == 7) {
							Date d = short_dateFormatter7.parse(eii.label);
							eii.description = long_dateFormatter7.format(d);
						} else {
							eii.description = "Year " + eii.label;
						}
					} catch (Exception e) {
						// do nothing, in this case we simply have no description
					}
				}
	
				result.add(eii);
			}
			
		} catch (Exception e) {
			String message = "Error occurred while attempting to look up entity IDs. Input Query: " + label + " | " + number;
			
			addToLog(message);
			throw(new QueryException(message,e));
		}

		return result;
	}
	

	/* retrieve the internal ID of a term
	 * label: input string to be matched to a term */
	public EntityInfoItem getTermByLabel(String label) throws QueryException {
		EntityInfoItem result;
		
		try {
			String stemmedLabel = stemString(label.trim()).toLowerCase();
			
			MongoCursor<Document> cursor = colTER.find(eq(ci_Terms_label, stemmedLabel)).noCursorTimeout(true).iterator();
			
			if (cursor.hasNext()) {
				result = new EntityInfoItem(cursor.next());
				result.description = "Stemmed form of " + label;	// add original string (unstemmed) as description
				result.type = setNames[TER];
			} else {
				result = null;
			}
		} catch (Exception e) {
			String message = "Error occurred while attempting to look up term IDs. Input Query: " + label;
			
			addToLog(message);
			throw(new QueryException(message,e));
		}

		return result;
	}

	/* Query the graph for a page ranking (P)
	 * 
	 * Parameters
	 * queryEntities: list of entities to be used in the query
	 * number:        max length of the result list 
	 * 
	 * Returns a list of EntityInfoItems e such that:
	 *  e.node_id     is the internal graphID of the page object
	 *  e.type        equals PAG
	 *  e.score       is the ranking score of the page
	 *  e.label       contains the page Wikipedia URL
	 *  e.description contains the Wikipedia page title    */
	public ArrayList<EntityInfoItem> pageQuery(ArrayList<EntityQueryItem> queryEntities, int number) throws QueryException {
		
		long start = System.currentTimeMillis();
		
		ArrayList<EntityInfoItem> retval = new ArrayList<EntityInfoItem>(); 
		if (queryEntities.isEmpty()) {
			return retval;
		}
		
		try {
			// Hashmap for counting the influence aggregation of pages based on their sentences
			HashMap<Integer, OccurrenceScoreItem> pages = new HashMap<Integer, OccurrenceScoreItem>();
			
			// get a ranking of sentences for the query entities
			ArrayList<OccurrenceScoreItem> sentences = rawSentenceQuery(queryEntities);
			
			// iterate over all sentences and propagate weights to pages
			double maxScore = 0;
			for (OccurrenceScoreItem osi : sentences) {
				int pageID = osi.page_id;
				
				if (pages.containsKey(pageID)) {
					OccurrenceScoreItem si = pages.get(pageID);
					
					if (osi.occurrence > si.occurrence) {
						si.occurrence = osi.occurrence;
					}
					si.score += osi.score;
					
					// keep track of max score for normalization later
					if (si.score > maxScore) {
						maxScore = si.score;
					}
				} else {
					pages.put(pageID, new OccurrenceScoreItem(pageID, osi.occurrence, osi.score, pageID));
					
					// keep track of max score for normalization later
					if (osi.score > maxScore) {
						maxScore = osi.score;
					}
				}
			}
			
			// sort the resulting pages by occurrence first, then score
			ArrayList<OccurrenceScoreItem> pageList = new ArrayList<OccurrenceScoreItem>(pages.values());
			Collections.sort(pageList);
	
			// go through all pages and fetch the top pages as result
			int counter = 0;
			while (counter < number && counter < pageList.size()) {
				
				OccurrenceScoreItem si = pageList.get(counter);
				int page_id = si.id;
				double score = si.occurrence-1 + si.score / maxScore;
				
				MongoCursor<Document> pageCursor = colPAG.find(new Document(ci_Page_id, page_id)).iterator();
				Document page = pageCursor.next();


				String pageTitle = reader.getContentItem(page.getString("article_id")).getTitle();

				EntityInfoItem pag = new EntityInfoItem(page_id, setNames[PAG], score, page.getString("article_id"), pageTitle);
				retval.add(pag);
				
				pageCursor.close();
				counter++;
			}
			
		} catch (Exception e) {
			String message = "Error occurred during a page query. Input Query:";
			for (EntityQueryItem q : queryEntities) {
				message += " (" + q.toString() + ")";
			}
			addToLog(message);
			throw(new QueryException(message,e));
		}
		logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, "PAG");
			
		return retval;
	}
	
	
	/* Query the graph for a sentence ranking (S)
	 * 
	 * Parameters
	 * queryEntities: list of entities to be used in the query
	 * number:        max length of the result list 
	 * 
	 * Returns a list of EntityInfoItems e such that:
	 *  e.node_id   is the internal graphID of the sentence object
	 *  e.type      equals SEN
	 *  e.score     is the ranking score of the sentence
	 *  e.label     contains the sentence text
	 * */
	public ArrayList<EntityInfoItem> sentenceQuery(ArrayList<EntityQueryItem> queryEntities, int number) throws QueryException {
		
		long start = System.currentTimeMillis();
		
		ArrayList<EntityInfoItem> retval = new ArrayList<EntityInfoItem>(); 
		
		try {
			// check if query is empty
			if (queryEntities.isEmpty()) {
				System.out.println("No query entities provided. Returning empty list.");
				return retval;
			}
	
			// get a raw ranking of sentences
			ArrayList<OccurrenceScoreItem> sortedSentences = rawSentenceQuery(queryEntities);
			Collections.sort(sortedSentences);
	
			int counter = 0;
			while (counter < number && counter < sortedSentences.size()) {
				
				OccurrenceScoreItem si = sortedSentences.get(counter);
				int sentence_id = si.id;
				double score = si.occurrence-1 + si.score / (queryEntities.size() * sentenceTermLimit);
				
				MongoCursor<Document> sentenceCursor = colSEN.find(new Document(ci_Sentences_occurence_id, sentence_id)).iterator();
				Document sentence = sentenceCursor.next();

				ImpressoContentItem sentenceItem = reader.getContentItem(sentence.getString("article_id"));
				String content = sentenceItem.getContent_txt()
						.substring(sentence.getInteger("min_offset"), sentence.getInteger("max_offset"));
				
				EntityInfoItem eOutput = new EntityInfoItem(sentence_id, setNames[SEN], score, content, null);
				retval.add(eOutput);
				
				sentenceCursor.close();
	
				counter++;
			}
			
		} catch (Exception e) {
			String message = "Error occurred during a sentence query. Input Query:";
			for (EntityQueryItem q : queryEntities) {
				message += " (" + q.toString() + ")";
			}
			addToLog(message);
			throw(new QueryException(message,e));
		}
		logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, "SEN");
		
		return retval;
	}
	
	/* Query the graph for a ranking of sentences
	 * 
	 * Parameters
	 * queryEntities: list of entities to be used in the query
	 * 
	 * Returns a list of OccurrenceScoreItem o such that:
	 *  o.id         is the sentence ID
	 *  o.occurrence is the number of occurrences of distinct query entities in the sentence
	 *  o.score      is the overall score of the sentence with regard to query entities
	 * */
	private ArrayList<OccurrenceScoreItem> rawSentenceQuery(ArrayList<EntityQueryItem> queryEntities) {
		
		// check if query is empty
		if (queryEntities.isEmpty()) {
			System.out.println("No query entities provided. Returning empty list.");
			return new ArrayList<OccurrenceScoreItem>();
		}

		// create Hashmap for counting sentences
		HashMap<Integer, OccurrenceScoreItem> sentences = new HashMap<Integer, OccurrenceScoreItem>();
		HashSet<Integer> relevantTerms = new HashSet<Integer>();

		// iterate over all query entities and find related sentences
		for (EntityQueryItem q : queryEntities) {
			int sourceID = q.node_id;
			String sourceType = q.type;
			
			MongoCursor<Document> edgeCursor = colEdges.find(and(eq(ci_Edge_SourceType, sourceType),
                                                                 eq(ci_Edge_SourceID, sourceID),
                                                                 eq(ci_Edge_TargetType, setNames[SEN])
                                                                )
                                                            ).noCursorTimeout(true).iterator();

			// update the neighbouring sentences' counts in the hash map
			while (edgeCursor.hasNext()) {
				Document edge = edgeCursor.next();
				int targetID = edge.getInteger(ci_Edge_TargetID);
				int pageID = edge.getInteger(ci_Edge_PageID);
				//double score = edge.getDouble(ci_Edge_TFIDF);
				
				if (sentences.containsKey(targetID)) {
					OccurrenceScoreItem si = sentences.get(targetID);
					si.occurrence++;
					//si.score += score;
					sentences.put(targetID, si);
				} else {
					//sentences.put(targetID, new OccurrenceScoreItem(targetID, 1, score));
					sentences.put(targetID, new OccurrenceScoreItem(targetID, 1, 0, pageID));
				}
			}
			edgeCursor.close();
			
			
			// TODO: this is not using a ranking of terms yet!!!! Include that
			// use this code:
			//Document order = new Document(ci_Edge_TFIDF, -1);
			// then ...noCursorTimeout(true).sort(order).limit(sentenceTermLimit)...
			
			
			// find the best X terms for the given query entitiy and store them in a Hash set
			MongoCursor<Document> edgeTermCursor = colEdges.find(and(eq(ci_Edge_SourceType, sourceType),
                                                                     eq(ci_Edge_SourceID, sourceID),
                                                                     eq(ci_Edge_TargetType, setNames[TER])
                                                                    )
                                                                ).noCursorTimeout(true).limit(sentenceTermLimit).iterator();
			
			while (edgeTermCursor.hasNext()) {
				Document termEdge = edgeTermCursor.next();
				relevantTerms.add(termEdge.getInteger(ci_Edge_TargetID));
			}
			edgeTermCursor.close();
		}
		
		// go over all relevant terms and slightly increase the weight of sentences if they contain the term
		for (Integer termID : relevantTerms) {
			MongoCursor<Document> termSentenceCursor = colEdges.find(and(eq(ci_Edge_SourceType, setNames[TER]),
                                                                         eq(ci_Edge_SourceID, termID),
                                                                         eq(ci_Edge_TargetType, setNames[SEN])
                                                                        )
                                                                    ).noCursorTimeout(true).iterator();
			while (termSentenceCursor.hasNext()) {
				Document sentenceEdge = termSentenceCursor.next();
				int sentenceID = sentenceEdge.getInteger(ci_Edge_TargetID);
				if (sentences.containsKey(sentenceID)) {
					sentences.get(sentenceID).score += 1.0;
				}
			}
			termSentenceCursor.close();
		}

		// turn HashSet into Array list
		ArrayList<OccurrenceScoreItem> retval = new ArrayList<OccurrenceScoreItem>(sentences.values());
		
		return retval;
	}
	
	
	/* Query the graph for an entity ranking (L, O, A, D, T)
	 * 
	 * Parameters
	 * queryEntities: list of entities to be used in the query
	 * targetType:    type of connected entitiy that should be ranked
	 * number:        max length of the result list 
	 * 
	 * Returns a list of EntityInfoItems e (all of type targetType) such that:
	 *  e.node_id     is the internal graphID of the entity
	 *  e.type        equals the type of the entity (targetType)
	 *  e.score       is the ranking score of the enity
	 *  e.label       contains the entity label
	 *  e.wikidata_id contains the wikidata ID of the entity (only if type == LOC, ORG or ACT)
	 * */
	public ArrayList<EntityInfoItem> entityQuery(ArrayList<EntityQueryItem> queryEntities, String targetType, int number) throws QueryException {
		
		long start = System.currentTimeMillis();
		
		// special case: query is empty
		ArrayList<EntityInfoItem> retval = new ArrayList<EntityInfoItem>(); 
		if (queryEntities.isEmpty()) {
			//System.out.println("No query entities provided. Returning empty list.");
			return retval;
		}
		
		try {
			// special case: query has only one element
			if (queryEntities.size() == 1) {
				
				EntityQueryItem queryEntity = queryEntities.get(0);
				ArrayList<EntityInfoItem> SEQ = singleEntityQuery(queryEntity, targetType);
				
				retval = sortAndLimitResultListSize(SEQ, number);
				retval = retrieveEntityInformation(retval);
				normalizeScores(retval);
				
			// if we have more than one query entity
			} else {
			
				// otherwise, if we get here: query has multiple elements (of type L O A D T)
				HashMap<EntityInfoItem, Tuple> candidates = new HashMap<EntityInfoItem, Tuple>();
				
				// iterate over all query items and retrieve their results
				// then count the occurrences in a hash map and add the tfidf scores
				double maxTFIDF = 0;
				for (EntityQueryItem qe : queryEntities) {
					
					ArrayList<EntityInfoItem> SEQ = singleEntityQuery(qe, targetType);
					for (EntityInfoItem e : SEQ) {
						if (candidates.containsKey(e)) {
							Tuple t = candidates.get(e);
							t.count++;
							t.score += e.score;
							candidates.put(e,t);
							
							// store max TFIDF value for normalization
							if (t.score > maxTFIDF) {
								maxTFIDF = t.score;
							}
							
						} else {
							candidates.put(e, new Tuple(1, e.score));
							
							// store max TFIDF value for normalization
							if (e.score > maxTFIDF) {
								maxTFIDF = e.score;
							}
						}
					}
				}
				
				// remove results entities that are query entities as well
				for (EntityQueryItem q : queryEntities) {
					EntityInfoItem e = new EntityInfoItem(q.node_id, q.type, 0, null, null);
					candidates.remove(e);
				}
				
				// create ranking for all items and put them in the result set
				// ranking score x.y with
				// x = cohesion -1 (number of other entities in that are connected to this result)
				// y = normalized TFIDF score in [0,1]
				for (Entry<EntityInfoItem,Tuple> entry : candidates.entrySet()) {
					EntityInfoItem e = entry.getKey();
					Tuple t = entry.getValue();
					e.score = t.count-1 + t.score / maxTFIDF;
					retval.add(e);
				}
				retval = sortAndLimitResultListSize(retval, number);
				retval = retrieveEntityInformation(retval);
			}
		
		} catch (Exception e) {
			String message = "Error occurred during an entity query. Target: (" + targetType + "), Input Query:";
			for (EntityQueryItem q : queryEntities) {
				message += " (" + q.toString() + ")";
			}
			addToLog(message);
			throw(new QueryException(message,e));
		}
		logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, targetType);
		
		return retval;
	}
	
	/* Query the graph for an entity ranking (L, O, A, D, T)
     *
	 * Parameters
	 * queryEntity:   entity to be used in the query
	 * targetType:    type of connected entitiy that should be ranked (LOADT)
	 * number:        max length of the result list 
	 * 
	 * Returns a list of EntityInfoItems e (all of type targetType) such that:
	 *  e.node_id     is the internal graphID of the entity
	 *  e.type        equals the type of the entity (targetType)
	 *  e.score       is the ranking score of the enity
	 *  e.label       contains the entity label
	 *  e.wikidata_id contains the wikidata ID of the entity (only if type == LOC, ORG or ACT)
	 * */
	private ArrayList<EntityInfoItem> singleEntityQuery(EntityQueryItem queryEntity, String targetType) {
		
		ArrayList<EntityInfoItem> retval = new ArrayList<EntityInfoItem>(); 
		
		int sourceID = queryEntity.node_id;
		String sourceType = queryEntity.type;
		Document order = new Document(ci_Edge_TFIDF, -1);
		
		// find all neighbours of the desired type
		MongoCursor<Document> edgeCursor = colEdges.find(and(eq(ci_Edge_SourceType, sourceType),
                                                            eq(ci_Edge_SourceID, sourceID),
                                                            eq(ci_Edge_TargetType, targetType)
                                                           )
                                                        ).noCursorTimeout(true).sort(order).iterator();
		
		while (edgeCursor.hasNext()) {
			Document edge = edgeCursor.next();
			int targetID = edge.getInteger(ci_Edge_TargetID);
			double score = edge.getDouble(ci_Edge_TFIDF);
			EntityInfoItem e = new EntityInfoItem(targetID, targetType, score, null, null);
			retval.add(e);
		}
		edgeCursor.close();
		
		return retval;
	}
	
	/* Query the graph for entity information (L, O, A, D, T)
     *
	 * Parameters
	 * entityList:   list of ranked entities that are missing knowledge base information
	 * 
	 * Returns the a list of the same EntityInfoItems e that have been augmented with KB information 
	 *  e.node_id     is the internal graphID of the entity
	 *  e.type        equals the type of the entity (targetType)
	 *  e.score       is the ranking score of the enity
	 *  e.label       contains the entity label
	 *  e.wikidata_id contains the wikidata ID of the entity (only if type == LOC, ORG or ACT)
	 * */
	private ArrayList<EntityInfoItem> retrieveEntityInformation(ArrayList<EntityInfoItem> entityList) {
		
		ArrayList<EntityInfoItem> retval = new ArrayList<EntityInfoItem>(); 
		
		for (EntityInfoItem eInput : entityList) {
			int entityID = eInput.node_id;
			String type = eInput.type;
			double score = eInput.score;
			
			if (type.equals(setNames[TER])) {
				MongoCursor<Document> termCursor = colTER.find(new Document(ci_Terms_id, entityID)).iterator();
				Document term = termCursor.next();
				
				String content = term.getString(ci_Terms_label);
				EntityInfoItem eOutput = new EntityInfoItem(entityID, type, score, content, null);
				eOutput.isQueryEntity = eInput.isQueryEntity;
				retval.add(eOutput);
				
				termCursor.close();
			} else { // entity is of type L O A D
				MongoCursor<Document> entityCursor = colENT.find(and(eq(ci_Entity_type, type),
                                                                     eq(ci_Entity_id, entityID)
                                                                    )
                                                                ).iterator();


				Document entityDoc = entityCursor.next();

				EntityInfoItem eOutput = new EntityInfoItem(entityDoc);

				if (type.equals(setNames[DAT])) {
					try {
						if (eOutput.label.length() == 10) {
							Date d = short_dateFormatter10.parse(eOutput.label);
							eOutput.description = long_dateFormatter10.format(d);
						} else if (eOutput.label.length() == 7) {
							Date d = short_dateFormatter7.parse(eOutput.label);
							eOutput.description = long_dateFormatter7.format(d);
						} else {
							eOutput.description = "Year " + eOutput.label;
						}
					} catch (Exception e) {
						// do nothing, in this case we simply have no description
					}
				}
				eOutput.score = score;
				eOutput.isQueryEntity = eInput.isQueryEntity;
				retval.add(eOutput);

				entityCursor.close();
			}
		}

		return retval;
	}
	
	/* 
     * Normalize the scores of the input entityInfoItem list to the interval [0,1]
     * Assumes that the list is sorted and the element with the highest score is the head of the list
     */
	private void normalizeScores(ArrayList<EntityInfoItem> entityList) {
		
		if (!entityList.isEmpty()) {
			double max = entityList.get(0).score;
			for (EntityInfoItem e : entityList) {
				e.score = e.score / max;
			}
		}
	}
	
	/* 
     * Limits the size of a ranked entity list to the top "number" entries
     * 
	 * Parameters
	 * entityList:   list of ranked entities that are missing knowledge base information
	 * 
	 * Returns the a list of the same EntityInfoItems e that have been augmented with KB information 
	 *  e.node_id     is the internal graphID of the entity
	 *  e.type        equals the type of the entity (targetType)
	 *  e.score       is the ranking score of the enity
	 *  e.label       contains the entity label
	 *  e.wikidata_id contains the wikidata ID of the entity (only if type == LOC, ORG or ACT)
	 * */
	private ArrayList<EntityInfoItem> sortAndLimitResultListSize(ArrayList<EntityInfoItem> entityList, int number) {
		
		if (entityList.isEmpty()) {
			return entityList;
		}
		
		Collections.sort(entityList);
		ArrayList<EntityInfoItem> retval = new ArrayList<EntityInfoItem>();
		
		int counter = 0;
		while (counter < number && counter < entityList.size()) {
			retval.add(entityList.get(counter));
			counter++;
		}
		return retval;
	}
	
	/* Query the graph for an  subgraph that surrounds the query entities - only (L, O, A, D, T) as input!
	 * 
	 * Parameters
	 * queryEntities: list of entities to be used in the query
	 * nNeighbours: number of neighbours of each type for each node in the query 
	 * 
	 * Returns a subgraph object for the query entities
	 * */
	public SubgraphItem subgraphQuery(ArrayList<EntityQueryItem> queryEntities) throws QueryException {
		
		long start = System.currentTimeMillis();
		
		SubgraphItem retval = new SubgraphItem();
		
		try {
			ArrayList<EntityInfoItem> nodes = new ArrayList<EntityInfoItem>();
			HashSet<EdgeItem> edges = new HashSet<EdgeItem>();
			
			// add all query entities as nodes in the graph
			for (EntityQueryItem q : queryEntities) {
				EntityInfoItem e = new EntityInfoItem(q.node_id, q.type, 0, null, null);
				e.isQueryEntity = true;
				nodes.add(e);
			}
			
			// create hash maps for counting the occurrences and scores of neighbours
			HashMap<String,HashMap<EntityInfoItem, Tuple>> neighbourCounts = new HashMap<String,HashMap<EntityInfoItem, Tuple>>();
			for (int type=0; type <= TER; type++) {
				neighbourCounts.put(setNames[type], new HashMap<EntityInfoItem, Tuple>());
			}
			
			// iterate over all query entities and get all neighbours
			// sort them by their type and count their occurrences
			for (EntityQueryItem q : queryEntities) {
				for (int type=0; type <= TER; type++) {
					String targetType = setNames[type];
					HashMap<EntityInfoItem, Tuple> candidates = neighbourCounts.get(targetType);
				
					ArrayList<EntityInfoItem> SEQ = singleEntityQuery(q, targetType);
					for (EntityInfoItem e : SEQ) {
						if (candidates.containsKey(e)) {
							Tuple t = candidates.get(e);
							t.count++;
							t.score += e.score;
							candidates.put(e,t);
						} else {
							candidates.put(e, new Tuple(1, e.score));
						}
					}
				}
			}
			
			// remove neighbours that do not match the cohesion (occur not often enough)
			for (int type=0; type <= TER; type++) {
				String targetType = setNames[type];
				HashMap<EntityInfoItem, Tuple> candidates = neighbourCounts.get(targetType);
				ArrayList<EntityInfoItem> neighbourList = new ArrayList<EntityInfoItem>();
				
				for (Entry<EntityInfoItem,Tuple> entry : candidates.entrySet()) {
					if (entry.getValue().count >= cohesion) {
						EntityInfoItem e = entry.getKey();
						//Tuple t = entry.getValue();
						//e.score = t.score;
						neighbourList.add(e);
					}
				}
				
				// limit the length of the list to the top nNeighbour entities
				neighbourList = sortAndLimitResultListSize(neighbourList, graphNeighbourLimit);
				for (EntityInfoItem e : neighbourList) {
					nodes.add(e);
				}			
			}
			
			// retrieve all edges between nodes in the graph
			double maxEdgeWeight = 0;
			for (int i=0; i<nodes.size(); i++) {
				EntityInfoItem source = nodes.get(i);
				int sourceID = source.node_id;
				String sourceType = source.type;
				String sourceStringID = sourceType + sourceID;
				
				for (int j=i+1; j<nodes.size(); j++) {
					EntityInfoItem target = nodes.get(j);
					int targetID = target.node_id;
					String targetType = target.type;
					
					MongoCursor<Document> edgeCursor = colEdges.find(and(eq(ci_Edge_SourceType, sourceType),
                                                                         eq(ci_Edge_SourceID, sourceID),
                                                                         eq(ci_Edge_TargetType, targetType),
                                                                         eq(ci_Edge_TargetID, targetID)
                                                                        )
                                                                    ).noCursorTimeout(true).iterator();
					
					if (edgeCursor.hasNext()) {
						Document edge = edgeCursor.next();
						double weight = edge.getDouble(ci_Edge_Weight);
						
						if (weight >= subgraphEdgeWeightLimit) {
							String targetStringID = targetType + targetID;
							EdgeItem graphEdge = new EdgeItem(sourceStringID, targetStringID, weight);
							edges.add(graphEdge);
							if (weight > maxEdgeWeight) {
								maxEdgeWeight = weight;
							}
						}
					}
					edgeCursor.close();
					
				}
			}
	
			// normalize edges and include them in the final graph
			retval.edges.addAll(edges);
			for (EdgeItem e : edges) {
				e.weight = e.weight / maxEdgeWeight;
			}
	
			// retrieve KB information for all nodes
			retval.nodes = retrieveEntityInformation(new ArrayList<EntityInfoItem>(nodes));
			
		} catch (Exception e) {
			String message = "Error occurred during a graph query. Input Query:";
			for (EntityQueryItem q : queryEntities) {
				message += " (" + q.toString() + ")";
			}
			addToLog(message);
			throw(new QueryException(message,e));
		}
		logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, "GRAPH");
		
		return retval;
	};
	
	
}
