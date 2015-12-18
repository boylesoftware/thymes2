package org.bsworks.x2.util;


/**
 * Mutable integer.
 *
 * @author Lev Himmelfarb
 */
public class MutableInt {

	/**
	 * The value.
	 */
	private int value;


	/**
	 * Create mutable integer.
	 *
	 * @param initialValue Initial value.
	 */
	public MutableInt(final int initialValue) {

		this.value = initialValue;
	}


	/**
	 * Get value.
	 *
	 * @return The value.
	 */
	public int get() {

		return this.value;
	}

	/**
	 * Set value.
	 *
	 * @param value The value.
	 */
	public void set(final int value) {

		this.value = value;
	}

	/**
	 * Increment the value.
	 *
	 * @return Previous value (before the increment).
	 */
	public int increment() {

		return this.value++;
	}
}
