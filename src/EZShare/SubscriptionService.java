package EZShare;

import java.util.List;

import Exceptions.InvalidResourceException;
/**
 * A subscription service allowing subscribers to ask for notifications and query for a particular object in the database.
 *
 * @param <T> The type of the objects in database.
 * @param <Q> The type used to query the database.
 */
public interface SubscriptionService<T, Q> {

	/**
	 * 
	 * @param subscriber
	 */
	public void subscribe(Subscriber<T, Q> subscriber);
	
	/**
	 * 
	 * @param object
	 * @return
	 * @throws InvalidResourceException The query describes an invalid database object. 
	 */
	public List<T> query(Q object) throws InvalidResourceException;
	
}
