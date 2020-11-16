# impresso LOAD

### Basic information

- Student : Julien Salomon
- Supervisors : Maud Ehrmann, Matteo Romanello, Andreas Spitz
- Academic year : 2020-2021 (autumn semester)

### Introduction

The goal of this project is to apply the  [LOAD](https://dbs.ifi.uni-heidelberg.de/resources/load/) model to the impresso corpus, adapting the LOAD implementation for the specific needs of the impresso corpus. The **impresso corpus** currently contains 76 newspapers from Switzerland and Luxembourg (written in French, German and Luxembourgish) for a total of 12 billion tokens. 164 million entity mentions were extracted from it, and linked to 500 thousand entities from DBpedia (partly mapped to Wikidata).  **LOAD** is a graph-based document model, developed by Andreas Spitz, that supports browsing, extracting and summarizing real world events in large collections of unstructured text based on named entities such as Locations, Organizations, Actors and Dates.

This adaptation includes the following changes: the addition of different entity types, sources of different languages(German, French and Luxembourgish), changing the context weight relation from sentence based to word distance based and adapting the database from MongoDB to Solr/MySQL.

### License  
**Impresso LOAD** - Julien Salomon
Copyright (c) 2020 EPFL
This program is licensed under the terms of the MIT license. 

### Program in the cluster
#### Run the program
cd impresso-LOAD-salomon/

(git checkout cluster)

git pull

screen -S impresso-LOAD -X nice -5 java -jar artifacts/impresso_LOAD_salomon_jar/impresso-LOAD-salomon.jar [args]

args: 
* "LIST OF NEWSPAPERS ACRONYMS" ==> get all issues for the listed newspapers
* "LIST OF NEWSPAPERS ACRONYMS" "YEARS FOR NEWSPAPER 1" "YEARS FOR NEWSPAPER 2"....
ex:
* "GDL, luxwort" "1900,1902" "1870,1871"
* "GDL"

#### Generate python graphs
*Output is required*

cd impresso-LOAD-salomon

python grapher/grapher.py

#### Play with Output

Output is in scratch/students/julien/Output


