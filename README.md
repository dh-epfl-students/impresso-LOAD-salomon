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

## Code Structure

In this part, the main aspects of the code will be described, with some notes on future improvements that could be made.

The main folders in the GitHub repository are: **GRAPH CREATOR** and **EVELIN IMPRESSO**. These two folders are 2 Java projects that work together.

### GRAPH CREATOR code

![GRAPH CREATOR pipeline](https://github.com/dhlab-epfl-students/impresso-LOAD-salomon/blob/master/GRAPH%20CREATOR/GRAPH%20CREATOR%20pipeline.png)

This folder contains code from the original LOAD network, created by Andreas Spitz. It is a good point of reference for how the LOAD network is constructed and is only there to compare with the rest of the code. 

#### External sort

Utility classes that are used to efficiently merge and sort the different edges files created.

#### Graph Query

This folder contains Java classes that can be used to debug a LOAD graph once it has been created. The *main* in the *GraphQueryInterface* Java file, starts a console that answers queries on the graph.

#### Impresso

This folder contains the main classes that can create the impresso-LOAD graph.

The main is contained in the **ParallelExtractNetworkFromImpresso** file.

The *S3Reader* class is the one querying the information contained in the S3 bucket: the **annotated words** for each article. If the boolean **TRANSFER_DUMP** is set to *true* it signifies that the **mentionned entities** will be queried from the transfer bucket (all mentions for a newspaper and a year) in a large compressed *jsonl* file. The expected, and final behavior of the code, will expect **TRANSFER_DUMP** to be *false*, but the boolean was added in case of testing. 

The *SolrReader* class is the one querying the information contained in the Solr database: the **text and mentionned entities** for each article, as well as the **entity information** for each mentionned entity. The way the code currently works, the *SolrReader* is called at the beginning of the code to query the id of all the articles required. These ids are stored in the following format: *local-files/NEWSPAPER_ACRONYM/YEAR.txt*. This could simply be by-passed and should be one of the first improvements that can be made to the code. The SolrReader only queries information about the entities if the **TRANSFER_DUMP** boolean is set to *false*.  
If **TRANSFER_DUMP** is set to *true* then make sure that the folder **GDL-mentions** is in the **GRAPH CREATOR** folder, and that the code is running for GDL for years 1890 and/or 1891, the only 2 available files for entity mentions locally.

**Entity**, **Token**, **Annotation**, **ImpressoContentItem** are classes used to represent elements of the final graph: ImpressoContentItem contains all of the necessary information from an article, as well as a list of **Token**s, each Token being either an **Entity** or a *term*. 

The **MultiThreadHubImpresso** is a hub used to manage the parallelization of the creation of the nodes and edges of the graph. It distributes to the many threads the id of the article they must extract the information from, and queries new information when it is required. As a note, two caches hold all the queried information for one year and one newspaper; when a year-newspaper combination is done another is queried and stored in the caches.

The **MultiThreadWorkerImpresso** class represent one thread managed by the hub described above. Each worker work and parallel. For each articleId, a worker takes its annotated words, and entities stored in Caches and create their nodes and edges in the graph.
In the workers, the bottleneck in terms of running time is the writing of the annotations edges (from line 302 to 347). There should be a way to optimize this aspect of the graph creation, and it should be explored in the future.

#### Settings

Contains static variables used throughout the code.

#### Wikidata demo

This folder contains different classes used to take the created graph, and load it into a MongoDB for it to be usable by the EVELIN interface. 
If the boolean **BUILD_MONGO_DB** is set to *true* then the loading of the graph onto a mongodb is activated when running **ParallelExtractNetworkFromImpresso**.

Once the graph is loaded into a mongo db, the wikidatademo.test folder contains a few tests to verify if the graph is well contstructed.

#### Testing
A few classes to test if the queriers (S3 + Solr) function properly.

### EVELIN IMPRESSO code

This codebase is the backend to the EVELIN interface. If a modification is made to the GRAPH CREATOR codebase, you must include a jar file containing the latest version of the project. Include the jar file into the *libs* folder of the EVELIN IMPRESSO project, and rename it **EVELIN_backend.jar**. As well, you must install this jar as a Maven dependency:
*mvn install:install-file –Dfile=libs/EVELIN_backend.jar -DgroupId=evelin -DartifactId=evelin -Dversion=1.0 -Dpackaging=jar*
(To install Maven check on this website: https://maven.apache.org/install.html. On MacOs you can use *brew install maven* on your terminal.)

## Run the code

### Run the graph creation code

To run the code it is required to give some arguments.
args: 
* "LIST OF NEWSPAPERS ACRONYMS" ==> get all issues for the listed newspapers
* "LIST OF NEWSPAPERS ACRONYMS" "YEARS FOR NEWSPAPER 1" "YEARS FOR NEWSPAPER 2"....
ex:
* "GDL, luxwort" "1900,1902, 1905" "1870,1871"
* "GDL"

Before running the code, set the **BUILD_MONGO_DB** variable to *true* in the *SystemSettings* file if you want the graph to be loaded to MongoDB once the graph is finished. Else you can do it independently afterwards.

### Load the graph on mongodb when the graph is already created
Load the *.txt* graph files into the *Output/graph_output* folder. Run the main class in **wikidatademo.dbcopy.MoveCompleteLOADNetworkToMongoDB**.


#### EVELIN interface
Once the dependencies are installed, you can run the project with the following steps:
- Build the project
- *mvn package*
- *mvn exec:java -Dexec.mainClass=controller.Controller -Dexec.args=settings.properties*
- Go to *localhost:1234* on any web browser and enjoy the usage of EVELIN interface.
(To install Maven check on this website: https://maven.apache.org/install.html. On MacOs you can use *brew install maven* on your terminal.)


#### Program in the cluster
To run the code in the cluster, set the **CLUSTER** boolean to *true* and create a jar file from the working version of the code. Once the jar file is loaded in the cluster, run it using the commands:
* screen -S impresso-LOAD
* nice -5 java -jar [*path_to_jar*]impresso-LOAD-salomon.jar [args]

Some folder predispositions are required to run the code, so it is better to leave the jar file inside of the project structure.

#### Generate python graphs
*Output is required*

cd impresso-LOAD-salomon

python grapher/grapher.py

#### Play with Output

Output is in scratch/students/julien/Output







