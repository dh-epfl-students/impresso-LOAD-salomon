package wikidatademo.users;

import wikidatademo.logger.QueryException;

public class UserToken {
	public final long startTime;	      // time this token was issued
	public final long expirationTime;	  // time when this token expires
	private volatile boolean abort;		  // abort toggle for this token
	
	public UserToken(long duration) {
		startTime = System.currentTimeMillis();
		expirationTime = startTime + duration;
		abort = false;
	}
	
	public void assertContinue() throws QueryException {
		if (abort) {
			String message = "Query aborted, queries per device exceeded.";
			throw(new QueryException(message));
		} else if (System.currentTimeMillis() > expirationTime) {
			String message = "Query timed out after " + (System.currentTimeMillis() - startTime) + "ms.";
			throw(new QueryException(message));
		}
	}
	
	public void abort() {
		abort = true;
	}
}
