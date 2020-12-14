package impresso;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.Builder;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;
import settings.SystemSettings;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.DEBUG_PROMPT;
import static settings.SystemSettings.VERBOSE;


public class SolrReader {
	private static final Logger LOGGER = Logger.getLogger(impresso.SolrReader.class.getName());
	private static Properties prop;
	private static String year = null;
	private static HttpSolrClient contentItemsClient;
	private static HttpSolrClient entityClient;
	private static String contentItemDBName;
	private static String solrEntityDBName;
	private static String solrUserName;

	public SolrReader(Properties properties) {
		prop = properties;
		contentItemDBName = properties.getProperty("solrDBName");
		solrEntityDBName = properties.getProperty("solrEntityDBName");
		solrUserName = properties.getProperty("solrUserName");
		contentItemsClient = (new Builder(properties.getProperty("solrDBName"))).build();
		entityClient = (new Builder(properties.getProperty("solrEntityDBName"))).build();
	}

	public List<String> getContentItemIDs(String newspaperID, String year, boolean firstRead) {
		List<String> solrIds = new ArrayList();
		String fileFolder = String.format(SystemSettings.ID_FOLDER + "%s-ids", newspaperID);
		if (firstRead) {
			HttpSolrClient client = (new Builder(contentItemDBName)).build();
			SolrQuery solrQuery = new SolrQuery();
			solrQuery.setQuery("meta_journal_s:" + newspaperID + " AND item_type_s:(ar OR ob OR page) AND meta_year_i:" + year);
			solrQuery.set("fl", new String[]{"id"});
			solrQuery.setSort("id", ORDER.asc);
			solrQuery.setRows(50000);
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
			String cursorMark = "*";
			boolean done = false;
			LOGGER.log(Level.FINE, " Cursor created");
			System.out.println("Cursor created");
			int counter = 0;

			QueryResponse prevYear;
			String file;
			try {
				for(; !done; cursorMark = file) {
					counter += 50000;
					LOGGER.log(Level.FINER, "processing[{0}]", new Object[]{counter});
					System.out.printf("processing %d%n cursor:%s%n", counter, cursorMark);
					solrQuery.set("cursorMark", new String[]{cursorMark});
					queryRequest = new QueryRequest(solrQuery);
					queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
					prevYear = (QueryResponse)queryRequest.process(client);
					System.out.println(prevYear.getResults().getNumFound());
					solrIds.addAll((Collection)prevYear.getResults().stream().map((x) -> {
						return (String)x.get("id");
					}).collect(Collectors.toList()));
					file = prevYear.getNextCursorMark();
					if (cursorMark.equals(file)) {
						done = true;
					}
				}
			} catch (SolrServerException var21) {
				var21.printStackTrace();
			} catch (IOException var22) {
				var22.printStackTrace();
			}

			try {
				prevYear = null;
				file = null;
				FileWriter writer = null;

				String id;
				for(Iterator var15 = solrIds.iterator(); var15.hasNext(); writer.write(id + System.lineSeparator())) {
					id = (String)var15.next();
					String curYear = id.split("-")[1];
					if (!curYear.equals(prevYear)) {
						if (writer != null) {
							if (SystemSettings.DEBUG_PROMPT) {
								System.out.println("Successfully wrote to the file.");
							}

							writer.close();
						}

						File directory = new File(fileFolder);
						if (!directory.exists()) {
							directory.mkdir();
						}

						file = String.format("%s/%s.txt", fileFolder, curYear);
						writer = new FileWriter(file, true);
					}
				}
			} catch (IOException var20) {
				System.out.println("An error occurred.");
				var20.printStackTrace();
			}
		} else if (newspaperID != null && year != null) {
			try {
				String file = String.format("%s/%d", fileFolder, year);
				solrIds = new ArrayList(Files.readAllLines(Paths.get(file)));
			} catch (IOException var19) {
			}
		}

		return solrIds;
	}

	public ImpressoContentItem getContentItem(String solrId) {
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:" + solrId);
		solrQuery.set("fl", new String[]{"*"});
		solrQuery.setRows(1);
		ImpressoContentItem impressoItem = null;

		try {
			HttpSolrClient client = (new Builder(contentItemDBName)).build();
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
			SolrDocumentList solrResponse = ((QueryResponse)queryRequest.process(client)).getResults();
			impressoItem = new ImpressoContentItem((SolrDocument)solrResponse.get(0), prop);
		} catch (SolrServerException var7) {
			var7.printStackTrace();
		} catch (IOException var8) {
			var8.printStackTrace();
		}

		return impressoItem;
	}

	public void populateCache(String newspaperID, String year, Cache<String, JSONArray> entityCache){
		System.out.println("Get all the mentionned entities for " +newspaperID+ " in " + year);
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("meta_journal_s:" + newspaperID + " AND item_type_s:(ar OR ob OR page) AND meta_year_i:" + year);
		solrQuery.set("fl", new String[]{"*"});
		solrQuery.setSort("id", ORDER.asc);
		solrQuery.setRows(50000);
		QueryRequest queryRequest = new QueryRequest(solrQuery);
		queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
		String cursorMark = "*";
		boolean done = false;
		LOGGER.log(Level.FINE, " Cursor created");
		if(VERBOSE)
			System.out.println("Cursor created");
		int counter = 0;

		QueryResponse contentItems;
		String file;
			try {
				for(; !done; cursorMark = file) {
					counter += 50000;
					LOGGER.log(Level.FINER, "processing[{0}]", new Object[]{counter});
					System.out.printf("processing %d%n cursor:%s%n", counter, cursorMark);
					solrQuery.set("cursorMark", new String[]{cursorMark});
					queryRequest = new QueryRequest(solrQuery);
					queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
					contentItems = (QueryResponse)queryRequest.process(contentItemsClient);
					SolrDocumentList resultBatch = contentItems.getResults();
					for(SolrDocument res : resultBatch){
						String articleId = (String) res.getFieldValue("id");
						Collection<String> fields = res.getFieldNames();

						JSONArray entities = new JSONArray();
						if(fields.contains("pers_mentions")){
							String[] persons = ((String)((ArrayList) res.getFieldValue("pers_mentions")).get(0)).split("\\|");
							//String[] entities_offsets = parseString(((String)res.getFieldValue("nem_offset_plain")), "person");

							for(int i = 0; i < persons.length; i++) {
								String p = persons[i];
								String[] entityInfo = getEntityInfoFromMention("\"" + p + "\"", articleId);
								if(entityInfo != null) {
									JSONObject entity = new JSONObject();
									entity.put("mention", p);
									entity.put("entity_id", entityInfo[0]);
									entity.put("entity_type", actEnt);
									entity.put("start_offset", entityInfo[1]);//entity.put("offset", Integer.parseInt(entities_offsets[i].split(idInfoSepChar)[0]));
									entity.put("system_id", entityInfo[2]);
									entity.put("surface", entityInfo[3]);

									entities.put(entity);
								}
							}
						}

						if(fields.contains("loc_mentions")){
							String[] locations = ((String)((ArrayList) res.getFieldValue("loc_mentions")).get(0)).split("\\|");
							//String[] entities_offsets = parseString(((String)res.getFieldValue("nem_offset_plain")), "person");

							for(String l : locations) {
								String[] entityInfo = getEntityInfoFromMention("\"" + l + "\"", articleId);
								if(entityInfo != null) {
									JSONObject entity = new JSONObject();
									entity.put("mention", l);
									entity.put("entity_id", entityInfo[0]);
									entity.put("entity_type", locEnt);
									entity.put("start_offset", entityInfo[1]); //entity.put("offset", Integer.parseInt(entities_offsets[i].split(idInfoSepChar)[0]));
									entity.put("system_id", entityInfo[2]);
									entity.put("surface", entityInfo[3]);
									entities.put(entity);
								}
							}
						}
						if(entities.length() > 0) {
							if(DEBUG_PROMPT){
								System.out.println(newspaperID + "-" + year + ": " +entities.length() + " entities.");
							}
							entityCache.put(articleId, entities);
						}
					}

					file = contentItems.getNextCursorMark();
					if (cursorMark.equals(file)) {
						done = true;
					}
				}
			} catch (SolrServerException var21) {
				var21.printStackTrace();
			} catch (IOException var22) {
				var22.printStackTrace();
			}
	}

	public String[] getEntityInfoFromMention(String mention, String articleId){
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("m_name_text:" + mention + " AND ci_id_s:" + articleId);
		solrQuery.set("fl", new String[]{"e_id_s", "beg_offset_i", "nerc_sysid_s", "m_surface_s"});
		solrQuery.setRows(1);

		try {
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
			SolrDocument solrResponse = ((QueryResponse)queryRequest.process(entityClient)).getResults().get(0);
			String[] entityInfo = new String[4];
			entityInfo[0] = (String) solrResponse.getFieldValue("e_id_s"); //Entity id
			entityInfo[1] = String.valueOf(((int) solrResponse.getFieldValue("beg_offset_i"))); //Mention offset
			entityInfo[2] = (String) solrResponse.getFieldValue("nerc_sysid_s"); //System id
			entityInfo[3] = (String) solrResponse.getFieldValue("m_surface_s");
			return entityInfo;
		} catch(IndexOutOfBoundsException e){
			return null;
		} catch(SolrServerException var6) {
			var6.printStackTrace();
		} catch (IOException var7) {
			var7.printStackTrace();
		}
		return null;
	}

	public String getEntityInfo(String entityId) {
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("e_id_s:" + entityId);
		solrQuery.set("fl", new String[]{"*"});
		solrQuery.setRows(1);

		try {
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
			SolrDocument solrResponse = ((QueryResponse)queryRequest.process(entityClient)).getResults().get(0);
			String wikidata = (String) solrResponse.getFieldValue("wkd_id_s");
			String label = (String) solrResponse.getFieldValue("mentionSuggest");
			return wikidata + idInfoSepChar + label;
		} catch (SolrServerException var6) {
			var6.printStackTrace();
		} catch (IOException var7) {
			var7.printStackTrace();
		}
		return null;
	}

	/*public static String[] parseString(String s, String type){
		String[] coords;
		String[] split = s.split("]]}");
		if(type.equals("person")) {
			String tmp = split[0].split(": ")[1];
			String[] tab = tmp.replaceAll("\\[", "").split("], ");
			coords = new String[tab.length];
			for (int i = 0; i < coords.length; i++) {
				String[] coord = tab[i].split(", ");
				if (coord[0] == "")
					coords[i] = coord[1] + idInfoSepChar + coord[2];
				else
					coords[i] = coord[0] + idInfoSepChar + coord[1];
			}
		} else {
			String tmp = split.length == 3 ? split[1].split(": ")[1] : split[0].split(": ")[1];
			String[] tab = tmp.replaceAll("\\[", "").split("], ");
			coords = new String[tab.length];
			for (int i = 0; i < coords.length; i++) {
				String[] coord = tab[i].split(", ");
				coords[i] = coord[0] + idInfoSepChar + coord[1];
			}
		}

		return coords;
	}*/

}

