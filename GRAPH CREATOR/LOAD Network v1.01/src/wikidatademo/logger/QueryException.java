package wikidatademo.logger;

/**
 * Custom exception for errors during query processing
 * May contain deflected original exceptions thrown during parsing.
 * 
 * Text description is retrievable with the function .getMessage()
 */
public class QueryException extends Exception {

	private static final long serialVersionUID = 1L;

	public QueryException() {}

    public QueryException(String message){
        super(message);
    }

    public QueryException(String message, Throwable cause){
        super(message, cause);
    }

}
