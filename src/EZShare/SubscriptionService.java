package EZShare;

import java.util.List;

public interface SubscriptionService<T, Q> {

	public void subscribe(Subscriber<T, Q> subscriber);
	
	public List<T> query(Q object);
	
}
