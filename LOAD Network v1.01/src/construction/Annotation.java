package construction;

/**
 * Named Entity Annotation class for LOAD Graph construction
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class Annotation implements Comparable<Annotation> {
    public char type;
    public String value;
    public int id;
    public int sentenceID;
    
    public Annotation(String value, int id, char type, int sentenceID) {
        this.value = value;
        this.id = id;
        this.type = type;
        this.sentenceID = sentenceID;
    }
    
    @Override
    // sort by sentence ID only
    public int compareTo(Annotation a) {
        return (sentenceID - a.sentenceID);
    }
    
}
