package wikidatademo.users;

import java.util.HashMap;
import java.util.LinkedList;

public class UserManagement {
	
	private HashMap<String, LinkedList<UserToken>> activeQueries;
	private int maxActiveQueries;
	private long maxComputeTimePerQuery;
	
	public UserManagement(int maxQueries, long maxComputeTime) {
		maxActiveQueries = maxQueries;
		maxComputeTimePerQuery = maxComputeTime;
		activeQueries = new HashMap<String, LinkedList<UserToken>>();
	}
	
	// get a new token for a new query
	public UserToken getToken(String user) {
		UserToken retval;
		
		synchronized(activeQueries) {
			LinkedList<UserToken> tokenList = activeQueries.get(user);
			if (tokenList == null) {
				tokenList = new LinkedList<UserToken>();
				activeQueries.put(user, tokenList);
			} else if (tokenList.size() >= maxActiveQueries) {
				UserToken token = getOldestTokenIndex(tokenList);
				token.abort(); // stop computation on this token
				tokenList.remove(token);
			} else {
				// if tokenList is no null and size is less than max, we can just add a new token
			}
			
			retval = new UserToken(maxComputeTimePerQuery);
			tokenList.add(retval);
			
			//System.out.println("+1 (" + tokenList.size() + " active tokens)");
		}
		
		return retval;
	}
	
	// return token after query is completed or timed out
	public void returnToken(String user, UserToken token) {
		synchronized(activeQueries) {
			LinkedList<UserToken> tokenList = activeQueries.get(user);
			if (tokenList != null) {
				
				// remove token from the list
				tokenList.remove(token);
				
				// remove list from the map if it is empty
				if (tokenList.isEmpty()) {
					activeQueries.remove(user);
				}
			}
			
			//System.out.println("-1 (" + tokenList.size() + " active tokens)");
			
		}
	}
	
	private UserToken getOldestTokenIndex(LinkedList<UserToken> tokenList) {
		long time = Long.MAX_VALUE;
		UserToken token = null;
		
		for (UserToken t : tokenList) {
			if (t.startTime < time) {
				time = t.startTime;
				token = t;
			}
		}
		return token;
	}
}
