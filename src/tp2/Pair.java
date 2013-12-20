
package tp2;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The Pair classe represents a Pair in which exists two elements
 */
public class Pair<T,O> {
	private T first; //first member of pair
    private O second; //second member of pair

    /**
     * Constructs a Pair
     * @param first first element
     * @param second second element
     */
    public Pair(T first, O second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Changes first element
     * @param first new first element
     */
    public void setFirst(T first) {
        this.first = first;
    }

    /**
     * Changes second element
     * @param second new second element
     */
    public void setSecond(O second) {
        this.second = second;
    }

    /**
     * Returns first element
     * @return first element
     */
    public T getFirst() {
        return first;
    }

    /**
     * Returns second element
     * @return second element
     */
    public O getSecond() {
        return second;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		return true;
	}  
}
