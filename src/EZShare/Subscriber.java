package EZShare;

public interface Subscriber<T, Q> {

	public void notifySubscriber(SubscriptionService<T, Q> subService);
	
	public void notifySubscriber(T obj);
	
}
