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
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;
import settings.SystemSettings;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.*;


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

	public void countEntities(String newspaperID, String year, HashMap<String, Integer> ents_count) {
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("meta_journal_s:" + newspaperID + " AND item_type_s:(ar OR ob OR page) AND meta_year_i:" + year);
		solrQuery.set("fl", new String[]{"id", "pers_mentions", "loc_mentions"});
		solrQuery.setSort("id", ORDER.asc);
		solrQuery.setRows(50000);
		QueryRequest queryRequest = new QueryRequest(solrQuery);
		queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
		String cursorMark = "*";
		boolean done = false;
		int counter = 0;

		QueryResponse contentItems;
		String file;
		try {
			for(; !done; cursorMark = file) {
				counter += 50000;
				solrQuery.set("cursorMark", new String[]{cursorMark});
				long time = System.nanoTime();
				contentItems = (QueryResponse)queryRequest.process(contentItemsClient);


				SolrDocumentList resultBatch = contentItems.getResults();
				for(SolrDocument res : resultBatch){
					int ent_count = 0;
					String articleId = (String) res.getFieldValue("id");
					Collection<String> fields = res.getFieldNames();

					JSONArray entities = new JSONArray();
					if(fields.contains("pers_mentions")){
						String[] persons = ((String)((ArrayList) res.getFieldValue("pers_mentions")).get(0)).split("\\|");
						ent_count += persons.length;
					}

					if(fields.contains("loc_mentions")){
						String[] locations = ((String)((ArrayList) res.getFieldValue("loc_mentions")).get(0)).split("\\|");
						ent_count += locations.length;
					}
					ents_count.put(articleId, ent_count);
					System.out.println(articleId + " - " + ent_count);
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

	public void populateCache(String newspaperID, String year, Cache<String, JSONArray> entityCache){
		System.out.println("Get all the mentionned entities for " +newspaperID+ " in " + year);
		long start_time = System.currentTimeMillis();
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("meta_journal_s:" + newspaperID + " AND item_type_s:(ar OR ob OR page) AND meta_year_i:" + year);
		solrQuery.set("fl", new String[]{"id", "pers_mentions", "nem_offset_plain", "loc_mentions"});
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
					long time = System.nanoTime();
					contentItems = (QueryResponse)queryRequest.process(contentItemsClient);


					SolrDocumentList resultBatch = contentItems.getResults();
					if(RUNTIME_PROMPT) {
						System.out.println("PROCESS QUERY " + resultBatch.size() + " : " + (System.nanoTime() - time) + " ns");
					}
					for(SolrDocument res : resultBatch){
						long doc_time = System.currentTimeMillis();
						String articleId = (String) res.getFieldValue("id");
						Collection<String> fields = res.getFieldNames();

						JSONArray entities = new JSONArray();
						if(fields.contains("pers_mentions")){
							String[] persons = ((String)((ArrayList) res.getFieldValue("pers_mentions")).get(0)).split("\\|");
							String[] entities_offsets = parseString(((String)res.getFieldValue("nem_offset_plain")), "person");

							for(int i = 0; i < persons.length; i++) {
								String p = persons[i];
								int offset = Integer.valueOf(entities_offsets[i].split(idInfoSepChar)[0]);
								String[] entityInfo = getEntityInfoFromMention("\"" + p + "\"", articleId, offset);
								if(entityInfo != null) {
									JSONObject entity = new JSONObject();
									entity.put("mention", p);
									entity.put("entity_id", entityInfo[0]);
									entity.put("entity_type", actEnt);
									entity.put("start_offset", offset);
									entity.put("system_id", entityInfo[1]);
									entity.put("surface", entityInfo[2]);

									entities.put(entity);
								}
							}
						}

						if(fields.contains("loc_mentions")){
							String[] locations = ((String)((ArrayList) res.getFieldValue("loc_mentions")).get(0)).split("\\|");
							String[] entities_offsets = parseString(((String)res.getFieldValue("nem_offset_plain")), "location");

							for(int i = 0; i < locations.length; i++) {
								String l = locations[i];
								int offset = Integer.valueOf(entities_offsets[i].split(idInfoSepChar)[0]);
								String[] entityInfo = getEntityInfoFromMention("\"" + l + "\"", articleId, offset);
								if(entityInfo != null) {
									JSONObject entity = new JSONObject();
									entity.put("mention", l);
									entity.put("entity_id", entityInfo[0]);
									entity.put("entity_type", locEnt);
									entity.put("start_offset", offset);
									entity.put("system_id", entityInfo[1]);
									entity.put("surface", entityInfo[2]);
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
						if(RUNTIME_PROMPT)
							System.out.println("DOCUMENT ENTITIES : " + (doc_time - System.currentTimeMillis()) + " ms");
					}

					file = contentItems.getNextCursorMark();
					if (cursorMark.equals(file)) {
						done = true;
					}
				}
				System.out.println("Entities queried in : " + (System.currentTimeMillis() - start_time) + " ms");
			} catch (SolrServerException var21) {
				var21.printStackTrace();
			} catch (IOException var22) {
				var22.printStackTrace();
			}
	}

	public String[] getEntityInfoFromMention(String mention, String articleId, int offset){
		long time = System.nanoTime();
		SolrQuery solrQuery = new SolrQuery();
		String query = "m_name_text:" + ClientUtils.escapeQueryChars(mention) + " AND ci_id_s:" + articleId + " AND beg_offset_i:" + offset;
		solrQuery.setQuery(query);
		solrQuery.set("fl", new String[]{"e_id_s", "nerc_sysid_s", "m_surface_s"});
		solrQuery.setRows(1);

		try {
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
			SolrDocument solrResponse = ((QueryResponse)queryRequest.process(entityClient)).getResults().get(0);
			String[] entityInfo = new String[3];
			entityInfo[0] = (String) solrResponse.getFieldValue("e_id_s"); //Entity id
			entityInfo[1] = (String) solrResponse.getFieldValue("nerc_sysid_s"); //System id
			entityInfo[2] = (String) solrResponse.getFieldValue("m_surface_s"); //Label
			if(RUNTIME_PROMPT)
				System.out.println("ENTITY INFO : " + (System.nanoTime() - time) + " ns");
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
		solrQuery.setQuery("e_id_s:" + ClientUtils.escapeQueryChars(entityId));
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

	public static String getArticleTitle(String articleId){
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:" + articleId);
		solrQuery.set("fl", new String[]{"*"});
		solrQuery.setRows(1);
		String title = "";
		try {
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
			SolrDocument solrResponse = ((QueryResponse) queryRequest.process(contentItemsClient)).getResults().get(0);
			String language = (String) solrResponse.getFieldValue("lg_s");
			switch(language) {
				case"fr":
					title = ((String) solrResponse.getFieldValue("title_txt_fr"));
					break;
				case"de":
					title = ((String) solrResponse.getFieldValue("title_txt_de"));
					break;
				case"lu":
					title = ((String) solrResponse.getFieldValue("title_txt_lu"));
					break;
			}
		} catch (SolrServerException var6) {
			var6.printStackTrace();
		} catch (IOException var7) {
			var7.printStackTrace();
		}
		return title;
	}
	public static String getSentenceText(String articleId, int minOffset, int maxOffset){
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:" + articleId);
		solrQuery.set("fl", new String[]{"*"});
		solrQuery.setRows(1);
		String sentence = "";
		try {
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName, System.getenv("SOLR_PASSWORD"));
			SolrDocument solrResponse = ((QueryResponse) queryRequest.process(contentItemsClient)).getResults().get(0);
			String language = (String) solrResponse.getFieldValue("lg_s");
			switch(language) {
				case"fr":
					sentence = ((String) solrResponse.getFieldValue("content_txt_fr")).substring(minOffset, maxOffset);
					break;
				case"de":
					sentence = ((String) solrResponse.getFieldValue("content_txt_de")).substring(minOffset, maxOffset);
					break;
				case"lu":
					sentence = ((String) solrResponse.getFieldValue("content_txt_lu")).substring(minOffset, maxOffset);
					break;
			}

		} catch (SolrServerException var6) {
			var6.printStackTrace();
		} catch (IOException var7) {
			var7.printStackTrace();
		}
		return sentence;
	}

	public static String[] parseString(String s, String type){
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
	}

}

