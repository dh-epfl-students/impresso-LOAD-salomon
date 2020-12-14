package wikidatademo.test;



import wikidatademo.graph.EntityInfoItem;
import wikidatademo.graph.LOADGraphAbstr;
import wikidatademo.graph.LOADGraphImplDatabase;

import java.util.ArrayList;

public class TestEntityLookup {

	public static void main(String[] args) {
		
		try {
			LOADGraphAbstr g = new LOADGraphImplDatabase();
			
			String searchTerm = "Paris";
	
			System.out.println("Basic search: " + searchTerm);
			ArrayList<EntityInfoItem> l1 = g. getEntitiesByLabel(searchTerm, 10);
			for (EntityInfoItem eii : l1) {
				System.out.println(eii);
			}
			System.out.println();

			String subTerm = searchTerm.substring(0, searchTerm.length()-1);
			System.out.println("SubTerm search: " + subTerm);
			ArrayList<EntityInfoItem> l2 = g.getEntitiesByLabel(subTerm, 10);
			for (EntityInfoItem eii : l2) {
				System.out.println(eii);
			}

			String phrase = "\"" + searchTerm +"\"";
			System.out.println("\nPhrase search: " + phrase);

			ArrayList<EntityInfoItem> l3 = g.getEntitiesByLabel(phrase, 10);
			for (EntityInfoItem eii : l3) {
				System.out.println(eii);
			}
			System.out.println();

			String subPhrase = "\"" + searchTerm.substring(0, searchTerm.length()-1) +"\"";
			System.out.println("SubTermPhrase : " + subPhrase);
			ArrayList<EntityInfoItem> l4 = g.getEntitiesByLabel(subPhrase, 10);
			for (EntityInfoItem eii : l4) {
				System.out.println(eii);
			}
			System.out.println();
			
			g.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
