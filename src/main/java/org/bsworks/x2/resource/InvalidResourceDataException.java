package org.bsworks.x2.resource;


/**
 * Invalid resource data.
 *
 * @author Lev Himmelfarb
 */
public class InvalidResourceDataException
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
	public InvalidResourceDataException(final String message) {
		super(message);
	}

	/**
	 * Create new exception.
	 *
	 * @param message Error description.
	 * @param cause Error cause.
	 */
	public InvalidResourceDataException(final String message,
			final Throwable cause) {
		super(message, cause);
	}
}
