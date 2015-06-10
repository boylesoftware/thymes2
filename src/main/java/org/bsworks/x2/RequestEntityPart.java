package org.bsworks.x2;

import java.io.IOException;
import java.io.InputStream;


/**
 * Represents and HTTP request part entity used with multipart HTTP requests.
 *
 * @author Lev Himmelfarb
 */
public interface RequestEntityPart {

	/**
	 * Get content type.
	 *
	 * @return Content type, or {@code null} if unavailable.
	 */
	String getContentType();

	/**
	 * Get character encoding.
	 *
	 * @return Character encoding, or {@code null} if unavailable.
	 */
	String getCharacterEncoding();

	/**
	 * Get content size.
	 *
	 * @return Size in bytes.
	 */
	long getSize();

	/**
	 * Get input stream for reading the content.
	 *
	 * @return Input stream.
	 *
	 * @throws IOException If an error happens.
	 */
	InputStream getInputStream()
		throws IOException;
}
