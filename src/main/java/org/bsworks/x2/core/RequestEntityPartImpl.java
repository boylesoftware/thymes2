package org.bsworks.x2.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Part;

import org.bsworks.x2.RequestEntityPart;


/**
 * Request entity part implementation.
 *
 * @author Lev Himmelfarb
 */
class RequestEntityPartImpl
	implements RequestEntityPart {

	/**
	 * Pattern for extracting character set from the content type value.
	 */
	private static final Pattern CHARSET_PATTERN =
		Pattern.compile(";\\s*charset\\s*=\\s*([^;]+)");


	/**
	 * HTTP request part.
	 */
	private final Part part;


	/**
	 * Create new request entity part object.
	 *
	 * @param part HTTP request part.
	 */
	RequestEntityPartImpl(final Part part) {

		this.part = part;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getContentType() {

		return this.part.getContentType();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getCharacterEncoding() {

		final String ctype = this.part.getContentType();
		if (ctype == null)
			return null;

		final Matcher m = CHARSET_PATTERN.matcher(ctype);
		if (!m.find())
			return null;

		return m.group(1);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public long getSize() {

		return this.part.getSize();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public InputStream getInputStream()
		throws IOException {

		return this.part.getInputStream();
	}
}
