package wikidatademo.graph;

public class SentenceInfoItem {

    public int node_id;			  // internal ID of he node in the graph (unique in its set by type)
    public String type;			  // type of the node. Here: SEN
    public String text;		      // text of the sentence
    public String pageURL;		  // URL of the page that contains the sentence
    public double score;		  // ranking score

    public SentenceInfoItem(int node_id, String text, String pageurl, double score) {
        this.node_id = node_id;
        this.type = "SEN";
        this.score = score;
        this.text = text;
        this.pageURL = pageurl;
    }

    @Override
    public String toString() {
        String retval = type + " " + node_id + ": <" + score + "> [" + pageURL + "] " + text;
        return retval;
    }

    // return a unique string nodeID (IDs are not unique without the type information)
    public String getStringNodeID() {
        return type + node_id;
    }

    @Override
    public int hashCode() {
        return node_id;			// can cause at most 7 collision due to 7 types
    }
}