package wikidatademo.graph;

/**
 * Item used for sorting sentences and pages by importance
 * 
 * Originally published December 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class OccurrenceScoreItem implements Comparable<OccurrenceScoreItem> {
	
	public int id;
	public int occurrence;
	public double score;
	public int page_id;
	
	public OccurrenceScoreItem(int id, int occurrence, double score, int page_id) {
		this.id = id;
		this.occurrence = occurrence;
		this.score = score;
		this.page_id = page_id;
	}
	
	@Override
	public boolean equals(Object o) {
		OccurrenceScoreItem si = (OccurrenceScoreItem) o;
		return (si.id == id);
	}
	
	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public int compareTo(OccurrenceScoreItem si) {
		
		if (occurrence != si.occurrence) {
			return si.occurrence - occurrence;
		} else if (score < si.score) {
			return 1;
		} else if (score > si.score)  {
			return -1;
		} else {
			return 0;
		}
	}
	

	
}
