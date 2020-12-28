import pandas as pd
import networkx as nx
import matplotlib.pyplot as plt
import random
import math

sep_char = "\t"
PATH = "Output/graph_output/"

vertices = pd.DataFrame(columns=['type', 'name', 'num', 'loc_deg', 'act_deg', 'term_deg', 'page_deg', 'sent_deg'])
def fill_vertice_df(file, typ):
    df = pd.DataFrame(columns=['type', 'name', 'num', 'loc_deg', 'act_deg', 'term_deg', 'page_deg', 'sent_deg'])
    f = open(file)
    cnt = 0
    for line in f:
        dic = {'type': typ, 'num': cnt}
        cnt += 1
        
        vertice = line.split(sep_char)
        dic['name'] = vertice[0]
        dic['loc_deg'] = int(vertice[2])
        dic['act_deg'] = int(vertice[3])
        dic['term_deg'] = int(vertice[5])
        dic['page_deg'] = int(vertice[6])
        dic['sent_deg'] = int(vertice[7])
        df = df.append(dic, ignore_index=True)
    f.close()
    return df
files = [PATH + "vACT.txt", PATH + "vLOC.txt", PATH + "vPAG.txt", PATH + "vSEN.txt", PATH + "vTER.txt"]
types = ["PERS", "LOC", "PAG", "SEN", "TER"]
for i in range(len(files)):
    vertices = vertices.append(fill_vertice_df(files[i], types[i]), ignore_index=True)
vertices['id'] = vertices['type'] + "_" + vertices['num'].astype(str)
vertices = vertices.set_index('id')

edges = pd.DataFrame(columns=['from_type','from', 'to_type', 'to', 'weight'])
def fill_edge_df(file, typ):
    f = open(file)
    df = pd.DataFrame(columns=['from_type','from', 'to_type', 'to', 'weight'])
    cnt = 0
    from_id = 0
    for l in f:
        if cnt == 0:
            from_id = int(l)
        elif cnt == 8:
            cnt = 0
        else:
            dic = {'from_type':typ, 'from': from_id}
            values = l.split("\t")
            dic['to_type'] = values[0]
            ind = 1
            while ind < len(values):
                dic['to'] = int(values[ind])
                dic['weight'] = float(values[ind+1])
                df = df.append(dic, ignore_index=True)
                ind += 2
        cnt += 1
    f.close() 
    return df

files = [PATH + "eACT.txt",PATH + "eLOC.txt",PATH + "ePAG.txt",PATH + "eSEN.txt",PATH + "eTER.txt"]
types = ["PERS", "LOC", "PAG", "SEN", "TER"]

for i in range(len(files)):
    edges = edges.append(fill_edge_df(files[i], types[i]), ignore_index=True)
edges['from_id'] = edges['from_type'] + "_" + edges['from'].astype(str)
edges['to_id'] = edges['to_type'] + "_" + edges['to'].astype(str)

graph = nx.from_pandas_edgelist(edges, 'from_id', 'to_id', 'weight')

nx.set_node_attributes(graph, vertices['name'].to_dict(), 'name')
nx.set_node_attributes(graph, vertices['type'].to_dict(), 'type')

colors = ['red', 'blue', 'black', 'grey', 'green']
type_color_map = dict(zip(types, colors))

edge_colors = {}
for e in graph.edges:
    f, t = e
    f_type, t_type = graph.node[f]['type'], graph.node[t]['type']
    if f_type == t_type:
        edge_colors[e] = type_color_map[f_type]
    elif (f_type == 'LOC' or f_type == 'PERS') and (t_type == 'LOC' or t_type == 'PERS'):
        edge_colors[e] = 'purple'
    elif f_type == 'LOC' or f_type == 'PERS':
        edge_colors[e] = type_color_map[f_type]
    elif f_type == 'TER' or t_type == 'TER':
        edge_colors[e] = 'green'
    elif f_type == 'SEN' or t_type == 'SEN':
        edge_colors[e] = 'grey'
    elif f_type == 'PAG' or t_type == 'PAG':
        edge_colors[e] = 'black'
    else:
        edge_colors[e] = 'grey'
        

plt.figure(figsize=(20,20)) 

    
pos = nx.circular_layout(graph, scale=1)
tags = nx.circular_layout(graph, scale=1)


radii = [10,30,60,120,150, 180]  # for concentric circles

range = 10

for n in pos.keys():
    new_r = 1
    if graph.node[n]['type'] == 'TER':
        new_r = radii[3]
    elif graph.node[n]['type'] == 'SEN':
        new_r = radii[4]
    elif graph.node[n]['type'] == 'PERS':
        new_r = radii[2]
    elif graph.node[n]['type'] == 'LOC':
        new_r = radii[2]
    else:
        new_r = radii[5]
    if random.random() < 0.5:
        new_r += random.random() * 10
    else:
        new_r -= random.random() * 10
    angle = 360 * random.random()
    x,y = new_r * math.cos(math.radians(angle)), new_r * math.sin(math.radians(angle))
    x_t,y_t = (new_r + 0.1) * math.cos(math.radians(angle)), (new_r+0.1) * math.sin(math.radians(angle))
    pos[n] = x,y
    tags[n] = x_t,y_t


ec = nx.draw_networkx_edges(graph, pos, edge_color=edge_colors.values(), alpha=0.2)
label = nx.get_node_attributes(graph, 'name')
nc = nx.draw_networkx_nodes(graph, pos, node_color=[type_color_map[graph.node[n]['type']] for n in graph.nodes], node_size=100, alpha=0.5)
lc = nx.draw_networkx_labels(graph, tags, labels=label)
plt.savefig("Output/Graph_circ.png", format="PNG")


plt.figure(figsize=(20,20)) 
pos = nx.spring_layout(graph, k=5, scale=1)
tags = nx.spring_layout(graph, k=5, scale=1.1)

lab_pos = pos
for l in pos:  # raise text positions
    lab_pos[l][1] += 5  # probably small value enough
    

ec = nx.draw_networkx_edges(graph, pos, edge_color=edge_colors.values(), alpha=0.3)
label = nx.get_node_attributes(graph, 'name')
nc = nx.draw_networkx_nodes(graph, pos, node_color=[type_color_map[graph.node[n]['type']] for n in graph.nodes], node_size=100, alpha=0.5)
lc = nx.draw_networkx_labels(graph, lab_pos, labels=label)
plt.savefig("Output/Graph_spring.png", format="PNG")
print("DONE")