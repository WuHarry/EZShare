package EZShare;

/**
 * Interface for a class which subscribes to a database which can be queried and notifies subscriber when 
 * events occur which may motivate a query.
 *
 * @param <T> Type of object stored in database.
 * @param <Q> Type of object used to query.
 */
public interface Subscriber<T, Q> {

	/**
	 * Notify a subscriber that it should query the entire subService.
	 * @param subService The SubscriptionService to be queried by the subscriber.
	 */
	public void notifySubscriber(SubscriptionService<T, Q> subService);
	
	/**
	 * Notify subscriber of a particular object which it may be interested in.
	 * @param obj The objct it may be interested in.
	 */
	public void notifySubscriber(T obj);
	
}
