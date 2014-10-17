package org.bsworks.x2.services.persistence;


/**
 * Unexpected persistent storage (the database) exception.
 *
 * @author Lev Himmelfarb
 */
public class PersistenceException
	extends RuntimeException {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Create new exception.
	 *
	 * @param message Error description.
	 */
	public PersistenceException(final String message) {
		super(message);
	}

	/**
	 * Create new exception.
	 *
	 * @param cause Error cause.
	 */
	public PersistenceException(final Throwable cause) {
		super("Database error.", cause);
	}

	/**
	 * Create new exception.
	 *
	 * @param message Error description.
	 * @param cause Error cause.
	 */
	public PersistenceException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
