
import com.fasterxml.jackson.core.JsonParser;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.noggit.JSONParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Test {
    public static void main(String[] args){


        try {
            FileInputStream fin = new FileInputStream("GDL-mentions/GDL-1890-mentions.jsonl.bz2");
            try (Scanner fileIn = new Scanner(new BZip2CompressorInputStream(fin))) {
                //First download the key
                // Read the text input stream one line at a as a json object and parse this object into contentitems
                if (null != fileIn) {
                    while (fileIn.hasNext()) {
                        JSONObject jsonObj = new JSONObject(fileIn.nextLine());
                        System.out.println(jsonObj.toString());
                    }
                }
            }
        } catch (Exception e){

        }



    }
}