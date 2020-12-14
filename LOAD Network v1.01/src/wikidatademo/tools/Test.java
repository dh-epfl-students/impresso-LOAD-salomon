package wikidatademo.tools;

import impresso.SolrReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static settings.SystemSettings.PROP_PATH;

public class Test {
    public static void main(String[] args){
        Properties prop=new Properties();
        String propFilePath = PROP_PATH;

        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(propFilePath);
            prop.load(inputStream);
            inputStream.close();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
        SolrReader reader = new SolrReader(prop);
        reader.getEntityInfo("aida-0001-50-Zeph_Gladstone");
        System.out.println("BA");
    }
}
