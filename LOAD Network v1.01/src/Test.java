import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import settings.*;

import static settings.SystemSettings.*;

public class Test {
    public static void main(String[] args) throws IOException {
        Properties prop = new Properties();
        FileInputStream inputStream;
        if(VERBOSE)
            System.out.println("Loading properties file :"  + PROP_PATH);
        try {
            //NOTE: Input stream best solution?
            inputStream = new FileInputStream(PROP_PATH);
            prop.load(inputStream);
            inputStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if(VERBOSE)
            System.out.println("File was read and is now closed");

        String accessKey = System.getenv("S3_ACCESS_KEY");
        String secretKey = System.getenv("S3_SECRET_KEY");

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        // Set S3 Client Endpoint
        AwsClientBuilder.EndpointConfiguration switchEndpoint = new AwsClientBuilder.EndpointConfiguration(
                prop.getProperty("s3BaseName"),"");
        if(DEBUG_PROMPT)
            System.out.println("S3 endpoint setup for the S3 base :"  + prop.getProperty("s3BaseName"));

        // Set signer type and http scheme
        ClientConfiguration conf = new ClientConfiguration();
        conf.setSignerOverride("S3SignerType");
        conf.setSocketTimeout(TIMEOUT); //doubles default timeout
        conf.setProtocol(Protocol.HTTPS);

        AmazonS3 S3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(switchEndpoint)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(conf)
                .withPathStyleAccessEnabled(true)
                .build();
        String bucketName = prop.getProperty("s3BucketName");
        String newspaperKey = "linguistic-processing/2020-03-11/GDL-1996.ling.annotation.jsonl.bz2";
        GetObjectRequest object_request = new GetObjectRequest(bucketName, newspaperKey);
        S3Object fullObject = S3Client.getObject(object_request);

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new InputStreamReader(fullObject.getObjectContent()));
        reader.setLenient(true);
        String s = reader.nextString();
        reader.beginObject();
        while (reader.hasNext()) {
            String line = gson.fromJson(reader, String.class);
        }
        reader.endArray();
        reader.close();
        }

    }
