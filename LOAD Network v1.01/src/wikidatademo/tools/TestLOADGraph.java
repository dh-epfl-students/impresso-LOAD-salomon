package wikidatademo.tools;
import wikidatademo.graph.*;

import java.util.ArrayList;

public class TestLOADGraph {

	public static void main(String[] args) {
		
		try {
			LOADGraphAbstr g = new LOADGraphImplDatabase();
	
			System.out.println("Testing entity retrieval by label");
			ArrayList<EntityInfoItem> l1 = g.getEntitiesByLabel("France", 6);
			for (EntityInfoItem eii : l1) {
				System.out.println(eii);
			}
			System.out.println();

			ArrayList<EntityInfoItem> l2 = g.getEntitiesByLabel("Australie", 10);
			for (EntityInfoItem eii : l2) {
				System.out.println(eii);
			}
			System.out.println();

			EntityInfoItem t1 = g.getTermByLabel("union");
			System.out.println(t1 + "\n");

			EntityInfoItem t2 = g.getTermByLabel("general");
			System.out.println(t2 + "\n");

			System.out.println("\nTesting PAG ranking function");
			ArrayList<EntityQueryItem> queryPAG = new ArrayList<EntityQueryItem>();
			queryPAG.add(new EntityQueryItem(0, "LOC")); // "Lausanne"
			queryPAG.add(new EntityQueryItem(457, "TER")); // add term "union"
//
			ArrayList<EntityInfoItem> rPAG = g.pageQuery(queryPAG, 8); // get (up to) 12 page that related to douglas adams
			for (EntityInfoItem eii : rPAG) {
				System.out.println(eii.toStringFormatPAG());
			}

			System.out.println("\nTesting SEN ranking function");
			ArrayList<EntityQueryItem> querySEN = new ArrayList<EntityQueryItem>();
			querySEN.add(new EntityQueryItem(0, "PERS")); // add M. Gladstone
			querySEN.add(new EntityQueryItem(13, "LOC")); // add Germany
			
			ArrayList<EntityInfoItem> rSEN = g.sentenceQuery(querySEN, 5); // get (up to) 12 sentences that related to douglas adams
			for (EntityInfoItem eii : rSEN) {
				System.out.println(eii.toStringFormatSEN());
			}
//			
			System.out.println("\nTesting ENTITY ranking function");
			ArrayList<EntityQueryItem> queryENT = new ArrayList<EntityQueryItem>();
			queryENT.add(new EntityQueryItem(10, "PERS")); // add Frederic Passy
//			
			ArrayList<EntityInfoItem> rENT = g.entityQuery(queryENT, "LOC", 10); // get (up to) 12 locations that related to Edouard Rod
			for (EntityInfoItem eii : rENT) {
				System.out.println(eii.toString());
			}
//			
			System.out.println("\nTesting ENTITY ranking function with two query entities");
			ArrayList<EntityQueryItem> queryENT2 = new ArrayList<EntityQueryItem>();
			queryENT2.add(new EntityQueryItem(15682, "PERS")); // add douglas adams (he has ID 15682 and is an actor)
			queryENT2.add(new EntityQueryItem(133, "ORG")); // add the BBC (has ID 133 and is an organization)
//			
			ArrayList<EntityInfoItem> rENT2 = g.entityQuery(queryENT2, "LOC", 10); // get (up to) 10 terms that related to query entities
			for (EntityInfoItem eii : rENT2) {
				System.out.println(eii.toString());
			}
//			
			System.out.println("\nTesting subgraph extraction function");
			ArrayList<EntityQueryItem> querySG = new ArrayList<EntityQueryItem>();
			querySG.add(new EntityQueryItem(15682, "PERS")); // add douglas adams (he has ID 15682 and is an actor)
			querySG.add(new EntityQueryItem(73, "LOC")); // add london (has ID 73 and is a location)
			querySG.add(new EntityQueryItem(9531, "ORG")); // add the BBC (has ID 9531 and is an organization)
			
			System.out.println("\nTesting subgraph extraction function");
			querySG = new ArrayList<EntityQueryItem>();
			querySG.add(new EntityQueryItem(1448, "TER"));
			querySG.add(new EntityQueryItem(115, "TER"));

			SubgraphItem sg = g.subgraphQuery(querySG);
			sg.printGraph();
			
			g.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

/* Entities:
 * ACT 868, Barack Obama
 * ORG 133, BBC
 * ACT 15682, Douglas Adams 
 */