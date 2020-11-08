package testing;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import impresso.SolrReader;
import impresso.Token;
import impresso.ImpressoContentItem;
import impresso.S3Reader;


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
		List<String> luxwortIds = reader.getContentItemIDs("GDL","1890",  true);
		System.out.println(luxwortIds.size());
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
