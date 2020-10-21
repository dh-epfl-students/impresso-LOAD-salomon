# impresso LOAD

### Basic information

- Student : Isabelle Pumford
- Supervisors : Maud Ehrmann and Matteo Romanello
- Academic year : 2019-2020 (spring semester)

### Introduction

The goal of this project is to apply the  [LOAD](https://dbs.ifi.uni-heidelberg.de/resources/load/) model to the impresso corpus, adapting the LOAD implementation for the specific needs of the impresso corpus. The **impresso corpus** currently contains 76 newspapers from Switzerland and Luxembourg (written in French, German and Luxembourgish) for a total of 12 billion tokens. 164 million entity mentions were extracted from it, and linked to 500 thousand entities from DBpedia (partly mapped to Wikidata).  **LOAD** is a graph-based document model, developed by Andreas Spitz, that supports browsing, extracting and summarizing real world events in large collections of unstructured text based on named entities such as Locations, Organizations, Actors and Dates.

This adaptation includes the following changes: the addition of different entity types, sources of different languages(German, French and Luxembourgish), changing the context weight relation from sentence based to word distance based and adapting the database from MongoDB to Solr/MySQL.

### License  
**Impresso LOAD** - Isabelle Pumford
Copyright (c) 2020 EPFL
This program is licensed under the terms of the MIT license. 