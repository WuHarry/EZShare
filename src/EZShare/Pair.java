package EZShare;

public class Pair<T1, T2> {
	
	private T1 first;
	private T2 second;
	
	public Pair(T1 first, T2 second){
		if(first == null || second == null){
			throw new IllegalArgumentException("Cannot create Pair with null item. "
					                         + "first = " + first + ", second = " + second + ".");
		}
		this.first = first;
		this.second = second;
	}

	public T1 getFirst() {
		return first;
	}

	public T2 getSecond() {
		return second;
	}	
}
