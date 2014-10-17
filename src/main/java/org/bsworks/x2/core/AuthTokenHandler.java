package org.bsworks.x2.core;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.Actor;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.util.Base64;


/**
 * Collection of methods used to handle the "AuthToken" HTTP authentication
 * scheme.
 *
 * @author Lev Himmelfarb
 */
class AuthTokenHandler {

	/**
	 * Pattern used to parse the "Authorization" header.
	 */
	private static final Pattern AUTH_HDR_EL_PATTERN =
		Pattern.compile(
				"(?:^\\s*AuthToken\\s+|\\G\\s*,\\s*)"
				+ "([^=\\s]*)\\s*=\\s*([^,\\s]*)");


	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Authentication token time-to-live in milliseconds.
	 */
	private final long authTokenTTL;

	/**
	 * Actor resolver.
	 */
	private final CachingAuthResolver authResolver;


	/**
	 * Create new handler.
	 *
	 * @param sc Servlet context.
	 * @param runtimeCtx Runtime context.
	 *
	 * @throws InitializationException If not configured correctly.
	 */
	AuthTokenHandler(final ServletContext sc,
			final RuntimeContextImpl runtimeCtx)
		throws InitializationException {

		this.authTokenTTL = Long.parseLong(
				sc.getInitParameter("x2.auth.tokenTTL"));

		this.authResolver = new CachingAuthResolver(sc, runtimeCtx);
	}


	/**
	 * Get actor making the specified HTTP request.
	 *
	 * @param httpRequest The HTTP request.
	 *
	 * @return The actor, or {@code null} if the request does not contain
	 * authentication information, or the authentication information is invalid,
	 * expired, or refers to a nonexistent or inactive actor.
	 */
	Actor getActor(final HttpServletRequest httpRequest) {

		final boolean debug = this.log.isDebugEnabled();

		// get authorization header
		final String authHeader = httpRequest.getHeader("Authorization");
		if (authHeader == null) {
			if (debug)
				this.log.debug("no Authorization header");
			return null;
		}

		// parse authorization header
		final Matcher m = AUTH_HDR_EL_PATTERN.matcher(authHeader);
		String username = null;
		String opaque = null;
		String token = null;
		while (m.find()) {
			switch (m.group(1)) {
			case "username":
				username = m.group(2);
				break;
			case "opaque":
				opaque = m.group(2);
				break;
			case "token":
				token = m.group(2);
				break;
			}
		}
		if ((username == null) || (token == null)) {
			if (debug)
				this.log.debug("invalid Authorization header value");
			return null;
		}
		if (debug)
			this.log.debug("decrypting Authorization header: username="
					+ username + ", opaque=" + opaque + ", token=" + token);

		// get actor record
		final Actor actor = this.authResolver.getActor(username, opaque);
		if (actor == null) {
			if (debug)
				this.log.debug("actor " + username + " does not exist");
			return null;
		}

		// get token bytes
		final ByteBuffer cipherBuf;
		try {
			final int base64Len = token.length();
			final char[] base64Chars = new char[base64Len];
			token.getChars(0, base64Len, base64Chars, 0);
			final CharBuffer base64Buf = CharBuffer.wrap(base64Chars);
			cipherBuf = ByteBuffer.allocate((base64Len * 6) / 8 + 1);
			Base64.decode(base64Buf, cipherBuf);
			cipherBuf.flip();
		} catch (final IndexOutOfBoundsException e) {
			if (debug)
				this.log.debug("token decryption error", e);
			return null;
		}

		// decrypt the token
		final SecretKey secretKey = actor.getSecretKey();
		final Cipher cipher;
		try {
			cipher = Cipher.getInstance(secretKey.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException("Error getting a cipher.", e);
		}
		final ByteBuffer clearBuf = ByteBuffer.allocate(
				cipher.getOutputSize(cipherBuf.remaining()));
		try {
			cipher.doFinal(cipherBuf, clearBuf);
			clearBuf.flip();
		} catch (final GeneralSecurityException e) {
			if (debug)
				this.log.debug("token decryption error", e);
			return null;
		}

		// get and verify the token expiration
		final long tokenTS;
		try {
			tokenTS = clearBuf.getLong();
		} catch (final BufferUnderflowException e) {
			if (debug)
				this.log.debug("invalid token", e);
			return null;
		}
		final long now = System.currentTimeMillis();
		if ((tokenTS < (now - this.authTokenTTL)) || (tokenTS > now)) {
			if (debug)
				this.log.debug("token timestamp out of range");
			return null;
		}

		// verify credentials
		if (clearBuf.compareTo(ByteBuffer.wrap(actor.getCredentials())) != 0) {
			if (debug)
				this.log.debug("credentials do not match");
			return null;
		}

		// success
		return actor;
	}

	/**
	 * Add authentication information to the HTTP response.
	 *
	 * @param httpResponse The HTTP response.
	 * @param actor The authenticated actor.
	 */
	void addAuthInfo(final HttpServletResponse httpResponse,
			final Actor actor) {

		// assemble authentication token value
		final byte[] credentials = actor.getCredentials();
		final ByteBuffer clearBuf = ByteBuffer.allocate(8 + credentials.length);
		clearBuf.putLong(System.currentTimeMillis());
		clearBuf.put(credentials);
		clearBuf.flip();

		// encrypt the token
		final SecretKey secretKey = actor.getSecretKey();
		final ByteBuffer cipherBuf;
		try {
			final Cipher cipher = Cipher.getInstance(secretKey.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			cipherBuf = ByteBuffer.allocate(
					cipher.getOutputSize(clearBuf.remaining()));
			cipher.doFinal(clearBuf, cipherBuf);
			cipherBuf.flip();
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException("Encryption error.", e);
		}

		// encode the token as Base64
		final CharBuffer base64Buf = CharBuffer.allocate(
				(cipherBuf.remaining() * 8) / 6 + 3);
		Base64.encode(cipherBuf, base64Buf);
		base64Buf.flip();
		final String token = base64Buf.toString();

		// add authentication info header
		httpResponse.setHeader("Authentication-Info", "nexttoken=" + token);
	}
}
