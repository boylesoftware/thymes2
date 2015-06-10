package org.bsworks.x2.core;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.bsworks.x2.RequestEntityPart;


/**
 * Wrapper for an HTTP request to represent its complete entity as a part.
 *
 * @author Lev Himmelfarb
 */
class CompleteRequestEntity
	implements RequestEntityPart {

	/**
	 * The HTTP request.
	 */
	private final HttpServletRequest httpRequest;


	/**
	 * Wrap a request.
	 *
	 * @param httpRequest The HTTP request.
	 */
	CompleteRequestEntity(final HttpServletRequest httpRequest) {

		this.httpRequest = httpRequest;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getContentType() {

		return this.httpRequest.getContentType();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getCharacterEncoding() {

		return this.httpRequest.getCharacterEncoding();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public long getSize() {

		return this.httpRequest.getContentLengthLong();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public InputStream getInputStream()
		throws IOException {

		return this.httpRequest.getInputStream();
	}
}
