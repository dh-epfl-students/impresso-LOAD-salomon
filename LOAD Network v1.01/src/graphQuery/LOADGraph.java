package graphQuery;

import static settings.LOADmodelSettings.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * LOAD graph
 * 
 * Constructor builds the complete graph from an input directory.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class LOADGraph {

    public int[] nNodes;
    public GraphNode[][] nodeLists;
    public ArrayList<HashMap<String,Integer>> namesToIdMaps;
    
    public LOADGraph(String inputFolderPath) {
        nodeLists = new GraphNode[nANNOTATIONS][];
        namesToIdMaps = new ArrayList<HashMap<String,Integer>>(nANNOTATIONS);
        
        HashMap<String,Integer> setNamesToIndexMap = new HashMap<String,Integer>();
        for (int i=0; i < nANNOTATIONS; i++) {
            setNamesToIndexMap.put(setNames[i], i);
        }
        
        if (!inputFolderPath.endsWith(File.separator)) {
            inputFolderPath = inputFolderPath + File.separator;
        }
        
        try {
            long time = System.currentTimeMillis();
            String line;
            nNodes = new int[nANNOTATIONS];
            
            // read metadata from file (includes number of vertices of different types)
            String filename = inputFolderPath + metaFileName; 
            //System.out.println("Reading graph meta data from:"  + filename);
            System.out.println("Reading graph meta data");
            BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
            while ((line = br.readLine()) != null) {
                if (line.startsWith(commentChar)) continue;
                
                String[] splitline = line.split(sepChar);
                for(String s: splitline)
                    System.out.println(s);
                Integer set = setNamesToIndexMap.get(splitline[0]);
                if (set != null) {
                    nNodes[set] = Integer.parseInt(splitline[1]);
                } else {
                    System.out.println("Unknown line specificer in metadata file:");
                    System.out.println(line);
                }
            }            
            br.close();
            
            // sanity check: are any vertex sets of size 0?
            for (int i=0; i < nANNOTATIONS; i++) {
                if (nNodes[i] == 0) {
                    System.out.println("Warning! Number of nodes is 0 for "  + setNames[i]);
                }
            }
            
            // allocate memory for hashmaps of vertex names to ids (set size to prevent rehashing during reading)
            for (int i=0; i < nANNOTATIONS; i++) {
                namesToIdMaps.add(new HashMap<String,Integer>((int) Math.ceil(nNodes[i] / 0.73), 0.75f));
            }

            // read vertex information
            for (int i=0; i < nANNOTATIONS; i++) {
                filename = inputFolderPath + vertexFileNames[i];
                //System.out.println("Reading vertex information from:"  + filename);
                System.out.print("Reading nodes of type"  + setNames[i]);
                int index = 0;
                int steps = nNodes[i]/100;
                if (steps == 0) steps = 1;
                nodeLists[i] = new GraphNode[nNodes[i]];
                HashMap<String,Integer> idmap = namesToIdMaps.get(i);
                GraphNode[] nodes = nodeLists[i];
                br = new BufferedReader(new FileReader(new File(filename)));
                while ((line = br.readLine()) != null) {
                    if (index % steps == 0) {
                        int progress = (int) (100.0*index/nNodes[i]);
                        System.out.print("\rReading nodes of type"  + setNames[i] +" :" + String.format("%1$" + 3 +"s",  progress) +"%");
                    }
                    String[] splitline = line.split(sepChar);
                    String value = splitline[0];
                    int[] degs = new int[nANNOTATIONS];
                    degs[DAT] = Integer.parseInt(splitline[1]);
                    degs[LOC] = Integer.parseInt(splitline[2]);
                    degs[ACT] = Integer.parseInt(splitline[3]);
                    degs[ORG] = Integer.parseInt(splitline[4]);
                    degs[TER] = Integer.parseInt(splitline[5]);
                    degs[PAG] = Integer.parseInt(splitline[6]);
                    degs[SEN] = Integer.parseInt(splitline[7]);
                    nodes[index] = new GraphNode(i, index, value, degs);
                    idmap.put(value, index);
                    ++index;
                }
                System.out.println("\rReading nodes of type "  + setNames[i] +" : done");
                br.close();
            }
            
            // read edge information
            for (int i=0; i < nANNOTATIONS; i++) {
                if (nodeLists[i].length > 0) {
                    filename = inputFolderPath + edgeFileNames[i];
                    //System.out.println("Reading edge information from:"  + filename);
                    System.out.print("Reading edges of type " + setNames[i]);
                    int index = 0;
                    int steps = nNodes[i] / 100;
                    if (steps == 0) steps = 1;
                    GraphNode[] nodes = nodeLists[i];
                    br = new BufferedReader(new FileReader(new File(filename)));
                    do {
                        if (index % steps == 0) {
                            int progress = (int) (100.0 * index / nNodes[i]);
                            System.out.print("\rReading edges of type " + setNames[i] + " :" + String.format("%1$" + 3 + "s", progress) + "%");
                        }
                        line = br.readLine();
                        GraphNode node = nodes[index];
                        for (int k = 0; k < nANNOTATIONS; k++) {
                            String[] splitline = br.readLine().split(sepChar);
                            int arrayIndex = 0;
                            for (int lineIndex = 1; lineIndex < splitline.length; ) {
                                node.adjacency[k][arrayIndex] = Integer.parseInt(splitline[lineIndex++]);
                                node.weights[k][arrayIndex] = Float.parseFloat(splitline[lineIndex++]);
                                ++arrayIndex;
                            }
                        }
                        ++index;
                    } while ((line = br.readLine()) != null);
                    System.out.println("\rReading edges of type " + setNames[i] + " : done");
                    br.close();

                }
            }
            
            time = System.currentTimeMillis() - time;
            System.out.println("Done reading graph. Time needed:"  + String.format("%.3f",  time / (1000 * 60.0)) +" minutes.");
            
        } catch (Exception e) {
            System.out.println("Unable to read graph from files");
            e.printStackTrace();
        }
    }
    
    public void print() {
        for (int i=0; i < nANNOTATIONS; i++) {
            GraphNode[] currentList = nodeLists[i];
            System.out.println("Node type:"  + setNames[i]);
            for (int s=0; s < currentList.length; s++) {
                GraphNode currentNode = currentList[s];
                System.out.println("ID:" + currentNode.id +" , label:"  + currentNode.value);
                for (int j=0; j < nANNOTATIONS; j++) {
                    System.out.println("Degrees for "  + setNames[j] +" :" + currentNode.degrees[j]);
                    for (int iter=0; iter<currentNode.adjacency[j].length; iter++) {
                        System.out.println("->"  + nodeLists[j][currentNode.adjacency[j][iter]].value +"  w=" + currentNode.weights[j][iter]);
                    }
                }
                //System.out.println();
            }
            //System.out.println();
        }
    }
    
    public void printEdgeCounts() {
        System.out.println("\nEdge connectivity matrix");
        System.out.println("              DAT           LOC           ACT           ORG           TER           PAG           SEN");
        for (int i=0; i < nANNOTATIONS; i++) {
            long[] edgeCounts = new long[nANNOTATIONS];
            GraphNode[] currentList = nodeLists[i];
            for (int s=0; s < currentList.length; s++) {
                GraphNode currentNode = currentList[s];
                for (int j=0; j < nANNOTATIONS; j++) {
                    edgeCounts[j] += currentNode.degrees[j];
                }
            }
            System.out.print(setNames[i]+"");
            for (int j=0; j < nANNOTATIONS; j++) {
                System.out.print(String.format("%1$14s",  Long.toString(edgeCounts[j])));
            }
            //System.out.println();
        }
    }
}
