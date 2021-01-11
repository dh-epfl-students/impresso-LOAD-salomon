package wikidatademo.graph;

import static settings.SystemSettings.stemmerLanguage;
import static settings.WebInterfaceStaticSettings.*;
import settings.WebInterfaceSettings;

import java.util.ArrayList;

import org.tartarus.snowball.SnowballStemmer;
import wikidatademo.logger.LogWriter;
import wikidatademo.logger.QueryException;

/**
 * LOAD graph for GUI Demo
 *
 * Template for different implementations (abstract class)
 *
 * Originally published November 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public abstract class LOADGraphAbstr {

	private SnowballStemmer stemmer;
	private LogWriter logger;
	protected WebInterfaceSettings settings;

	public LOADGraphAbstr(WebInterfaceSettings settings) {
		this.settings = settings;
		stemmer = getStemmer(stemmerLanguage);
		logger = new LogWriter(settings.logfileName);
	}

	public abstract void close();

	protected void closeLogger() {
		logger.closeFile();
	}

	protected synchronized String stemString(String input) {
		stemmer.setCurrent(input);
		stemmer.stem();
		String retval = stemmer.getCurrent();
		return retval;
	}

	// write a single String to the logfile
	public synchronized void addToLog(String logString) {
		logger.writeToFile(logString);
	}

	// write the content of an exception to logfile
	public synchronized void addErrorToLog(Throwable t) {
		logger.writeToFile(t);
	}

	/* retrieve a ranked list of entities (L, O, A, D) that match an input label
	 *
	 * label: input string to be matched
	 * number: max length of the result list */
	public abstract ArrayList<EntityInfoItem> getEntitiesByLabel(String label, int number, String user) throws QueryException;

	/* retrieve the internal ID of a term
	 * The original (unstemmed) input term is added as the description
	 *
	 * label: input string to be matched to a term */
	public abstract EntityInfoItem getTermByLabel(String label, String user) throws QueryException;

	/* Query the graph for a page ranking (P)
	 *
	 * queryEntities: list of entities to be used in the query
	 * number: max length of the result list */
	public abstract ArrayList<PageInfoItem> pageQuery(ArrayList<EntityQueryItem> queryEntities, int number, String user) throws QueryException;

	/* Query the graph for a sentence ranking (S)
	 *
	 * queryEntities: list of entities to be used in the query
	 * number: max length of the result list */
	public abstract ArrayList<SentenceInfoItem> sentenceQuery(ArrayList<EntityQueryItem> queryEntities, int number, String user) throws QueryException;

	/* Query the graph for an entity ranking (L, O, A, D, T)
	 *
	 * queryEntities: list of entities to be used in the query
	 * targetType : type of the result entities: (L, O, A, D, T) only!
	 * number: max length of the result list */
	public abstract ArrayList<EntityInfoItem> entityQuery(ArrayList<EntityQueryItem> queryEntities, String targetType, int number, String user) throws QueryException;

	/* Query the graph for an  subgraph that surrounds the query entities - only (L, O, A, D)
	 *
	 * queryEntities: list of entities to be used in the query
	 * nNeighbours: number of neighbours of each type for each node in the query */
	public abstract SubgraphItem subgraphQuery(ArrayList<EntityQueryItem> queryEntities, String user) throws QueryException;

}
