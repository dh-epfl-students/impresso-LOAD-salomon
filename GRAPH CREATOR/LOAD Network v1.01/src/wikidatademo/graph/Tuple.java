package wikidatademo.graph;

/**
 * Item used to count entity occurrences
 * 
 * Originally published December 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class Tuple {
	
	public int count;
	public double score;
	
	public Tuple(int count, double score) {
		this.count = count;
		this.score = score;
	}
	
}
