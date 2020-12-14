import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import impresso.SolrReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.json.JSONArray;

import static settings.LOADmodelSettings.idInfoSepChar;
import static settings.SystemSettings.*;

public class Test {

    public static void main(String[] args) throws IOException {
        Properties prop = new Properties();
        FileInputStream inputStream;
        if (VERBOSE)
            System.out.println("Loading properties file :" + PROP_PATH);
        try {
            //NOTE: Input stream best solution?
            inputStream = new FileInputStream(PROP_PATH);
            prop.load(inputStream);
            inputStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (VERBOSE)
            System.out.println("File was read and is now closed");

        SolrReader reader = new SolrReader(prop);
        Cache<String, JSONArray> cache = CacheBuilder.newBuilder().build();
        reader.populateCache("GDL", "1799", cache);
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
