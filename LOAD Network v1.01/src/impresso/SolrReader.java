package impresso;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.io.FileWriter;

import com.google.common.cache.Cache;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONObject;

import static settings.SystemSettings.DEBUG_PROMPT;
import static settings.SystemSettings.ID_FOLDER;


public class SolrReader {

	private static final Logger LOGGER = Logger.getLogger(SolrReader.class.getName());
	private static Properties prop;
	private static String year = null;
	private static HttpSolrClient clientTest;
	private static String solrDBName;
	private static String solrUserName;
	//NOTE: get only useful info from the properties component
	public SolrReader(Properties properties) {
		prop = properties;
		solrDBName = properties.getProperty("solrDBName");
		solrUserName = properties.getProperty("solrUserName");
		clientTest = new HttpSolrClient.Builder(properties.getProperty("solrDBName")).build();
	}


	public static void populateCache(String contentID, Cache<String, SolrDocument> entityCache){
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:"+contentID);
		solrQuery.set("fl","*");
		solrQuery.setRows(1);

		ImpressoContentItem impressoItem = null;
		try {
			HttpSolrClient client = new HttpSolrClient.Builder(solrDBName).build();
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName,System.getenv("SOLR_PASSWORD"));
			SolrDocument solrResponse = queryRequest.process(client).getResults().get(0);
			entityCache.put(solrResponse.get("id").toString() , solrResponse);
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<String> getContentItemIDs(String newspaperID,String year, boolean firstRead) {
		List<String> solrIds = new ArrayList<>();
	    String fileFolder = String.format(ID_FOLDER +"%s-ids",  newspaperID);
		
		if(firstRead) {
			HttpSolrClient client = new HttpSolrClient.Builder(solrDBName).build();
			SolrQuery solrQuery = new SolrQuery();
			solrQuery.setQuery("meta_journal_s:"+newspaperID +" AND item_type_s:(ar OR ob OR page) AND meta_year_i:"+ year); //Sets query for the newspaper and limits the item_type
			solrQuery.set("fl","id");
			solrQuery.setSort("id",  ORDER.asc);
			solrQuery.setRows(50000);
		    QueryRequest queryRequest = new QueryRequest(solrQuery);
		    queryRequest.setBasicAuthCredentials(solrUserName,System.getenv("SOLR_PASSWORD"));
		    String cursorMark = CursorMarkParams.CURSOR_MARK_START;
		    boolean done = false;
		    
		    LOGGER.log(Level.FINE," Cursor created");
		    System.out.println("Cursor created");
		    int counter = 0;
		    
			try {
				while(!done) {
					counter += 50000;
					LOGGER.log( Level.FINER," processing[{0}]",  new Object[]{counter} );
					System.out.printf("processing %d%n cursor:%s%n",  counter, cursorMark);
					
				    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
				    
				    queryRequest = new QueryRequest(solrQuery);
				    queryRequest.setBasicAuthCredentials(solrUserName,System.getenv("SOLR_PASSWORD"));
				    QueryResponse solrResponse = queryRequest.process(client);
				    System.out.println(solrResponse.getResults().getNumFound());
				    solrIds.addAll(solrResponse.getResults()
				    		  .stream()
				    		  .map(x -> (String) x.get("id"))
				    		  .collect(Collectors.toList()));
				    String nextCursorMark = solrResponse.getNextCursorMark();
				    if (cursorMark.equals(nextCursorMark)) {
				        done = true;
				    }
				    cursorMark = nextCursorMark;
				}
	
			} catch (SolrServerException e) {
			    e.printStackTrace();
			} catch (IOException e) {
			    e.printStackTrace();
			}
			//Write to the newspaper folder in a text file that is divided by year
			try {
				  String prevYear = null, file = null;
				  FileWriter writer = null;
		    	  for (String id: solrIds) {
		    		  String curYear = id.split("-")[1]; //When file is in the format Newspaperid-year, finds second element as year
		    		  if(!curYear.equals(prevYear)) {
		    			  if(writer != null) {
		    			  	  if(DEBUG_PROMPT)
							      System.out.println("Successfully wrote to the file.");
			    			  writer.close();
			    			  if(DEBUG_PROMPT)
							      System.out.println("Beginning to write to file");
		    			  }
		    			  file = String.format("%s/%s.txt", fileFolder, curYear);
		    			  writer = new FileWriter(file, true);
		    		  }
		    		  writer.write(id+ System.lineSeparator());
		    	  }
			    } catch (IOException e) {
			      System.out.println("An error occurred.");
			      e.printStackTrace();
			    }
			
		}
		else if(newspaperID != null && year != null) {
			try {
				//When reading from the already read ids
				String file = String.format("%s/%d",  fileFolder, year);
			    solrIds = new ArrayList<>(Files.readAllLines(Paths.get(file)));
			}
			catch (IOException e) {
			    // Handle a potential exception
			}
		}
			
		return solrIds;

	}

	public SolrDocument getSolrDocument(String solrId){
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:"+solrId);
		solrQuery.set("fl","*");
		solrQuery.setRows(1);

		ImpressoContentItem impressoItem = null;
		try {
			HttpSolrClient client = new HttpSolrClient.Builder(solrDBName).build();
			QueryRequest queryRequest = new QueryRequest(solrQuery);
			queryRequest.setBasicAuthCredentials(solrUserName,System.getenv("SOLR_PASSWORD"));
			SolrDocument solrResponse = queryRequest.process(client).getResults().get(0);

			return solrResponse;
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ImpressoContentItem getContentItem(String solrId) {
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("id:"+solrId);
		solrQuery.set("fl","*");
		solrQuery.setRows(1);
		
		ImpressoContentItem impressoItem = null;
		try {
			HttpSolrClient client = new HttpSolrClient.Builder(solrDBName).build();
		    QueryRequest queryRequest = new QueryRequest(solrQuery);
		    queryRequest.setBasicAuthCredentials(solrUserName,System.getenv("SOLR_PASSWORD"));
		    SolrDocumentList solrResponse = queryRequest.process(client).getResults();
		    impressoItem = new ImpressoContentItem(solrResponse.get(0), prop); //Get the only item of the list
		} catch (SolrServerException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		return impressoItem;
	}
	
	public void getEntityId(String entityId) {
		//Test to access entityId
		HttpSolrClient client = new HttpSolrClient.Builder(solrDBName).build();
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery(entityId);
		solrQuery.set("fl","*");
		solrQuery.setRows(1);
		
		try {
		    QueryRequest queryRequest = new QueryRequest(solrQuery);
		    queryRequest.setBasicAuthCredentials(solrUserName,System.getenv("SOLR_PASSWORD"));
		    SolrDocumentList solrResponse = queryRequest.process(client).getResults();
		    System.out.println(solrResponse.get(0)); //Get the only item of the list
		} catch (SolrServerException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		return;
	}
		
}
