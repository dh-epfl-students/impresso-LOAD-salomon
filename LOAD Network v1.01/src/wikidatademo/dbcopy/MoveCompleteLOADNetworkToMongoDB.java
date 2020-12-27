package wikidatademo.dbcopy;

import static settings.LOADmodelSettings.*;
import static settings.LOADmodelSettings.ACT;
import static settings.LOADmodelSettings.DAT;
import static settings.LOADmodelSettings.LOC;
import static settings.LOADmodelSettings.ORG;
import static settings.LOADmodelSettings.PAG;
import static settings.LOADmodelSettings.SEN;
import static settings.LOADmodelSettings.TER;
import static settings.LOADmodelSettings.nANNOTATIONS;
import static settings.LOADmodelSettings.setNames;
import static settings.SystemSettings.PROP_PATH;
import static settings.SystemSettings.VERBOSE;
import static settings.WebInterfaceStaticSettings.*;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// mongoDB imports
import impresso.SolrReader;
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

/**
 * Reads a LOAD graph from the internally use file structure and stores it in a mongoDB.
 * USED FOR LOAD WIKIPEDIA ENTITY SEARCH INTERFACE ON METIS: Evelin
 * 
 * Originally published
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class MoveCompleteLOADNetworkToMongoDB {
//	// TARGET mongoDB login settings
	public static boolean LOCAL_TARGET = true;
	public static String target_MongoAdress = "127.0.0.1";//"metis.ifi.uni-heidelberg.de";//"<mongo server adress>";
	public static int target_MongoPort = 27017;
	public static String target_password = "password";
	public static String target_username = "username";
	public static String target_auth_db = "name of authentication DB";
	//public static MongoCredential mongocred = MongoCredential.createCredential(username, auth_db, password.toCharArray());
	public static MongoCredential target_mongocred = null;
	public static int bulkSize = 1000;


	
	
	// read the number of nodes for each set from the meta data file
	public static int[] getSetSizes() {
		
		HashMap<String,Integer> setNamesToIndexMap = new HashMap<String,Integer>();
		for (int i=0; i < nANNOTATIONS; i++) {
			setNamesToIndexMap.put(setNames[i], i);
		}
		int[] nNodes = new int[nANNOTATIONS];
		
		try {			
			// read meta data from file to obtain number of vertices of different types
			String line;
			String filename = outfolder + metaFileName;
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) continue;
				
				String[] splitline = line.split(sepChar); 
				Integer set = setNamesToIndexMap.get(splitline[0]);
				if (set != null) {
					nNodes[set] = Integer.parseInt(splitline[1]);
				} else {
					System.out.println("Unknown line specificer in metadata file:");
					System.out.println(line);
				}
			}
			br.close();
			
			// sanity check: are any vertex sets of size 0?
			for (int i=0; i < nANNOTATIONS; i++) {
				if (nNodes[i] == 0) {
					System.out.println("Warning! Number of nodes is 0 for " + setNames[i]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nNodes;
	}
	
	// write metadata information to mongoDB
	public static void readMetaData(MongoCollection<Document> cMeta) {
		
		System.out.println("Writing meta data");
		
		try {
			// read meta data from file to obtain number of vertices of different types
			String line;
			String filename = outfolder + metaFileName;
			System.out.println("Reading graph meta data");
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			
			Document doc = new Document().append("_id","Meta data information");
			//doc.append("source", metaSource);
			ArrayList<String> typelabels = new ArrayList<String>();
			for (int i=0; i < nANNOTATIONS; i++) {
				typelabels.add(setNames[i]);
			}
			doc.append("nodeTypes", typelabels);
			
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) continue;
				
				String[] splitline = line.split(sepChar); 
				String set = splitline[0];
				Integer count = Integer.parseInt(splitline[1]);
				
				doc.append(set,count);
			}
			br.close();
			cMeta.insertOne(doc);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	// write node degrees to mongoDB
	public static void readNodesAndWriteNodeDegrees(MongoCollection<Document> cOUT_NodeDegs) {
			
		System.out.println("Writing node degrees");
		try {
            // read vertex information
			for (int i=0; i < nANNOTATIONS; i++) {
				String filename = outfolder + vertexFileNames[i];
				String nodeType = setNames[i];
				System.out.print(" Reading nodes of type " + setNames[i]);
				int index = 0;
				String line;
				ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);

				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
				while ((line = br.readLine()) != null) {
					
					String[] splitline = line.split(sepChar);
					
					Document node = new Document().append(ci_NodeDegrees_id,index)
							                      .append(ci_NodeDegrees_type, nodeType);
					
					ArrayList<Integer> degrees = new ArrayList<Integer>();
					for (int k=0; k < nANNOTATIONS; k++) {
						degrees.add(Integer.parseInt(splitline[k+1]));
					}
										
					node.append(ci_NodeDegrees_degrees, degrees);
					
                    index++;
                    insertList.add(node);
                    if (insertList.size() == bulkSize) {
                    	cOUT_NodeDegs.insertMany(insertList);
                    	insertList.clear();
                    	System.out.print("\r Reading nodes of type " + setNames[i] + ": " + index + " done");
                    }
				}
				if (!insertList.isEmpty()) {
					cOUT_NodeDegs.insertMany(insertList);
					insertList.clear();
				}
				br.close();
				
				System.out.println("\r Reading nodes of type " + setNames[i] + ": finished                           ");
			}
			
		} catch (Exception e) {
			System.out.println("Problem while transfering node degrees.");
			e.printStackTrace();
		}
	}
	//TODO
	/*private static String sentenceTextRetriever(String article, int min_offset, int max_offset){
		return "";
	}*/

	// write sentence information to mongoDB
	public static void readNodesAndWriteSentences(MongoCollection<Document> cOUT_Sentences) {
			
		System.out.println("Writing sentence information");
		try {
            // read sentence vertex information
			String filename = outfolder + vertexFileNames[SEN];
			System.out.print(" Reading nodes of type SEN");
			int index = 0;
			String line;
			ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			while ((line = br.readLine()) != null) {
				String[] splitline = line.split(sepChar);
				//Sentence id structure: PAPERACR-YEAR-MONTH-DAY-"a"-ARTICLE_ID-MIN_OFFSET-MAX_OFFSET 
				//WARNING: MAX_OFFSET excluded!
				String[] sentenceId = splitline[0].split(";");
				int min_offset = Integer.parseInt(sentenceId[1]);
				int max_offset = Integer.parseInt(sentenceId[2]);
				String article_id = sentenceId[0];

				Document node = new Document().append(ci_Sentences_occurence_id, index)
						.append(ci_Sentences_article_id, article_id)
						.append(ci_min_offset, min_offset)
						.append(ci_max_offset, max_offset);
				//node.append(ci_Sentences_text, sentenceTextRetriever(article_id, min_offset, max_offset));
                index++;
                insertList.add(node);
                if (insertList.size() == bulkSize) {
                	cOUT_Sentences.insertMany(insertList);
                  	insertList.clear();
                  	System.out.print("\r Reading nodes of type SEN: " + index + " done");
                }
			}
			if (!insertList.isEmpty()) {
				cOUT_Sentences.insertMany(insertList);
				insertList.clear();
			}
			br.close();
			System.out.println("\r Reading nodes of type SEN: finished                         ");
		} catch (Exception e) {
			System.out.println("Problem while transfering sentences.");
			e.printStackTrace();
		}
	}

	// write page information to mongoDB
	public static void readNodesAndWritePages(MongoCollection<Document> cOUT_Pages) {
			
		System.out.println("Writing page information");
		try {
            // read page vertex information
			String filename = outfolder + vertexFileNames[PAG];
			System.out.print(" Reading nodes of type PAG");
			int index = 0;
			String line;
			ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			while ((line = br.readLine()) != null) {
				
				String[] splitline = line.split(sepChar);
				String articleId = splitline[0];

				Document node = new Document().append(ci_Page_id,index)
						                      .append(ci_article_id, articleId);
				
                index++;
                insertList.add(node);
                if (insertList.size() == bulkSize) {
                	cOUT_Pages.insertMany(insertList);
                  	insertList.clear();
                  	System.out.print("\r Reading nodes of type PAG: " + index + " done");
                }
			}
			if (!insertList.isEmpty()) {
				cOUT_Pages.insertMany(insertList);
				insertList.clear();
			}
			br.close();
			System.out.println("\r Reading nodes of type PAG: finished                    ");
		} catch (Exception e) {
			System.out.println("Problem while transfering pages.");
			e.printStackTrace();
		}
	}
	
	// write term information to mongoDB
	public static void readNodesAndWriteTerms(MongoCollection<Document> cOUT_Terms) {
			
		System.out.println("Writing term information");
		try {
            // read page vertex information
			String filename = outfolder + vertexFileNames[TER];
			System.out.print(" Reading nodes of type TER");
			int index = 0;
			String line;
			ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			while ((line = br.readLine()) != null) {
				
				String[] splitline = line.split(sepChar);
				String term = splitline[0];
				int sendeg = Integer.parseInt(splitline[SEN+1]);
				
				Document node = new Document().append(ci_Terms_id,index)
						                      .append(ci_Terms_label, term)
						                      .append(ci_Terms_senDegree, sendeg);
				
                index++;
                insertList.add(node);
                if (insertList.size() == bulkSize) {
                	cOUT_Terms.insertMany(insertList);
                  	insertList.clear();
                  	System.out.print("\r Reading nodes of type TER: " + index + " done");
                }
			}
			if (!insertList.isEmpty()) {
				cOUT_Terms.insertMany(insertList);
				insertList.clear();
			}
			br.close();
			System.out.println("\r Reading nodes of type TER: finished                    ");
		} catch (Exception e) {
			System.out.println("Problem while transfering terms.");
			e.printStackTrace();
		}
	}
	
	// write page information to mongoDB
	private static void getEntityInfo(Document node, String entityId){
		//Loads the property file

	}
	
	public static void readNodesAndWriteEntities(MongoCollection<Document> cOUT_Entities, MongoCollection<Document> cOUT_Lookup) {
		System.out.println("Writing entity information");
		try {
			// read vertex information for dates
			String filename = outfolder + vertexFileNames[DAT];
			System.out.print(" Reading nodes of type DAT");
			int index = 0;
			String line;
			ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);
			String nodeType = setNames[DAT];
			
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			Properties prop=new Properties();
			String propFilePath =PROP_PATH;

			FileInputStream inputStream;
			try {
				inputStream = new FileInputStream(propFilePath);
				prop.load(inputStream);
				inputStream.close();

			} catch (IOException e1) {
				e1.printStackTrace();
			}
			SolrReader entityReader = new SolrReader(prop);
			while ((line = br.readLine()) != null) {
			
				String[] splitline = line.split(sepChar);
				String date_str = splitline[0];
				int sendeg = Integer.parseInt(splitline[SEN+1]);
				
				Document node = new Document().append(ci_Entity_node_id,index)
						                      .append(ci_Entity_type, nodeType)
											  .append(ci_Entity_senDegree, sendeg);
				
				node.append(ci_Entity_label, date_str);
				
				index++;
            	insertList.add(node);
            	if (insertList.size() == bulkSize) {
            		cOUT_Entities.insertMany(insertList);
					cOUT_Lookup.insertMany(insertList);
					insertList.clear();
            		System.out.print("\r Reading nodes of type DAT: " + index + " done");
            	}
			}
			if (!insertList.isEmpty()) {
				cOUT_Entities.insertMany(insertList);
				cOUT_Lookup.insertMany(insertList);
				insertList.clear();
			}
			br.close();
			System.out.println("\r Reading nodes of type DAT: finished              ");

			
            // read vertex information for non-date entities
			for (int i=1; i <= ORG; i++) {
				filename = outfolder + vertexFileNames[i];
				System.out.println(" Reading nodes of type " + setNames[i]);
				index = 0;
				insertList = new ArrayList<Document>(bulkSize);
				ArrayList<Document> insertListLookup = new ArrayList<Document>(bulkSize);
				nodeType = setNames[i];
				
				br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
				while ((line = br.readLine()) != null) {
				
					String[] splitline = line.split(sepChar);
					String entityId = splitline[0];
					int sendeg = Integer.parseInt(splitline[SEN+1]);
					
					Document node = new Document().append(ci_Entity_node_id,index)
							                      .append(ci_Entity_type, nodeType)
												  .append(ci_Entity_id, entityId)
												  .append(ci_Entity_senDegree, sendeg);
					try{
						String[] info = entityReader.getEntityInfo(entityId).split(idInfoSepChar);
						String label = info[1];
						node.append(ci_Entity_description, info[0]);
						node.append(ci_Entity_label, label);

						insertList.add(node);

						// add multiple possible entity labels to the lookup table
						HashSet<String> cleanlabels = new HashSet<String>();
						cleanlabels.add(label);
						String cleanLabel = label.replaceAll("\\p{Punct}+", "").trim();
						cleanlabels.add(cleanLabel);
						String[] cleanlabelparts = cleanLabel.split(" ");
						for (String s : cleanlabelparts) {
							s = s.trim();
							if (s.length() >= minLabelComponentLength) {
								cleanlabels.add(s);
							}
						}

						for (String l : cleanlabels) {
							Document looknode = new Document().append(ci_Entity_id,index)
									.append(ci_Entity_type, nodeType)
									.append(ci_Entity_senDegree, sendeg)
									.append(ci_Entity_label, l)
									.append(ci_Entity_fullLabel, label);
							insertListLookup.add(looknode);
						}
						if (insertListLookup.size() >= bulkSize) {
							cOUT_Lookup.insertMany(insertListLookup);
							insertListLookup.clear();
						}
						index++;
					}catch (Exception e){
						if(VERBOSE)
							System.out.println("Problem: no entity info available for line:" + line);
						System.out.println(e);
					}

					if (insertList.size() == bulkSize) {
                		cOUT_Entities.insertMany(insertList);
                		insertList.clear();
                		System.out.print("\r Reading nodes of type " + setNames[i] + ": " + index + " done");
                	}

				}
				if (!insertList.isEmpty()) {
					cOUT_Entities.insertMany(insertList);
					insertList.clear();
				}
				if (!insertListLookup.isEmpty()) {
					cOUT_Lookup.insertMany(insertListLookup);
					insertListLookup.clear();
				}
				br.close();
				System.out.println("\r Reading nodes of type " + setNames[i] + ": finished              ");
			}
		} catch (Exception e) {
			System.out.println("Problem while transfering entities.");
			e.printStackTrace();
		}
	}
	
	// write edges to mongoDB
	public static void readEdgesAndWriteEdgeList(MongoCollection<Document> cEdges) {
		
		int[] nNodes = getSetSizes();
		
		try {
            // read node information
		
			for (int i=0; i < nANNOTATIONS; i++) {
				String filename = outfolder + edgeFileNames[i];
				System.out.print("Reading edges of type " + setNames[i]);
				int index = 0;
				int steps = nNodes[i]/100;
				if (steps == 0) steps = 1;
				
				@SuppressWarnings("unused")
				String line;
				
				ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);
				
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
				do {
					if (index % steps == 0) {
						int progress = (int) (100.0*index/nNodes[i]);
						System.out.print("\rReading edges of type " + setNames[i] + ":" + String.format("%1$" + 3 + "s", progress) +"%");
					}
					line = br.readLine();

					for (int k=0; k < nANNOTATIONS; k++) {
						String[] splitline = br.readLine().split(sepChar);
						
						String sourceType = setNames[i];
						String targetType = setNames[k];
						
						for (int lineIndex=1; lineIndex<splitline.length; ) {
							int targetID = Integer.parseInt(splitline[lineIndex++]);
							double weight = Float.parseFloat(splitline[lineIndex++]);
							int sourceID = index;
							
							Document edge = new Document().append(ci_Edge_SourceType, sourceType)
									  					  .append(ci_Edge_TargetType, targetType)
									  					  .append(ci_Edge_SourceID, sourceID)
									  					  .append(ci_Edge_TargetID, targetID)
									  					  .append(ci_Edge_Weight, weight);
							
							insertList.add(edge);
		                    if (insertList.size() == bulkSize) {
		                    	cEdges.insertMany(insertList);
		                    	insertList.clear();
		                    }
						}
					}
					++index;
				} while ((line = br.readLine()) != null);
				
				if (!insertList.isEmpty()) {
					cEdges.insertMany(insertList);
					insertList.clear();
				}
				
				System.out.println("\rReading edges of type " + setNames[i] + ": done");
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// write edges with TFIDF and page information (for sentence edges) to mongoDB
	public static void readEdgesAndWriteEdgeListWithTFIDF(MongoCollection<Document> cEdges) {
		
		int[] nNodes = getSetSizes();
		GraphNode[][] nodeLists = new GraphNode[nANNOTATIONS][];;
		String line;
		
		try {
			// read degree information
			System.out.println("Preparing to write edges");
			for (int i=0; i < nANNOTATIONS; i++) {
				String filename = outfolder + vertexFileNames[i];
				System.out.print(" Reading node degrees of type " + setNames[i]);
				int index = 0;
				int steps = nNodes[i]/100;
				if (steps == 0) steps = 1;
				nodeLists[i] = new GraphNode[nNodes[i]];
				GraphNode[] nodes = nodeLists[i];
				BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
				while ((line = br.readLine()) != null) {
					if (index % steps == 0) {
						int progress = (int) (100.0*index/nNodes[i]);
						System.out.print("\r Reading node degrees of type " + setNames[i] + ":" + String.format("%1$" + 3 + "s", progress) +"%");
					}
					String[] splitline = line.split(sepChar);
					String value = splitline[0];
					int[] degs = new int[nANNOTATIONS];
					degs[DAT] = Integer.parseInt(splitline[1]);
					degs[LOC] = Integer.parseInt(splitline[2]);
					degs[ACT] = Integer.parseInt(splitline[3]);
					degs[ORG] = Integer.parseInt(splitline[4]);
					degs[TER] = Integer.parseInt(splitline[5]);
					degs[PAG] = Integer.parseInt(splitline[6]);
					degs[SEN] = Integer.parseInt(splitline[7]);
					try {
						nodes[index] = new GraphNode(i, index, value, degs);
					} catch (ArrayIndexOutOfBoundsException e){
						System.out.println("BAD");
					}
					++index;
				}
				System.out.println("\r Reading node degrees of type " + setNames[i] + ": done");
				br.close();
			}
			
			// read sentence to page edges and store them internally
			// to later be able to add the page to every sentence edge
			HashMap<Integer,Integer> sentenceToPageMap = new HashMap<Integer,Integer>();
			for (int i=SEN; i == SEN; i++) {
				String filename = outfolder + edgeFileNames[i];
				System.out.print(" Reading sentence to page edges ");
				int index = 0;
				int steps = nNodes[i]/100;
				if (steps == 0) steps = 1;
				BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
				do {
					if (index % steps == 0) {
						int progress = (int) (100.0*index/nNodes[i]);
						System.out.print("\r Reading sentence to page edges:" + String.format("%1$" + 3 + "s", progress) +"%");
					}
					line = br.readLine();
					for (int k=0; k < nANNOTATIONS; k++) {
						
						if (k == PAG) {
							String[] splitline = br.readLine().split(sepChar);
							for (int lineIndex=1; lineIndex<splitline.length; ) {
								int pageID = Integer.parseInt(splitline[lineIndex]);
								sentenceToPageMap.put(index, pageID);
								lineIndex += 2;
							}
						} else {
							br.readLine();
						}
					}
					++index;
				} while ((line = br.readLine()) != null);
				System.out.println("\r Reading sentence to page edges: done");
				br.close();
			}
		
            // read edge information
			for (int sourceType_id=0; sourceType_id < nANNOTATIONS; sourceType_id++) {
				String filename = outfolder + edgeFileNames[sourceType_id];
				System.out.print("Reading edges of type " + setNames[sourceType_id]);
				int index = 0;
				int steps = nNodes[sourceType_id]/100;
				if (steps == 0) steps = 1;
				
				ArrayList<Document> insertList = new ArrayList<Document>(bulkSize);
				
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
				do {
					if (index % steps == 0) {
						int progress = (int) (100.0*index/nNodes[sourceType_id]);
						System.out.print("\rReading edges of type " + setNames[sourceType_id] + ":" + String.format("%1$" + 3 + "s", progress) +"%");
					}
					line = br.readLine();

					for (int targetType_id=0; targetType_id < nANNOTATIONS; targetType_id++) {
						String[] splitline = br.readLine().split(sepChar);
						
						String sourceType = setNames[sourceType_id];
						String targetType = setNames[targetType_id];
						
						for (int lineIndex=1; lineIndex<splitline.length; ) {
							int targetID = Integer.parseInt(splitline[lineIndex++]);
							double weight = Float.parseFloat(splitline[lineIndex++]);
							int sourceID = index;
							
							int degree = nodeLists[targetType_id][targetID].degrees[sourceType_id]; // degree of target with respect to source type
							int setSize = nNodes[sourceType_id];
							
							// compute graph tf-idf value
							double idf = setSize / (double) degree;
							double tfidf = weight * Math.log(idf);
							
							Document edge = new Document().append(ci_Edge_SourceType, sourceType)
									  					  .append(ci_Edge_TargetType, targetType)
									  					  .append(ci_Edge_SourceID, sourceID)
									  					  .append(ci_Edge_TargetID, targetID)
									  					  .append(ci_Edge_Weight, weight)
									  					  .append(ci_Edge_TFIDF, tfidf);
							
							// if the target of an edge is a sentence, also include the ID of the page the sentence belongs to
							if (targetType_id == SEN) {
								int pageID = sentenceToPageMap.get(targetID);
								edge.append(ci_Edge_PageID, pageID);
							}
							
							insertList.add(edge);
		                    if (insertList.size() == bulkSize) {
		                    	cEdges.insertMany(insertList);
		                    	insertList.clear();
		                    }
						}
					}
					++index;
				} while ((line = br.readLine()) != null);
				
				if (!insertList.isEmpty()) {
					cEdges.insertMany(insertList);
					insertList.clear();
				}
				
				System.out.println("\rReading edges of type " + setNames[sourceType_id] + ": done");
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args){
		loadGraphToDB();
	}
	public static void loadGraphToDB() {
		
		Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(Level.WARNING);


		ServerAddress target_address = new ServerAddress(target_MongoAdress, target_MongoPort);
		MongoClient target_mongoClient;
		if (target_mongocred != null) {
			target_mongoClient = new MongoClient(target_address, Arrays.asList(target_mongocred));
		} else {
			target_mongoClient = new MongoClient(target_address);
		}
		
		// Wikipedia input collections
		MongoCollection<Document> cSentences;
		MongoCollection<Document> cArticles;


		// LOAD output collections
		MongoDatabase load_db = target_mongoClient.getDatabase(target_MongoName_LOAD);
		MongoCollection<Document> cOUT_Meta = load_db.getCollection(target_MongoCollection_Meta);
		MongoCollection<Document> cOUT_Edges = load_db.getCollection(target_MongoCollection_Edges);
		MongoCollection<Document> cOUT_NodeDegs = load_db.getCollection(target_MongoCollection_NodeDegrees);
		MongoCollection<Document> cOUT_SEN = load_db.getCollection(target_MongoCollection_Sentences);
		MongoCollection<Document> cOUT_PAG = load_db.getCollection(target_MongoCollection_Pages);
		MongoCollection<Document> cOUT_TER = load_db.getCollection(target_MongoCollection_Terms);
		MongoCollection<Document> cOUT_ENT = load_db.getCollection(target_MongoCollection_Entities);
		MongoCollection<Document> cOUT_LOOK = load_db.getCollection(target_MongoCollection_EntityLookup);


		// remove existing collections
		if(VERBOSE)
			System.out.println("Delete existing DB");
		cOUT_Meta.drop();
		cOUT_Edges.drop();
		cOUT_NodeDegs.drop();
		cOUT_SEN.drop();
		cOUT_PAG.drop();
		cOUT_TER.drop();
		cOUT_ENT.drop();
		
		// read meta data from file and write to MongoDB
		if(VERBOSE)
			System.out.println("Writing metadata");
		readMetaData(cOUT_Meta);

		// read nodes from file and write to MongoDB
		if(VERBOSE)
			System.out.println("Writing nodes");
		readNodesAndWriteNodeDegrees(cOUT_NodeDegs);
		
		// read sentences from file and write to MongoDB
		if(VERBOSE)
			System.out.println("Writing sentences");
		 readNodesAndWriteSentences(cOUT_SEN);

		// read pages from file and write to MongoDB
		if(VERBOSE)
			System.out.println("Writing pages");
		readNodesAndWritePages(cOUT_PAG);

		// read terms from file and write to MongoDB
		if(VERBOSE)
			System.out.println("Writing terms");
		readNodesAndWriteTerms(cOUT_TER);
		
		// read entities from file and write to MongoDB
		if(VERBOSE)
			System.out.println("Writing entities");
		readNodesAndWriteEntities(cOUT_ENT, cOUT_LOOK);
		
		// read edges from file and write to MongoDB
		//readEdgesAndWriteEdgeList(cOUT_Edges);
		//alternatively
		readEdgesAndWriteEdgeListWithTFIDF(cOUT_Edges);
//
		if(VERBOSE)
			System.out.println("\nAll data transfered!\n");

		if(VERBOSE)
			System.out.println("Creating indexes for Edges");
		cOUT_Edges.createIndex(new Document(ci_Edge_SourceType, 1).append(ci_Edge_SourceID, 1).append(ci_Edge_TargetType, 1));
		cOUT_Edges.createIndex(new Document(ci_Edge_SourceType, 1).append(ci_Edge_SourceID, 1).append(ci_Edge_TargetType, 1).append(ci_Edge_TFIDF, -1));
//
		if(VERBOSE)
			System.out.println("Creating indexes for NodeDegs");
		cOUT_NodeDegs.createIndex(new Document(ci_NodeDegrees_id, 1));
		cOUT_NodeDegs.createIndex(new Document(ci_NodeDegrees_type, 1).append(ci_NodeDegrees_id, 1));
//
		if(VERBOSE)
			System.out.println("Creating indexes for Terms");
		cOUT_TER.createIndex(new Document(ci_Terms_id, 1));
		cOUT_TER.createIndex(new Document(ci_Terms_label, 1));
		if(VERBOSE)
			System.out.println("Creating indexes for Pages");
		cOUT_PAG.createIndex(new Document(ci_Page_id, 1));

		if(VERBOSE)
			System.out.println("Creating indexes for Sentences");
		cOUT_SEN.createIndex(new Document(ci_Sentences_occurence_id, 1));

		if(VERBOSE)
			System.out.println("Creating indexes for Entities");
		cOUT_ENT.createIndex(new Document(ci_Entity_node_id, 1));
		cOUT_ENT.createIndex(new Document(ci_Entity_type, 1).append(ci_Entity_node_id, 1));
		IndexOptions options = new IndexOptions();
		options.defaultLanguage("none");
		cOUT_ENT.createIndex(new Document(ci_Entity_label, "text").append(ci_Entity_senDegree, -1), options);

		if(VERBOSE)
			System.out.println("Creating indexes for Lookup");
		IndexOptions optionsInd = new IndexOptions();
		optionsInd.defaultLanguage("none");
		cOUT_LOOK.createIndex(new Document(ci_Entity_label, "text").append(ci_Entity_senDegree, -1), options);

		target_mongoClient.close();

		if(VERBOSE)
			System.out.println("All done!");
	}

}
