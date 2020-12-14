mongoexport -h 127.0.0.1:27017 -d IMPRESSO_LOAD_FINAL -c metaData -o metaData.json --jsonArray
mongoexport -h 127.0.0.1:27017 -d IMPRESSO_LOAD_FINAL -c edgesPlusPages -o edgesPlusPages.json --jsonArray
mongoexport -h 127.0.0.1:27017 -d IMPRESSO_LOAD_FINAL -c nodesDegrees -o nodesDegrees.json --jsonArray
mongoexport -h 127.0.0.1:27017 -d IMPRESSO_LOAD_FINAL -c nodesPages -o nodesPages.json --jsonArray
mongoexport -h 127.0.0.1:27017 -d IMPRESSO_LOAD_FINAL -c nodesEntities -o nodesEntities.json --jsonArray
mongoexport -h 127.0.0.1:27017 -d IMPRESSO_LOAD_FINAL -c nodesSentences -o nodesSentences.json --jsonArray
mongoexport -h 127.0.0.1:27017 -d IMPRESSO_LOAD_FINAL -c nodesTerms -o nodesTerms.json --jsonArray
