package EZShare;

public interface SubscriptionService<T> {

	public void subscribe(Subscriber<T> subscriber);
	
}
