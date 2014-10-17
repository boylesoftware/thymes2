package org.bsworks.x2.services.persistence;


/**
 * Persistence transaction life-cycle handler.
 *
 * <p>The handler must be closed by the application after it is no longer
 * needed. This is usually done in a {@code finally} block. When it is closed,
 * the handled transaction is automatically rolled back, unless
 * {@link #commitTransaction()} was called.
 *
 * @author Lev Himmelfarb
 */
public interface PersistenceTransactionHandler
	extends AutoCloseable {

	/**
	 * Get transaction handled by the handler.
	 *
	 * @return The transaction.
	 */
	PersistenceTransaction getTransaction();

	/**
	 * Commit the handled transaction. After this method is called, the
	 * transaction returned by the {@link #getTransaction()} method becomes
	 * unusable. Thus, the only operation that can be made on the handler after
	 * committing the transaction is closing it.
	 *
	 * @throws IllegalStateException If already committed.
	 */
	void commitTransaction();

	/**
	 * Overridden method from {@link AutoCloseable} that does not throw any
	 * checked exceptions.
	 */
	@Override
	void close();
}
