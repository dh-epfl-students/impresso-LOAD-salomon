package testing;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.IntStream;


import com.jcraft.jsch.IO;
import impresso.SolrReader;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.request.QueryRequest;

import static settings.LOADmodelSettings.MAX_YEAR;
import static settings.LOADmodelSettings.MIN_YEAR;


public class SolrQuerying {
	
	public static void main(String[] args) throws IOException{
		entityCount();
	}

	public static void count_docs() throws FileNotFoundException, IOException {

		Properties prop=new Properties();
		String propFilePath ="LOAD Network v1.01/resources/config.properties";
		
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(propFilePath);
			prop.load(inputStream);
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		 
	    System.out.println(prop.getProperty("solrDBName"));
		HttpSolrClient client = new HttpSolrClient.Builder(prop.getProperty("solrDBName")).build();
		SolrQuery solrQuery = new SolrQuery();

		BufferedReader reader = new BufferedReader(new FileReader("local_files/GDL-ids/1985.txt"));
		String line = reader.readLine();
		while (line != null) {
			solrQuery.setQuery("id:GDL-1798-12-01-a-i0012");
			solrQuery.set("fl", "*");
			solrQuery.setRows(1);

			try {
				QueryRequest queryRequest = new QueryRequest(solrQuery);
				queryRequest.setBasicAuthCredentials(prop.getProperty("solrUserName"), System.getenv("SOLR_PASSWORD"));
				QueryResponse solrResponse = queryRequest.process(client);
				System.out.println(solrResponse);
				System.out.println("Total Documents :" + solrResponse.getResults().getNumFound());
			} catch (SolrServerException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			line = reader.readLine();
		}
	}

	public static HashMap<String, Integer> entityCount() throws IOException {
		File file = new File("countEnt_log.txt");
		//Instantiating the PrintStream class
		PrintStream stream = new PrintStream(file);
		System.setOut(stream);
		Properties prop=new Properties();
		String propFilePath ="LOAD Network v1.01/resources/config.properties";

		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(propFilePath);
			prop.load(inputStream);

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		SolrReader reader = new SolrReader(prop);
		HashMap<String, Integer> ents_count = new HashMap<>();
		String[] years = {"1798", "1799"}; //IntStream.range(MIN_YEAR, MAX_YEAR).mapToObj(String::valueOf).toArray(String[]::new);
		for(String year : years){
			System.out.println("YEAR : " + year);
			reader.countEntities("GDL", year, ents_count);
		}
		return ents_count;
	}

}