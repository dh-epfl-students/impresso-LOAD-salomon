package testing;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import impresso.S3Reader;
import impresso.SolrReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

public class YearQueryTest {
    public static void main(String[] args) throws IOException{
        boolean TEST_S3 = false;
        boolean TEST_SOLR = true;
        File file = new File("query_log.txt");
        //Instantiating the PrintStream class
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);


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

        String accessKey = System.getenv("S3_ACCESS_KEY");
        String secretKey = System.getenv("S3_SECRET_KEY");

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        // Set S3 Client Endpoint

        AwsClientBuilder.EndpointConfiguration switchEndpoint = new AwsClientBuilder.EndpointConfiguration(
                prop.getProperty("s3BaseName"),"");

        // Set signer type and http scheme
        ClientConfiguration conf = new ClientConfiguration();
        conf.setSignerOverride("S3SignerType");
        conf.setProtocol(Protocol.HTTPS);

        AmazonS3 S3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(switchEndpoint)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(conf)
                .withPathStyleAccessEnabled(true)
                .build();

        Cache<String, JSONObject> newspaperCache = CacheBuilder.newBuilder().build();
        Cache<String, JSONArray> entityCache = CacheBuilder.newBuilder().build();

        Collection<String> ids = new ArrayList<>();
        if(TEST_S3) {
            System.out.println("YEAR 1996 (max)");
            S3Reader S3reader = new S3Reader("GDL", "1996", prop, S3Client, newspaperCache, entityCache, ids);
            System.out.println("YEAR 1850 (med)");
            S3Reader S3reader3 = new S3Reader("GDL", "1850", prop, S3Client, newspaperCache, entityCache, ids);
            System.out.println("YEAR 1798 (min)");
            S3Reader S3reader2 = new S3Reader("GDL", "1798", prop, S3Client, newspaperCache, entityCache, ids);
        }

        SolrReader solrReader = new SolrReader(prop);

        if(TEST_SOLR) {
            System.out.println("YEAR 1996 (max)");
            solrReader.populateCache("GDL", "1996", entityCache);
            System.out.println("DONE");
            System.out.println("YEAR 1850 (med)");
            solrReader.populateCache("GDL", "1850", entityCache);
            System.out.println("YEAR 1798 (min)");
            solrReader.populateCache("GDL", "1798", entityCache);
        }


    }
}
