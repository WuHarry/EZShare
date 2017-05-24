package EZShare;

public interface Subscriber<T> {

	public void notifySubscriber(SubscriptionService<T> subService);
	
	public void notifySubscriber(T obj);
	
}
