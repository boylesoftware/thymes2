package org.bsworks.x2;


/**
 * Application initialization error.
 *
 * @author Lev Himmelfarb
 */
public class InitializationException
	extends Exception {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Create new exception.
	 *
	 * @param message Error description.
	 */
	public InitializationException(final String message) {
		super(message);
	}

	/**
	 * Create new exception.
	 *
	 * @param message Error description.
	 * @param cause Error cause.
	 */
	public InitializationException(final String message,
			final Throwable cause) {
		super(message, cause);
	}
}
