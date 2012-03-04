package edu.gatech.cc.HTML2Mobile.helper;

/**
 * Immutable tuple, meant for use as a map key, set member, etc.
 */
public class Tuple {
	private final Object[] items;

	public Tuple(Object... items) {
		this.items = new Object[items.length];
		System.arraycopy(items, 0, this.items, 0, items.length);
	}

	/**
	 * Gets the item at the given index.
	 * @param index the index
	 * @return the item
	 * @throws IndexOutOfBoundsException
	 */
	public Object get(int index) {
		return items[index];
	}

	/**
	 * Returns the number of items in this tuple.
	 * @return the number of items
	 */
	public int size() {
		return items.length;
	}

	@Override
	public int hashCode() {
		final int FACTOR = 31;
		int hash = 0;

		for( Object item : items ) {
			hash *= FACTOR;
			hash += item == null ? 0 : item.hashCode();
		}

		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if( obj == this ) {
			return true;
		}
		if( obj == null || !obj.getClass().equals(getClass()) ) {
			return false;
		}
		Tuple that = (Tuple)obj;
		if( this.size() != that.size() ) {
			return false;
		}

		for( int i = 0; i < items.length; ++i ) {
			Object thisItem = this.items[i];
			Object thatItem = that.items[i];
			if( thisItem == null ) {
				if( thatItem != null ) {
					return false;
				}
			} else if( thatItem == null || !thisItem.equals(thatItem) ) {
				return false;
			}
		}

		return true;
	}

}
