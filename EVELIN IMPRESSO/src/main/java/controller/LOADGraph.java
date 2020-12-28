package controller;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import impresso.SolrReader;
import org.apache.solr.client.solrj.SolrQuery;
import settings.WebInterfaceSettings;
import wikidatademo.graph.*;
import wikidatademo.logger.QueryException;
import org.bson.Document;
import org.bson.conversions.Bson;

import wikidatademo.users.UserManagement;
import wikidatademo.users.UserToken;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.VERBOSE;
import static settings.WebInterfaceStaticSettings.*;

public class LOADGraph extends LOADGraphAbstr {
    private static int maxNumberOfQueryEntities = 10000;
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

    private SolrReader sentenceReader;
    public LOADGraph(WebInterfaceSettings settings) {
        super(settings);
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.OFF);
        if (settings.useSSHtunnel) {
            try {
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                JSch jsch = new JSch();
                this.ssh = jsch.getSession(settings.LDAPUsername, settings.SSHHost, settings.SSHport);
                this.ssh.setPassword(settings.LDAPPassword);
                this.ssh.setConfig(config);
                this.ssh.connect();
                this.ssh.setPortForwardingL(8989, settings.MongoAdress, settings.MongoPort);
                this.mongoClient = new MongoClient(new ServerAddress("127.0.0.1", 27017));
            } catch (JSchException var5) {
                var5.printStackTrace();
                String msg = "Cannot make ssh connections ! " + var5.getMessage();
                System.out.println(msg);
            }
        } else {
            ServerAddress address = new ServerAddress(settings.MongoAdress, settings.MongoPort);
            if (settings.mongocred != null) {
                this.mongoClient = new MongoClient(address, Arrays.asList(settings.mongocred));
            } else {
                this.mongoClient = new MongoClient(address);
            }
        }


        this.load_db = this.mongoClient.getDatabase(target_MongoName_LOAD);
        this.colEdges = this.load_db.getCollection(MongoCollection_Edges);
        this.colSEN = this.load_db.getCollection(MongoCollection_Sentences);
        this.colPAG = this.load_db.getCollection(MongoCollection_Pages);
        this.colTER = this.load_db.getCollection(MongoCollection_Terms);
        this.colENT = this.load_db.getCollection(MongoCollection_Entities);
        this.colLOOK = this.load_db.getCollection(MongoCollection_EntityLookup);
        this.userManagement = new UserManagement(settings.maxQueryCount, settings.maxQueryDuration);

        this.addToLog(this.load_db.getName());

        Properties prop=new Properties();
        String propFilePath ="solr_config.properties";
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(propFilePath);
            prop.load(inputStream);
            inputStream.close();

        } catch (IOException e1) {
            e1.printStackTrace();
        }

        sentenceReader = new SolrReader(prop);
    }

    public void close() {
        this.mongoClient.close();
        this.closeLogger();
        if (this.ssh != null) {
            this.ssh.disconnect();
        }

    }

    private void logSuccessfulQuery(ArrayList<EntityQueryItem> queryEntities, long ms, String querytype, String user) {
        try {
            String message = "[" + user + "] Query SUCCEEDED in " + ms + "ms. Target: " + querytype + " Input Query:";

            EntityQueryItem q;
            for(Iterator var7 = queryEntities.iterator(); var7.hasNext(); message = message + " (" + q.toString() + ")") {
                q = (EntityQueryItem)var7.next();
            }

            this.addToLog(message);
            if (VERBOSE) {
                System.out.println(message);
            }
        } catch (Exception var9) {
            if (VERBOSE) {
                var9.printStackTrace();
            }
        }

    }

    private void logFailedQuery(ArrayList<EntityQueryItem> queryEntities, String querytype, String user, String reason) {
        try {
            String message = "[" + user + "] Query FAILED: " + reason + " Target: " + querytype + ". Input Query:";

            EntityQueryItem q;
            for(Iterator var6 = queryEntities.iterator(); var6.hasNext(); message = message + " (" + q.toString() + ")") {
                q = (EntityQueryItem)var6.next();
            }

            this.addToLog(message);
            if (VERBOSE) {
                System.out.println(message);
            }
        } catch (Exception var8) {
            ;
        }

    }

    private void checkQueryLimit(ArrayList<EntityQueryItem> queryEntities) throws QueryException {
        if (queryEntities.size() > maxNumberOfQueryEntities) {
            String message = "Maximum number of query entities exceeded (input: " + queryEntities.size() + ", max: " + maxNumberOfQueryEntities + ").";
            throw new QueryException(message);
        }
    }

    public ArrayList<EntityInfoItem> getEntitiesByLabel(String label, int number, String user) throws QueryException {
        ArrayList result = new ArrayList();

        try {
            HashSet<EntityIDItem> usedIDs = new HashSet();
            MongoCursor cursor = this.colLOOK.find(Filters.text(label)).projection(Projections.metaTextScore(ci_Lookup_score)).sort(Sorts.orderBy(new Bson[]{Sorts.metaTextScore(ci_Lookup_score), Sorts.descending(new String[]{ci_Lookup_senDegree})})).noCursorTimeout(true).iterator();
            while(number > 0 && cursor.hasNext()) {
                --number;
                Document doc = (Document)cursor.next();
                EntityIDItem item = new EntityIDItem(doc.getInteger(ci_Lookup_id).intValue(), doc.getString(ci_Lookup_type));
                if (!usedIDs.contains(item)) {
                    EntityInfoItem entityInfo = new EntityInfoItem(doc);
                    if (entityInfo.type.equals(dateEnt)){
                        try {
                            Date d;
                            if (entityInfo.label.length() == 10) {
                                d = short_dateFormatter10.parse(entityInfo.label);
                                entityInfo.description = long_dateFormatter10.format(d);
                            } else if (entityInfo.label.length() == 7) {
                                d = short_dateFormatter7.parse(entityInfo.label);
                                entityInfo.description = long_dateFormatter7.format(d);
                            } else {
                                entityInfo.description = "Year " + entityInfo.label;
                            }
                        } catch (Exception var11) {
                            ;
                        }
                    }
                    result.add(entityInfo);
                    usedIDs.add(item);
                }
            }

            return result;
        } catch (Exception var12) {
            String message = "[" + user + "] Entity ID lookup FAILED. Input Query: " + label + " | " + number;
            this.addToLog(message);
            if (VERBOSE) {
                System.out.println(message);
            }

            throw new QueryException(message, var12);
        }
    }

    public EntityInfoItem getTermByLabel(String label, String user) throws QueryException {
        EntityInfoItem result = null;

        try {
            String stemmedLabel = this.stemString(label.trim()).toLowerCase();
            MongoCursor<Document> cursor = this.colTER.find(Filters.eq(ci_Terms_label, stemmedLabel)).noCursorTimeout(true).iterator();
            if (cursor.hasNext()) {
                String description = "Stemmed form of " + label;
                result = new EntityInfoItem(cursor.next());
            }

            return result;
        } catch (Exception var7) {
            String message = "[" + user + "] Term ID lookup FAILED. Input Query: " + label;
            this.addToLog(message);
            if (VERBOSE) {
                System.out.println(message);
            }

            throw new QueryException(message, var7);
        }
    }

    public ArrayList<PageInfoItem> pageQuery(ArrayList<EntityQueryItem> queryEntities, int number, String user) throws QueryException {
        this.checkQueryLimit(queryEntities);
        long start = System.currentTimeMillis();
        ArrayList<PageInfoItem> retval = new ArrayList();
        if (queryEntities.isEmpty()) {
            return retval;
        } else {
            UserToken token = this.userManagement.getToken(user);

            try {
                HashMap<Integer, OccurrenceScoreItem> pages = new HashMap();
                ArrayList<OccurrenceScoreItem> sentences = this.rawSentenceQuery(queryEntities, token);
                int progressCounter = 0;
                double maxScore = 0.0D;
                Iterator var13 = sentences.iterator();

                label127:
                while(true) {
                    if (!var13.hasNext()) {
                        if (maxScore <= 0.0D) {
                            maxScore = 1.0D;
                        }

                        ArrayList<OccurrenceScoreItem> pageList = new ArrayList(pages.values());
                        Collections.sort(pageList);
                        int counter = 0;

                        while(true) {
                            if (counter >= number || counter >= pageList.size()) {
                                break label127;
                            }

                            token.assertContinue();
                            OccurrenceScoreItem si = (OccurrenceScoreItem)pageList.get(counter);
                            int page_id = si.id;
                            double score = (double)(si.occurrence - 1) + si.score / maxScore;
                            MongoCursor<Document> pageCursor = this.colPAG.find(new Document(ci_Page_id, page_id)).iterator();
                            Document page = (Document)pageCursor.next();
                            String articleId = page.getString(ci_article_id);

                            String pageTitle = sentenceReader.getArticleTitle(articleId);
                            String pageURL = "";
                            if (articleId != null)
                                pageURL = articleId;


                            PageInfoItem pag = new PageInfoItem(page_id, pageTitle, pageURL, score);
                            retval.add(pag);
                            pageCursor.close();
                            ++counter;
                        }
                    }

                    OccurrenceScoreItem osi = (OccurrenceScoreItem)var13.next();
                    if (progressCounter++ % abortTokenInterval == 0) {
                        token.assertContinue();
                    }

                    int pageID = osi.page_id;
                    if (pages.containsKey(pageID)) {
                        OccurrenceScoreItem si = (OccurrenceScoreItem)pages.get(pageID);
                        if (osi.occurrence > si.occurrence) {
                            si.occurrence = osi.occurrence;
                        }

                        si.score += osi.score;
                        if (si.score > maxScore) {
                            maxScore = si.score;
                        }
                    } else {
                        pages.put(pageID, new OccurrenceScoreItem(pageID, osi.occurrence, osi.score, pageID));
                        if (osi.score > maxScore) {
                            maxScore = osi.score;
                        }
                    }
                }
            } catch (QueryException var30) {
                this.logFailedQuery(queryEntities, page, user, var30.getMessage());
                throw var30;
            } catch (Exception var31) {
                String message = "Error occurred while processing page query.";
                this.logFailedQuery(queryEntities, page, user, message);
                throw new QueryException(message, var31);
            } finally {
                this.userManagement.returnToken(user, token);
            }

            this.logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, page, user);
            return retval;
        }
    }

    public ArrayList<SentenceInfoItem> sentenceQuery(ArrayList<EntityQueryItem> queryEntities, int number, String user) throws QueryException {
        this.checkQueryLimit(queryEntities);
        long start = System.currentTimeMillis();
        ArrayList<SentenceInfoItem> retval = new ArrayList();
        UserToken token = this.userManagement.getToken(user);

        try {
            ArrayList sortedSentences;
            if (queryEntities.isEmpty()) {
                sortedSentences = retval;
                return sortedSentences;
            }

            System.out.println();
            sortedSentences = this.rawSentenceQuery(queryEntities, token);
            Collections.sort(sortedSentences);

            for(int index = 0; retval.size() < number && index < sortedSentences.size(); ++index) {
                token.assertContinue();
                OccurrenceScoreItem si = (OccurrenceScoreItem)sortedSentences.get(index);
                int sentence_id = si.id;
                int page_id = si.page_id;
                double score = (double)(si.occurrence - 1) + si.score / (double)(queryEntities.size() * sentenceTermLimit + 1);
                MongoCursor<Document> sentenceCursor = this.colSEN.find(new Document(ci_Sentences_occurence_id, sentence_id)).iterator();
                Document sentence = (Document)sentenceCursor.next();
                String content = sentenceReader.getSentenceText(sentence.getString(ci_Sentences_article_id), sentence.getInteger(ci_min_offset), sentence.getInteger(ci_max_offset));
                sentenceCursor.close();
                int maxSentenceLength = 7;
                MongoCursor<Document> pageCursor = this.colPAG.find(new Document(ci_Page_id, page_id)).iterator();
                Document page = (Document)pageCursor.next();
                String pageURL = "";
                String articleId = page.getString(ci_article_id);
                if (articleId != null) {
                    pageURL = articleId;
                }
                pageCursor.close();
                SentenceInfoItem eOutput = new SentenceInfoItem(sentence_id, content, pageURL, score);
                retval.add(eOutput);
            }
        } catch (QueryException var28) {
            this.logFailedQuery(queryEntities, sentence, user, var28.getMessage());
            throw var28;
        } catch (Exception var29) {
            String message = "Error occurred while processing sentence query.";
            this.logFailedQuery(queryEntities, sentence, user, message);
            throw new QueryException(message, var29);
        } finally {
            this.userManagement.returnToken(user, token);
        }

        this.logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, sentence, user);
        return retval;
    }

    private ArrayList<OccurrenceScoreItem> rawSentenceQuery(ArrayList<EntityQueryItem> queryEntities, UserToken token) throws QueryException {
        if (queryEntities.isEmpty()) {
            return new ArrayList();
        } else {
            HashMap<Integer, OccurrenceScoreItem> sentences = new HashMap();
            HashSet<Integer> relevantTerms = new HashSet();
            Iterator var5 = queryEntities.iterator();

            int sentenceID;
            while(var5.hasNext()) {
                EntityQueryItem q = (EntityQueryItem)var5.next();
                int sourceID = q.node_id;
                String sourceType = q.type;
                MongoCursor edgeCursor = null;

                Document order;
                int targetID;
                try {
                    edgeCursor = this.colEdges.find(Filters.and(new Bson[]{Filters.eq(ci_Edge_SourceType, sourceType), Filters.eq(ci_Edge_SourceID, sourceID), Filters.eq(ci_Edge_TargetType, sentence)})).noCursorTimeout(true).iterator();
                    sentenceID = 0;

                    while(edgeCursor.hasNext()) {
                        if (sentenceID++ % abortTokenInterval == 0) {
                            token.assertContinue();
                        }

                        order = (Document)edgeCursor.next();
                        targetID = order.getInteger(ci_Edge_TargetID).intValue();
                        int pageID = order.getInteger(ci_Edge_PageID).intValue();
                        if (sentences.containsKey(targetID)) {
                            OccurrenceScoreItem si = (OccurrenceScoreItem)sentences.get(targetID);
                            ++si.occurrence;
                            sentences.put(targetID, si);
                        } else {
                            sentences.put(targetID, new OccurrenceScoreItem(targetID, 1, 0.0D, pageID));
                        }
                    }
                } finally {
                    if (edgeCursor != null) {
                        edgeCursor.close();
                    }

                }

                MongoCursor edgeTermCursor = null;

                try {
                    order = new Document(ci_Edge_TFIDF, Integer.valueOf(-1));
                    edgeTermCursor = this.colEdges.find(Filters.and(new Bson[]{Filters.eq(ci_Edge_SourceType, sourceType), Filters.eq(ci_Edge_SourceID, sourceID), Filters.eq(ci_Edge_TargetType, term)})).noCursorTimeout(true).sort(order).limit(sentenceTermLimit).iterator();
                    targetID = 0;

                    while(edgeTermCursor.hasNext()) {
                        if (targetID++ % abortTokenInterval == 0) {
                            token.assertContinue();
                        }

                        Document termEdge = (Document)edgeTermCursor.next();
                        relevantTerms.add(termEdge.getInteger(ci_Edge_TargetID));
                    }
                } finally {
                    if (edgeTermCursor != null) {
                        edgeTermCursor.close();
                    }

                }
            }

            var5 = relevantTerms.iterator();

            while(var5.hasNext()) {
                Integer termID = (Integer)var5.next();
                MongoCursor termSentenceCursor = null;

                try {
                    termSentenceCursor = this.colEdges.find(Filters.and(new Bson[]{Filters.eq(ci_Edge_SourceType, term), Filters.eq(ci_Edge_SourceID, termID), Filters.eq(ci_Edge_TargetType, setNames[6])})).noCursorTimeout(true).iterator();
                    int var33 = 0;

                    while(termSentenceCursor.hasNext()) {
                        if (var33++ % abortTokenInterval == 0) {
                            token.assertContinue();
                        }

                        Document sentenceEdge = (Document)termSentenceCursor.next();
                        sentenceID = sentenceEdge.getInteger(ci_Edge_TargetID).intValue();
                        if (sentences.containsKey(sentenceID)) {
                            ++((OccurrenceScoreItem)sentences.get(sentenceID)).score;
                        }
                    }
                } finally {
                    if (termSentenceCursor != null) {
                        termSentenceCursor.close();
                    }

                }
            }

            ArrayList<OccurrenceScoreItem> retval = new ArrayList(sentences.values());
            return retval;
        }
    }

    public ArrayList<EntityInfoItem> entityQuery(ArrayList<EntityQueryItem> queryEntities, String targetType, int number, String user) throws QueryException {
        this.checkQueryLimit(queryEntities);
        long start = System.currentTimeMillis();
        ArrayList<EntityInfoItem> retval = new ArrayList();
        if (queryEntities.isEmpty()) {
            return retval;
        } else {
            UserToken token = this.userManagement.getToken(user);
            try {
                if (queryEntities.size() == 1) {
                    EntityQueryItem queryEntity = queryEntities.get(0);
                    ArrayList<EntityInfoItem> SEQ = this.singleEntityQuery(queryEntity, targetType, token);
                    token.assertContinue();
                    retval = this.sortAndLimitResultListSize(SEQ, number);
                    retval = this.retrieveEntityInformation(retval);
                    this.normalizeScores(retval);
                } else {
                    HashMap<EntityInfoItem, Tuple> candidates = new HashMap();
                    double maxTFIDF = 0.0D;
                    Iterator inputEnts = queryEntities.iterator();

                    EntityQueryItem q;
                    while(inputEnts.hasNext()) {
                        q = (EntityQueryItem)inputEnts.next();
                        System.out.println("next input ent: " + q);
                        ArrayList<EntityInfoItem> linkedTargetEnt = this.singleEntityQuery(q, targetType, token);
                        System.out.println("Target entities : ");
                        for(EntityInfoItem e: linkedTargetEnt)
                            System.out.println(e);
                        token.assertContinue();
                        Iterator targetEntsIt = linkedTargetEnt.iterator();

                        while(targetEntsIt.hasNext()) {
                            EntityInfoItem targetEnt = (EntityInfoItem)targetEntsIt.next();
                            if (candidates.containsKey(targetEnt)) {
                                Tuple t = (Tuple)candidates.get(targetEnt);
                                ++t.count;
                                t.score += targetEnt.score;
                                candidates.put(targetEnt, t);
                                if (t.score > maxTFIDF) {
                                    maxTFIDF = t.score;
                                }
                            } else {
                                candidates.put(targetEnt, new Tuple(1, targetEnt.score));
                                if (targetEnt.score > maxTFIDF) {
                                    maxTFIDF = targetEnt.score;
                                }
                            }
                        }

                        token.assertContinue();
                    }

                    inputEnts = queryEntities.iterator();
                    while(inputEnts.hasNext()) {
                        q = (EntityQueryItem)inputEnts.next();
                        EntityInfoItem e = new EntityInfoItem(q.node_id, q.type, 0.0D, (String)null, (String)null);
                        candidates.remove(e);
                    }

                    int progressCounter = 0;
                    Iterator foundEntsIt = candidates.entrySet().iterator();

                    while(foundEntsIt.hasNext()) {
                        Entry<EntityInfoItem, Tuple> entry = (Entry)foundEntsIt.next();
                        if (progressCounter++ % abortTokenInterval == 0) {
                            token.assertContinue();
                        }

                        EntityInfoItem e = (EntityInfoItem)entry.getKey();
                        Tuple t = (Tuple)entry.getValue();
                        e.score = (double)(t.count - 1) + t.score / maxTFIDF;
                        retval.add(e);
                    }

                    retval = this.sortAndLimitResultListSize(retval, number);
                    retval = this.retrieveEntityInformation(retval);
                    System.out.println("Entity info retrieved");
                }
            } catch (QueryException var22) {
                this.logFailedQuery(queryEntities, targetType, user, var22.getMessage());
                throw var22;
            } catch (Exception var23) {
                String message = "Error occurred while processing entity query.";
                this.logFailedQuery(queryEntities, targetType, user, message);
                throw new QueryException(message, var23);
            } finally {
                this.userManagement.returnToken(user, token);
            }

            this.logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, targetType, user);
            return retval;
        }
    }

    private ArrayList<EntityInfoItem> singleEntityQuery(EntityQueryItem queryEntity, String targetType, UserToken token) throws QueryException {
        ArrayList<EntityInfoItem> retval = new ArrayList();
        int sourceID = queryEntity.node_id;
        String sourceType = queryEntity.type;
        Document order = new Document(ci_Edge_TFIDF, Integer.valueOf(-1));
        MongoCursor edgeCursor = null;

        try {
            edgeCursor = this.colEdges.find(Filters.and(new Bson[]{Filters.eq(ci_Edge_SourceType, sourceType),
                    Filters.eq(ci_Edge_SourceID, sourceID),
                    Filters.eq(ci_Edge_TargetType, targetType)})).noCursorTimeout(true).sort(order).iterator();
            int var9 = 0;

            while(edgeCursor.hasNext()) {
                if (var9++ % abortTokenInterval == 0) {
                    token.assertContinue();
                }

                Document edge = (Document)edgeCursor.next();
                int targetID = edge.getInteger(ci_Edge_TargetID).intValue();
                double score = edge.getDouble(ci_Edge_TFIDF).doubleValue();
                EntityInfoItem e = new EntityInfoItem(targetID, targetType, score, (String)null, (String)null);
                retval.add(e);
            }
        } finally {
            if (edgeCursor != null) {
                edgeCursor.close();
            }

        }

        return retval;
    }

    private ArrayList<EntityInfoItem> retrieveEntityInformation(ArrayList<EntityInfoItem> entityList) {
        ArrayList<EntityInfoItem> retval = new ArrayList();
        Iterator entityIt = entityList.iterator();

        while(entityIt.hasNext()) {
            EntityInfoItem eInput = (EntityInfoItem)entityIt.next();
            int entityID = eInput.node_id;
            String type = eInput.type;
            double score = eInput.score;
            MongoCursor entityCursor;
            Document termDoc;
            if (type.equals(term)) {
                entityCursor = this.colTER.find(new Document(ci_Terms_id, entityID)).iterator();
                termDoc = (Document)entityCursor.next();
                String content = termDoc.getString(ci_Terms_label);
                EntityInfoItem eOutput = new EntityInfoItem(entityID, type, score, content, (String)null);
                eOutput.isQueryEntity = eInput.isQueryEntity;
                retval.add(eOutput);
                entityCursor.close();
            } else {
                entityCursor = this.colENT.find(Filters.and(new Bson[]{Filters.eq(ci_Entity_type, type), Filters.eq(ci_Entity_node_id, entityID)})).iterator();
                termDoc = (Document)entityCursor.next();
                EntityInfoItem eOutput = new EntityInfoItem(termDoc, score, eInput.isQueryEntity);
                if (type.equals(dateEnt)) {
                    try {
                        Date d;
                        if (eOutput.label.length() == 10) {
                            d = short_dateFormatter10.parse(eOutput.label);
                            eOutput.description = long_dateFormatter10.format(d);
                        } else if (eOutput.label.length() == 7) {
                            d = short_dateFormatter7.parse(eOutput.label);
                            eOutput.description = long_dateFormatter7.format(d);
                        } else {
                            eOutput.description = "Year " + eOutput.label;
                        }
                    } catch (Exception var13) {
                        ;
                    }
                }

                retval.add(eOutput);
                entityCursor.close();
            }
        }

        return retval;
    }

    private void normalizeScores(ArrayList<EntityInfoItem> entityList) {
        if (!entityList.isEmpty()) {
            double max = ((EntityInfoItem)entityList.get(0)).score;

            EntityInfoItem e;
            for(Iterator var4 = entityList.iterator(); var4.hasNext(); e.score /= max) {
                e = (EntityInfoItem)var4.next();
            }
        }

    }

    private ArrayList<EntityInfoItem> sortAndLimitResultListSize(ArrayList<EntityInfoItem> entityList, int number) {
        if (entityList.isEmpty()) {
            return entityList;
        } else {
            Collections.sort(entityList);
            ArrayList<EntityInfoItem> retval = new ArrayList();

            for(int counter = 0; counter < number && counter < entityList.size(); ++counter) {
                retval.add(entityList.get(counter));
            }

            return retval;
        }
    }

    public SubgraphItem subgraphQuery(ArrayList<EntityQueryItem> queryEntities, String user) throws QueryException {
        this.checkQueryLimit(queryEntities);
        long start = System.currentTimeMillis();
        SubgraphItem retval = new SubgraphItem();
        UserToken token = this.userManagement.getToken(user);
        int effectiveCohesion = Math.min(cohesion, queryEntities.size());

        try {
            ArrayList<EntityInfoItem> nodes = new ArrayList();
            HashSet<EdgeItem> edges = new HashSet();
            Iterator var10 = queryEntities.iterator();

            while(var10.hasNext()) {
                EntityQueryItem q = (EntityQueryItem)var10.next();
                EntityInfoItem e = new EntityInfoItem(q.node_id, q.type, 0.0D, (String)null, (String)null);
                e.isQueryEntity = true;
                nodes.add(e);
            }

            HashMap<String, HashMap<EntityInfoItem, Tuple>> neighbourCounts = new HashMap();

            for(int type = 0; type <= 4; ++type) {
                neighbourCounts.put(setNames[type], new HashMap());
            }

            double maxScore = 0.0D;
            Iterator var13 = queryEntities.iterator();

            int i;
            EntityInfoItem e;
            while(var13.hasNext()) {
                EntityQueryItem q = (EntityQueryItem)var13.next();

                for(i = 0; i <= 4; ++i) {
                    String targetType = setNames[i];
                    HashMap<EntityInfoItem, Tuple> candidates = (HashMap)neighbourCounts.get(targetType);
                    ArrayList<EntityInfoItem> SEQ = this.singleEntityQuery(q, targetType, token);
                    Iterator var19 = SEQ.iterator();

                    while(var19.hasNext()) {
                        e = (EntityInfoItem)var19.next();
                        if (candidates.containsKey(e)) {
                            Tuple t = (Tuple)candidates.get(e);
                            ++t.count;
                            t.score += e.score;
                            candidates.put(e, t);
                        } else {
                            candidates.put(e, new Tuple(1, e.score));
                        }

                        if (e.score > maxScore) {
                            maxScore = e.score;
                        }
                    }
                }
            }

            if (maxScore <= 0.0D) {
                maxScore = 1.0D;
            }

            for(int type = 0; type <= 4; ++type) {
                token.assertContinue();
                String targetType = setNames[type];
                HashMap<EntityInfoItem, Tuple> candidates = (HashMap)neighbourCounts.get(targetType);
                ArrayList<EntityInfoItem> neighbourList = new ArrayList();
                Iterator var48 = candidates.entrySet().iterator();

                while(var48.hasNext()) {
                    Entry<EntityInfoItem, Tuple> entry = (Entry)var48.next();
                    int count = ((Tuple)entry.getValue()).count;
                    if (count >= effectiveCohesion) {
                        e = (EntityInfoItem)entry.getKey();
                        e.score = (double)count + e.score / maxScore;
                        neighbourList.add(e);
                    }
                }

                neighbourList = this.sortAndLimitResultListSize(neighbourList, graphNeighbourLimit);
                var48 = neighbourList.iterator();

                while(var48.hasNext()) {
                    e = (EntityInfoItem) var48.next();
                    nodes.add(e);
                }
            }

            double maxEdgeWeight = 0.0D;

            for(i = 0; i < nodes.size(); ++i) {
                EntityInfoItem source = (EntityInfoItem)nodes.get(i);
                int sourceID = source.node_id;
                String sourceType = source.type;
                String sourceStringID = sourceType + sourceID;

                for(int j = i + 1; j < nodes.size(); ++j) {
                    token.assertContinue();
                    EntityInfoItem target = (EntityInfoItem)nodes.get(j);
                    int targetID = target.node_id;
                    String targetType = target.type;
                    MongoCursor<Document> edgeCursor = this.colEdges.find(Filters.and(new Bson[]{Filters.eq(ci_Edge_SourceType, sourceType), Filters.eq(ci_Edge_SourceID, sourceID), Filters.eq(ci_Edge_TargetType, targetType), Filters.eq(ci_Edge_TargetID, targetID)})).noCursorTimeout(true).iterator();
                    if (edgeCursor.hasNext()) {
                        Document edge = (Document)edgeCursor.next();
                        double weight = edge.getDouble(ci_Edge_Weight).doubleValue();
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

            retval.edges.addAll(edges);

            EdgeItem e1;
            for(Iterator var46 = edges.iterator(); var46.hasNext(); e1.weight /= maxEdgeWeight) {
                e1 = (EdgeItem) var46.next();
            }

            retval.nodes = this.retrieveEntityInformation(new ArrayList(nodes));
        } catch (QueryException var34) {
            this.logFailedQuery(queryEntities, "GRAPH", user, var34.getMessage());
            throw var34;
        } catch (Exception var35) {
            var35.
                    printStackTrace();
            String message = "Error occurred while processing subgraph query.";
            this.logFailedQuery(queryEntities, "GRAPH", user, message);
            throw new QueryException(message, var35);
        } finally {
            this.userManagement.returnToken(user, token);
        }

        this.logSuccessfulQuery(queryEntities, System.currentTimeMillis() - start, "GRAPH", user);
        return retval;
    }
}
