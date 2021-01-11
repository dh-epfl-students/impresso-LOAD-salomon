package testing;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import impresso.SolrReader;
import org.json.JSONArray;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SentenceRetrieverTest {
    public static void main(String[] args){
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
        //System.out.println(reader.getSentenceText("GDL-1986-01-03-a-i0028", 0,26));
        //System.out.println(reader.getArticleTitle("GDL-1986-01-03-a-i0028"));
        Cache<String, JSONArray> cache = CacheBuilder.newBuilder().build();
        reader.populateCache("GDL", "1798", cache);
    }
}
