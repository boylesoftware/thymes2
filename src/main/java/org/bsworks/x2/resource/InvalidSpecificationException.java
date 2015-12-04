package org.bsworks.x2.resource;


/**
 * Invalid filter, properties fetch or order specification.
 *
 * @author Lev Himmelfarb
 */
public class InvalidSpecificationException
	extends IllegalArgumentException {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Create new exception.
	 *
	 * @param message Error description.
	 */
	public InvalidSpecificationException(final String message) {
		super(message);
	}

	/**
	 * Create new exception.
	 *
	 * @param message Error description.
	 * @param cause Error cause.
	 */
	public InvalidSpecificationException(final String message,
			final Throwable cause) {
		super(message, cause);
	}
}
