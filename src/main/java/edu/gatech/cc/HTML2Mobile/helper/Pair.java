package edu.gatech.cc.HTML2Mobile.helper;

/**
 * A simple pair of objects.
 * 
 * @param <One> the type of the first object
 * @param <Two> the type of the second object
 */
public class Pair<One, Two> {
	/**
	 * Make a pair with the types of the given variables.
	 * 
	 * @param <T1> the first type
	 * @param <T2> the second type
	 * @param one  the first object
	 * @param two  the second object
	 * @return the pair
	 */
	public static <T1, T2> Pair<T1, T2> makePair(T1 one, T2 two) {
		return new Pair<T1, T2>(one, two);
	}

	/** The first object. */
	private One one;
	/** The second object. */
	private Two two;

	/**
	 * Creates an immutable pair.
	 * 
	 * @param one the first object
	 * @param two the second object
	 */
	public Pair(One one, Two two) {
		this.one = one;
		this.two = two;
	}

	/**
	 * Returns a copy of this pair with the elements swapped.
	 * @return a swapped pair
	 */
	public Pair<Two, One> swapped() {
		return new Pair<Two, One>(two, one);
	}

	/**
	 * Returns the first object.
	 * @return the first object
	 */
	public One getOne() {
		return one;
	}

	/**
	 * Returns the second object.
	 * @return the second object
	 */
	public Two getTwo() {
		return two;
	}

	@Override
	public int hashCode() {
		final int FACTOR = 31;
		int hash = one == null ? 0 : one.hashCode();
		hash *= FACTOR;
		if( two != null ) {
			hash += two.hashCode();
		}
		return hash;
	}

	/** Pairs are equal if both members are equal. */
	@Override
	public boolean equals(Object obj) {
		if( this == obj ) {
			return true;
		}
		if( obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		Pair<?,?> that = (Pair<?,?>)obj;
		if( this.one == null ) {
			if( that.one != null ) {
				return false;
			}
		} else if ( !this.one.equals(that.one) ) {
			return false;
		}
		if( this.two == null ) {
			if( that.two != null ) {
				return false;
			}
		} else if( !this.two.equals(that.two) ) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return "Pair(" + one + ", " + two + ")";
	}
}
