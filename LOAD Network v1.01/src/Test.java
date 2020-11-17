import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import settings.*;

import static settings.SystemSettings.ID_FOLDER;

public class Test {
    public static void main(String[] args) throws IOException {

        String folder_path = ID_FOLDER + "GDL" + "-ids";
        File newspaper_folder = new File(folder_path);
        File year_file = new File(folder_path + "/" + "1798" + ".txt");
        List<String> lines = new ArrayList<String>();
        lines = FileUtils.readLines(year_file, "UTF-8");

        System.out.println("J");

    }
}