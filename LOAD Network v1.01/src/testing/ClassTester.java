package testing;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import impresso.SolrReader;
import impresso.Token;
import impresso.ImpressoContentItem;
import impresso.S3Reader;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;


public class ClassTester {

	private static final Logger LOGGER = Logger.getLogger(ClassTester.class.getName());


	public static void main(String[] args) throws IOException {
		//Creating logger
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINE);
		LOGGER.addHandler(handler);
		LOGGER.log( Level.FINE,"Starting GDL reading");

		//Loads the property file
		Properties prop=new Properties();
		String propFilePath ="LOAD Network v1.01/resources/config.properties";
		
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(propFilePath);
			prop.load(inputStream);
			inputStream.close();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
			
		SolrReader reader = new SolrReader(prop);
		
		//reader.getEntityId("aida-0001-54-Italy");
		List<String> luxwortIds = reader.getContentItemIDs("GDL","1891",  true);
		System.out.println(luxwortIds.size());
		SolrDocument doc = reader.getSolrDocument(luxwortIds.get(0));
		System.out.println(doc.get("id"));
		System.out.println(doc.get("meta_issue_id_s"));
		System.out.println(doc.get("page_id_ss"));
		System.out.println(doc.get("meta_partnerid_s"));

		System.out.println("=============OFFSET============");
		System.out.println(doc.get("nem_offset_plain"));

		System.out.println("=============LENGTH============");
		System.out.println(doc.get("content_length_i"));

		System.out.println("=============SURFACE = WORD?============");
		System.out.println(doc.get("pers_mentions"));
		System.out.println(doc.get("loc_mentions"));

		System.out.println(("======NAME========="));
		System.out.println(doc.get("pers_mentions"));
		System.out.println(doc.get("loc_mentions"));

		System.out.println("======ENTITIES===========");
		System.out.println(doc.get("pers_entities_dpfs"));
		System.out.println(doc.get("loc_entities_dpfs"));
		System.out.println("========================");


		String[] pers_mentions = doc.get("pers_mentions").toString().split("\\|");
		String[] pers_entities = doc.get("pers_entities_dpfs").toString().split("\\|");
		String offsets = doc.getFieldValue("nem_offset_plain").toString();
		Object funct = doc.get("funct_pers_mentions");
		offsets = offsets.substring(1, offsets.length() -1);
		JSONObject offsetsJ = new JSONObject(offsets);
		JSONArray pers_offsets = offsetsJ.getJSONArray("person");
		for(int i = 0; i < pers_offsets.length(); i++){
			JSONObject pers_mention = new JSONObject();
			pers_mention.put("surface", pers_mentions[i]);
			pers_mention.put("name", pers_mentions[i]);

			String offset_vals = pers_offsets.get(i).toString();
			String[] tuple = offset_vals.substring(1, offset_vals.length()-1).split(",");
			pers_mention.put("start_offset", tuple[0]);
			pers_mention.put("length", tuple[1]);
			pers_mention.put("type", "pers");
		}
		System.out.println("HELLO");

		/*
		//Testing getting the contentId
		ImpressoContentItem test = reader.getContentItem(luxwortIds.get(4));
		System.out.println(test.getContent_txt());
		
		S3Reader injector = new S3Reader("luxwort"," 1848"  prop);
		test = injector.injectLingusticAnnotations(test);
		for(Token token: test.getTokens()) {
			System.out.println(token.getPOS());
		}*/
	}

}
