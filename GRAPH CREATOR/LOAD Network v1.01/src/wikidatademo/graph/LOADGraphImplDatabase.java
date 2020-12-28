package wikidatademo.graph;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static settings.LOADmodelSettings.*;
import static settings.WebInterfaceStaticSettings.*;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import settings.WebInterfaceSettings;

//import com.mongodb.ReadPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import wikidatademo.logger.QueryException;
import wikidatademo.users.UserManagement;
import wikidatademo.users.UserToken;

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
	private MongoCollection<Document> colEdges;
	private MongoCollection<Document> colSEN;
	private MongoCollection<Document> colPAG;
	private MongoCollection<Document> colTER;
	private MongoCollection<Document> colENT;
	private MongoCollection<Document> colLOOK;
	private Session ssh = null;

	private UserManagement userManagement;

	public LOADGraphImplDatabase(WebInterfaceSettings settings) {

		super(settings); // parent constructor of abstract class

		Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(Level.WARNING);

		if(settings.useSSHtunnel)
		{
			try {
				Properties config = new Properties();
				config.put("StrictHostKeyChecking", "no");
				JSch jsch = new JSch();
				ssh = jsch.getSession(settings.LDAPUsername, settings.SSHHost, settings.SSHport);
				ssh.setPassword(settings.LDAPPassword);
				ssh.setConfig(config);
				ssh.connect();
				ssh.setPortForwardingL(8989, settings.MongoAdress, settings.MongoPort);
				mongoClient = new MongoClient(new ServerAddress("localhost", 8989));
//				mongoClient.setReadPreference(ReadPreference.nearest());


			} catch (JSchException e) {
				e.printStackTrace();
				String msg="Cannot make ssh connections ! "+e.getMessage();
				System.out.println(msg);
			}

		}
		else {
			ServerAddress address = new ServerAddress(settings.MongoAdress, settings.MongoPort);
			if (settings.mongocred != null) {
				mongoClient = new MongoClient(address, Arrays.asList(settings.mongocred));
			} else {
				mongoClient = new MongoClient(address);
			}
		}

		load_db = mongoClient.getDatabase(settings.MongoDBName);
		colEdges = load_db.getCollection(MongoCollection_Edges);
		colSEN = load_db.getCollection(MongoCollection_Sentences);
		colPAG = load_db.getCollection(MongoCollection_Pages);
		colTER = load_db.getCollection(MongoCollection_Terms);
		colENT = load_db.getCollection(MongoCollection_Entities);
		colLOOK = load_db.getCollection(MongoCollection_EntityLookup);

		userManagement = new UserManagement(settings.maxQueryCount, settings.maxQueryDuration);

	}


	public void close() {
		mongoClient.close();
		closeLogger();
		if (ssh != null) {
			ssh.disconnect();
		}
	}

	/* write data of successful query to logfile */
	private void logSuccessfulQuery (ArrayList<EntityQueryItem> queryEntities, long ms, String querytype, String user) {
		try {
			String message = "[" + user + "] Query SUCCEEDED in " + ms + "ms. Target: " + querytype + " Input Query:";
			for (EntityQueryItem q : queryEntities) {
				message += " (" + q.toString() + ")";
			}
			addToLog(message);
			if (settings.verbose) {
				System.out.println(message);
			}
		} catch (Exception e) {
			// if we can't log then there's no point in handling this exception
			if (settings.verbose) {
				e.printStackTrace();
			}
		}
	}

	/* write data of successful query to logfile */
	private void logFailedQuery (ArrayList<EntityQueryItem> queryEntities, String querytype, String user, String reason) {
		try {
			String message = "[" + user + "] Query FAILED: " + reason + " Target: " + querytype + ". Input Query:";
			for (EntityQueryItem q : queryEntities) {
				message += " (" + q.toString() + ")";
			}
			addToLog(message);
			if (settings.verbose) {
				System.out.println(message);
			}
		} catch (Exception e) {
			// if we can't log then there's no point in handling this exception
		}
	}

	private void checkQueryLimit(ArrayList<EntityQueryItem> queryEntities) throws QueryException {
		if (queryEntities.size() >= settings.maxNumberOfQueryEntities) {
			String message = "Maximum number of query entities exceeded (input: " + queryEntities.size() + ", max: " + settings.maxNumberOfQueryEntities + ").";
			throw(new QueryException(message));
		}
	}

	/* retrieve a ranked list of entities (L, O, A, D) that match an input label
	 * label: input string to be matched
	 * number: max length of the result list */
	public ArrayList<EntityInfoItem> getEntitiesByLabel(String label, int number, String user) throws QueryException {

		ArrayList<EntityInfoItem> result = new ArrayList<EntityInfoItem>();

		try {
			HashSet<EntityIDItem> usedIDs = new HashSet<EntityIDItem>();

			long c = colLOOK.count();
			MongoCursor<Document> cursor = colLOOK.find(Filters.text(label))
					.projection(Projections.metaTextScore(ci_Lookup_score))
					.sort(Sorts.orderBy(Sorts.metaTextScore(ci_Lookup_score), Sorts.descending(ci_Lookup_senDegree)))
					.noCursorTimeout(true).iterator();

			while (number > 0 && cursor.hasNext()) {
				number--;
				Document doc = cursor.next();

				// make sure that duplicate entities are not suggested again
				EntityIDItem item = new EntityIDItem(doc.getInteger(ci_Lookup_id), doc.getString(ci_Lookup_type));
				if (!usedIDs.contains(item)) {

					EntityInfoItem entityInfo = new EntityInfoItem(doc);

					if (entityInfo.type.equals(setNames[DAT])) {
						try {
							if (entityInfo.label.length() == 10) {
								Date d = short_dateFormatter10.parse(entityInfo.label);
								entityInfo.description = long_dateFormatter10.format(d);
							} else if (entityInfo.label.length() == 7) {
								Date d = short_dateFormatter7.parse(entityInfo.label);
								entityInfo.description = long_dateFormatter7.format(d);
							} else {
								entityInfo.description = "Year " + entityInfo.label;
							}
						} catch (Exception e) {
							// do nothing, in this case we simply have no description
						}
					}

					result.add(entityInfo);
					usedIDs.add(item);
				}
			}

		} catch (Exception e) {
			String message = "[" + user + "] Entity ID lookup FAILED. Input Query: " + label + " | " + number;

			addToLog(message);
			if (settings.verbose) {
				System.out.println(message);
			}
			throw(new QueryException(message,e));
		}

		return result;
	}


	/* retrieve the internal ID of a term
	 * label: input string to be matched to a term */
	public EntityInfoItem getTermByLabel(String label, String user) throws QueryException {
		EntityInfoItem result = null;

		try {
			String stemmedLabel = stemString(label.trim()).toLowerCase();

			MongoCursor<Document> cursor = colTER.find(eq(ci_Terms_label, stemmedLabel)).noCursorTimeout(true).iterator();

			if (cursor.hasNext()) {
				String description = "Stemmed form of " + label;	// add original string (unstemmed) as description
				result = new EntityInfoItem(cursor.next(), description);
			}

		} catch (Exception e) {
			String message = "[" + user + "] Term ID lookup FAILED. Input Query: " + label;

			addToLog(message);
			if (settings.verbose) {
				System.out.println(message);
			}
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
	public ArrayList<PageInfoItem> pageQuery(ArrayList<EntityQueryItem> queryEntities, int number, String user) throws QueryException {

		checkQueryLimit(queryEntities);

		long start = System.currentTimeMillis();

		ArrayList<PageInfoItem> retval = new ArrayList<PageInfoItem>();
		if (queryEntities.isEmpty()) {
			return retval;
		}

		UserToken token = userManagement.getToken(user);

		try {
			// Hashmap for counting the influence aggregation of pages based on their sentences
			HashMap<Integer, OccurrenceScoreItem> pages = new HashMap<Integer, OccurrenceScoreItem>();

			// get a ranking of sentences for the query entities
			ArrayList<OccurrenceScoreItem> sentences = rawSentenceQuery(queryEntities, token);

			// iterate over all sentences and propagate weights to pages
			int progressCounter = 0;
			double maxScore = 0;
			for (OccurrenceScoreItem osi : sentences) {

				if (progressCounter++ % abortTokenInterval == 0) {
					token.assertContinue();
				}

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
			if (maxScore <= 0) {
				maxScore = 1;
			}

			// sort the resulting pages by occurrence first, then score
			ArrayList<OccurrenceScoreItem> pageList = new ArrayList<OccurrenceScoreItem>(pages.values());
			Collections.sort(pageList);

			// go through all pages and fetch the top pages as result
			int counter = 0;
			while (counter < number && counter < pageList.size()) {

				token.assertContinue();

				OccurrenceScoreItem si = pageList.get(counter);
				int page_id = si.id;
				double score = si.occurrence-1 + si.score / maxScore;

				MongoCursor<Document> pageCursor = colPAG.find(new Document(ci_Page_id, page_id)).iterator();
				Document page = pageCursor.next();

				String pageTitle = page.getString(ci_Page_title);
				String pageURL = "";
				Integer pageWikiID = page.getInteger(ci_Page_wikiid);
				String pageArticleURL = page.getString(ci_Page_url);
				if (pageWikiID != null) {
					pageURL = wikipediaPrefixURL + pageWikiID;
				} else if (pageArticleURL != null) {
					pageURL = pageArticleURL;
				}

				PageInfoItem pag = new PageInfoItem(page_id, pageTitle, pageURL, score);
				retval.add(pag);

				pageCursor.close();
				counter++;
			}

		} catch (QueryException qe) {
			logFailedQuery(queryEntities, "PAG", user, qe.getMessage());
			throw qe;
		} catch (Exception e) {
			String message = "Error occurred while processing page query.";
			logFailedQuery(queryEntities, "PAG", user, message);
			throw(new QueryException(message,e));
		} finally {
			userManagement.returnToken(user, token);
		}

		logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, "PAG", user);

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
	public ArrayList<SentenceInfoItem> sentenceQuery(ArrayList<EntityQueryItem> queryEntities, int number, String user) throws QueryException {

		checkQueryLimit(queryEntities);

		long start = System.currentTimeMillis();

		ArrayList<SentenceInfoItem> retval = new ArrayList<SentenceInfoItem>();

		UserToken token = userManagement.getToken(user);

		try {
			// check if query is empty
			if (queryEntities.isEmpty()) {
				//System.out.println("No query entities provided. Returning empty list.");
				return retval;
			}

			// get a raw ranking of sentences
			ArrayList<OccurrenceScoreItem> sortedSentences = rawSentenceQuery(queryEntities, token);
			Collections.sort(sortedSentences);

			int index = 0;
			while (retval.size() < number && index < sortedSentences.size()) {

				token.assertContinue();

				OccurrenceScoreItem si = sortedSentences.get(index);
				int sentence_id = si.id;
				int page_id = si.page_id;
				double score = si.occurrence-1 + si.score / (queryEntities.size() * settings.sentenceTermLimit +1);

				MongoCursor<Document> sentenceCursor = colSEN.find(new Document(ci_Sentences_id, sentence_id)).iterator();
				Document sentence = sentenceCursor.next();
				String content = sentence.getString(ci_Sentences_text);
				sentenceCursor.close();

				if (content.length() <= settings.maxSentenceLength) {

					MongoCursor<Document> pageCursor = colPAG.find(new Document(ci_Page_id, page_id)).iterator();
					Document page = pageCursor.next();

					String pageURL = "";
					Integer pageWikiID = page.getInteger(ci_Page_wikiid);
					String pageArticleURL = page.getString(ci_Page_url);
					if (pageWikiID != null) {
						pageURL = wikipediaPrefixURL + pageWikiID;
					} else if (pageArticleURL != null) {
						pageURL = pageArticleURL;
					}
					pageCursor.close();

					SentenceInfoItem eOutput = new SentenceInfoItem(sentence_id, content, pageURL, score);
					retval.add(eOutput);
				}

				index++;
			}

		} catch (QueryException qe) {
			logFailedQuery(queryEntities, "SEN", user, qe.getMessage());
			throw qe;
		} catch (Exception e) {
			String message = "Error occurred while processing sentence query.";
			logFailedQuery(queryEntities, "SEN", user, message);
			throw(new QueryException(message,e));
		} finally {
			userManagement.returnToken(user, token);
		}

		logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, "SEN", user);

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
	private ArrayList<OccurrenceScoreItem> rawSentenceQuery(ArrayList<EntityQueryItem> queryEntities, UserToken token) throws QueryException {

		// check if query is empty
		if (queryEntities.isEmpty()) {
			//System.out.println("No query entities provided. Returning empty list.");
			return new ArrayList<OccurrenceScoreItem>();
		}

		// create Hashmap for counting sentences
		HashMap<Integer, OccurrenceScoreItem> sentences = new HashMap<Integer, OccurrenceScoreItem>();
		HashSet<Integer> relevantTerms = new HashSet<Integer>();

		// iterate over all query entities and find related sentences
		for (EntityQueryItem q : queryEntities) {
			int sourceID = q.node_id;
			String sourceType = q.type;

			MongoCursor<Document> edgeCursor = null;

			try {
				edgeCursor = colEdges.find(and(eq(ci_Edge_SourceType, sourceType),
						eq(ci_Edge_SourceID, sourceID),
						eq(ci_Edge_TargetType, setNames[SEN])
						)
				).noCursorTimeout(true).iterator();

				// update the neighbouring sentences' counts in the hash map
				int progressCounter = 0;
				while (edgeCursor.hasNext()) {
					if (progressCounter++ % abortTokenInterval == 0) {
						token.assertContinue();
					}

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
			} finally {
				if (edgeCursor != null) {
					edgeCursor.close();
				}
			}

			// find the best X terms for the given query entitiy and store them in a Hash set
			MongoCursor<Document> edgeTermCursor = null;

			try {
				Document order = new Document(ci_Edge_TFIDF, -1);
				edgeTermCursor = colEdges.find(and(eq(ci_Edge_SourceType, sourceType),
						eq(ci_Edge_SourceID, sourceID),
						eq(ci_Edge_TargetType, setNames[TER])
						)
				).noCursorTimeout(true).sort(order).limit(settings.sentenceTermLimit).iterator();

				int progressCounter = 0;
				while (edgeTermCursor.hasNext()) {
					if (progressCounter++ % abortTokenInterval == 0) {
						token.assertContinue();
					}

					Document termEdge = edgeTermCursor.next();
					relevantTerms.add(termEdge.getInteger(ci_Edge_TargetID));
				}

			} finally {
				if (edgeTermCursor != null) {
					edgeTermCursor.close();
				}
			}
		}

		// go over all relevant terms and slightly increase the weight of sentences if they contain the term
		for (Integer termID : relevantTerms) {

			MongoCursor<Document> termSentenceCursor = null;

			try {
				termSentenceCursor = colEdges.find(and(eq(ci_Edge_SourceType, setNames[TER]),
						eq(ci_Edge_SourceID, termID),
						eq(ci_Edge_TargetType, setNames[SEN])
						)
				).noCursorTimeout(true).iterator();

				int progressCounter = 0;
				while (termSentenceCursor.hasNext()) {
					if (progressCounter++ % abortTokenInterval == 0) {
						token.assertContinue();
					}

					Document sentenceEdge = termSentenceCursor.next();
					int sentenceID = sentenceEdge.getInteger(ci_Edge_TargetID);
					if (sentences.containsKey(sentenceID)) {
						sentences.get(sentenceID).score += 1.0;
					}
				}
			} finally {
				if (termSentenceCursor != null) {
					termSentenceCursor.close();
				}
			}
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
	public ArrayList<EntityInfoItem> entityQuery(ArrayList<EntityQueryItem> queryEntities, String targetType, int number, String user) throws QueryException {

		checkQueryLimit(queryEntities);

		long start = System.currentTimeMillis();

		// special case: query is empty
		ArrayList<EntityInfoItem> retval = new ArrayList<EntityInfoItem>();
		if (queryEntities.isEmpty()) {
			//System.out.println("No query entities provided. Returning empty list.");
			return retval;
		}

		UserToken token = userManagement.getToken(user);

		try {
			// special case: query has only one element
			if (queryEntities.size() == 1) {

				EntityQueryItem queryEntity = queryEntities.get(0);
				ArrayList<EntityInfoItem> SEQ = singleEntityQuery(queryEntity, targetType, token);

				token.assertContinue();

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

					ArrayList<EntityInfoItem> SEQ = singleEntityQuery(qe, targetType, token);

					token.assertContinue();

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

					token.assertContinue();
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
				int progressCounter = 0;
				for (Entry<EntityInfoItem,Tuple> entry : candidates.entrySet()) {
					if (progressCounter++ % abortTokenInterval == 0) {
						token.assertContinue();
					}
					EntityInfoItem e = entry.getKey();
					Tuple t = entry.getValue();
					e.score = t.count-1 + t.score / maxTFIDF;
					retval.add(e);
				}
				retval = sortAndLimitResultListSize(retval, number);
				retval = retrieveEntityInformation(retval);
			}

		} catch (QueryException qe) {
			logFailedQuery(queryEntities, targetType, user, qe.getMessage());
			throw qe;
		} catch (Exception e) {
			String message = "Error occurred while processing entity query.";
			logFailedQuery(queryEntities, targetType, user, message);
			throw(new QueryException(message,e));
		} finally {
			userManagement.returnToken(user, token);
		}

		logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, targetType, user);

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
	private ArrayList<EntityInfoItem> singleEntityQuery(EntityQueryItem queryEntity, String targetType, UserToken token) throws QueryException {

		ArrayList<EntityInfoItem> retval = new ArrayList<EntityInfoItem>();

		int sourceID = queryEntity.node_id;
		String sourceType = queryEntity.type;
		Document order = new Document(ci_Edge_TFIDF, -1);

		MongoCursor<Document> edgeCursor = null;
		try {
			// find all neighbours of the desired type
			edgeCursor = colEdges.find(and(eq(ci_Edge_SourceType, sourceType),
					eq(ci_Edge_SourceID, sourceID),
					eq(ci_Edge_TargetType, targetType)
					)
			).noCursorTimeout(true).sort(order).iterator();

			int progressCounter = 0;
			while (edgeCursor.hasNext()) {
				if (progressCounter++ % abortTokenInterval == 0) {
					token.assertContinue();
				}

				Document edge = edgeCursor.next();
				int targetID = edge.getInteger(ci_Edge_TargetID);
				double score = edge.getDouble(ci_Edge_TFIDF);
				EntityInfoItem e = new EntityInfoItem(targetID, targetType, score, null, null);
				retval.add(e);
			}
		} finally {
			if (edgeCursor != null) {
				edgeCursor.close();
			}
		}

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

				EntityInfoItem eOutput = new EntityInfoItem(entityDoc, score, eInput.isQueryEntity);

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
	public SubgraphItem subgraphQuery(ArrayList<EntityQueryItem> queryEntities, String user) throws QueryException {

		checkQueryLimit(queryEntities);

		long start = System.currentTimeMillis();

		SubgraphItem retval = new SubgraphItem();

		UserToken token = userManagement.getToken(user);

		int effectiveCohesion = Math.min(settings.cohesion, queryEntities.size());

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
			double maxScore = 0;
			for (EntityQueryItem q : queryEntities) {
				for (int type=0; type <= TER; type++) {
					String targetType = setNames[type];
					HashMap<EntityInfoItem, Tuple> candidates = neighbourCounts.get(targetType);

					ArrayList<EntityInfoItem> SEQ = singleEntityQuery(q, targetType, token);
					for (EntityInfoItem e : SEQ) {
						if (candidates.containsKey(e)) {
							Tuple t = candidates.get(e);
							t.count++;
							t.score += e.score;
							candidates.put(e,t);
						} else {
							candidates.put(e, new Tuple(1, e.score));
						}
						if (e.score > maxScore) {
							maxScore = e.score;
						}
					}
				}
			}
			if (maxScore <= 0) {
				maxScore = 1;
			}

			// remove neighbours that do not match the cohesion (occur not often enough)
			for (int type=0; type <= TER; type++) {

				token.assertContinue();

				String targetType = setNames[type];
				HashMap<EntityInfoItem, Tuple> candidates = neighbourCounts.get(targetType);
				ArrayList<EntityInfoItem> neighbourList = new ArrayList<EntityInfoItem>();

				for (Entry<EntityInfoItem,Tuple> entry : candidates.entrySet()) {
					int count = entry.getValue().count;
					if (count >= effectiveCohesion) {
						EntityInfoItem e = entry.getKey();
						e.score = count + e.score / maxScore;
						neighbourList.add(e);
					}
				}

				// limit the length of the list to the top nNeighbour entities
				neighbourList = sortAndLimitResultListSize(neighbourList, settings.graphNeighbourLimit);
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

					token.assertContinue();

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

						if (weight >= settings.subgraphEdgeWeightLimit) {
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

		} catch (QueryException qe) {
			logFailedQuery(queryEntities, "GRAPH", user, qe.getMessage());
			throw qe;
		} catch (Exception e) {
			String message = "Error occurred while processing subgraph query.";
			logFailedQuery(queryEntities, "GRAPH", user, message);
			throw(new QueryException(message,e));
		} finally {
			userManagement.returnToken(user, token);
		}

		logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, "GRAPH", user);

		return retval;
	};


}
