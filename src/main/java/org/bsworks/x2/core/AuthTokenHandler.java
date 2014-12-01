package org.bsworks.x2.core;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.util.Base64;
import org.bsworks.x2.util.Hex;


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
	private static final Pattern AUTH_HDR_EL_PATTERN = Pattern.compile(
			"^\\s*AuthToken\\s+"
			+ "([A-Za-z0-9+/]+={0,2})\\.([A-Za-z0-9+/]+={0,2})\\s*$");

	/**
	 * UTF-8 charset.
	 */
	private static final Charset UTF8 = Charset.forName("UTF-8");


	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Application-wide secret key.
	 */
	private final SecretKey appSecretKey;

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

		try {
			this.appSecretKey = new SecretKeySpec(
					Hex.decode(sc.getInitParameter("x2.auth.secretKey")),
					sc.getInitParameter("x2.auth.tokenEncAlg"));
			final Cipher cipher =
				Cipher.getInstance(this.appSecretKey.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, this.appSecretKey);
		} catch (final IllegalArgumentException | GeneralSecurityException e) {
			throw new InitializationException("Invalid authentication token"
					+ " encryption configuration.", e);
		}

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
		if (!m.matches()) {
			if (debug)
				this.log.debug("invalid Authorization header value");
			return null;
		}
		final String tokenP0 = m.group(1);
		final String tokenP1 = m.group(2);
		if (debug)
			this.log.debug("decrypting Authorization header: p0=" + tokenP0
					+ ", p1=" + tokenP1);

		// get bytes of the first part of the token
		ByteBuffer cipherBuf;
		try {
			final int base64Len = tokenP0.length();
			final char[] base64Chars = new char[base64Len];
			tokenP0.getChars(0, base64Len, base64Chars, 0);
			final CharBuffer base64Buf = CharBuffer.wrap(base64Chars);
			cipherBuf = ByteBuffer.allocate((base64Len * 6) / 8 + 1);
			Base64.decode(base64Buf, cipherBuf);
			cipherBuf.flip();
		} catch (final IndexOutOfBoundsException e) {
			if (debug)
				this.log.debug("token decryption error", e);
			return null;
		}

		// decrypt first part of the token
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(this.appSecretKey.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, this.appSecretKey);
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException("Error getting a cipher.", e);
		}
		ByteBuffer clearBuf = ByteBuffer.allocate(
				cipher.getOutputSize(cipherBuf.remaining()));
		try {
			cipher.doFinal(cipherBuf, clearBuf);
			clearBuf.flip();
		} catch (final GeneralSecurityException e) {
			if (debug)
				this.log.debug("token decryption error", e);
			return null;
		}

		// get actor id and opaque value
		final String actorId;
		final String opaque;
		try {
			byte[] strBytes = new byte[clearBuf.getShort()];
			clearBuf.get(strBytes);
			actorId = new String(strBytes, UTF8);
			strBytes = new byte[clearBuf.getShort()];
			if (strBytes.length > 0) {
				clearBuf.get(strBytes);
				opaque = new String(strBytes, UTF8);
			} else {
				opaque = null;
			}
		} catch (final BufferUnderflowException e) {
			if (debug)
				this.log.debug("invalid token", e);
			return null;
		}
		if (debug)
			this.log.debug("decrypted p0: actorId=" + actorId + ", opaque="
					+ opaque);

		// get actor record
		final Actor actor = this.authResolver.getActor(actorId, opaque);
		if (actor == null) {
			if (debug)
				this.log.debug("actor with id " + actorId + " and opaque "
						+ opaque + " does not exist");
			return null;
		}

		// get bytes of the second part of the token
		try {
			final int base64Len = tokenP1.length();
			final char[] base64Chars = new char[base64Len];
			tokenP1.getChars(0, base64Len, base64Chars, 0);
			final CharBuffer base64Buf = CharBuffer.wrap(base64Chars);
			cipherBuf = ByteBuffer.allocate((base64Len * 6) / 8 + 1);
			Base64.decode(base64Buf, cipherBuf);
			cipherBuf.flip();
		} catch (final IndexOutOfBoundsException e) {
			if (debug)
				this.log.debug("token decryption error", e);
			return null;
		}

		// decrypt second part of the token
		SecretKey secretKey = actor.getSecretKey();
		if (secretKey == null)
			secretKey = this.appSecretKey;
		try {
			cipher = Cipher.getInstance(secretKey.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException("Error getting a cipher.", e);
		}
		clearBuf = ByteBuffer.allocate(
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

		// verify opaque value
		final String tokenOpaque;
		try {
			final byte[] strBytes = new byte[clearBuf.getShort()];
			if (strBytes.length > 0) {
				clearBuf.get(strBytes);
				tokenOpaque = new String(strBytes, UTF8);
			} else {
				tokenOpaque = null;
			}
		} catch (final BufferUnderflowException e) {
			if (debug)
				this.log.debug("invalid token", e);
			return null;
		}
		final boolean opaqueMatch;
		if (opaque != null)
			opaqueMatch = opaque.equals(tokenOpaque);
		else
			opaqueMatch = (tokenOpaque == null);
		if (!opaqueMatch) {
			if (debug)
				this.log.debug("opaque values do not match");
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

		// assemble first part of the token
		final byte[] actorIdBytes = actor.getActorId().getBytes(UTF8);
		final String opaque = actor.getOpaque();
		final byte[] opaqueBytes;
		opaqueBytes = (opaque != null ? opaque.getBytes(UTF8) : new byte[0]);
		ByteBuffer clearBuf = ByteBuffer.allocate(
				2 + actorIdBytes.length + 2 + opaqueBytes.length);
		clearBuf.putShort((short) actorIdBytes.length);
		clearBuf.put(actorIdBytes);
		clearBuf.putShort((short) opaqueBytes.length);
		if (opaqueBytes.length > 0)
			clearBuf.put(opaqueBytes);
		clearBuf.flip();

		// encrypt first part of the token
		ByteBuffer cipherBuf;
		try {
			final Cipher cipher =
				Cipher.getInstance(this.appSecretKey.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, this.appSecretKey);
			cipherBuf = ByteBuffer.allocate(
					cipher.getOutputSize(clearBuf.remaining()));
			cipher.doFinal(clearBuf, cipherBuf);
			cipherBuf.flip();
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException("Encryption error.", e);
		}

		// encode first part of the token as Base64
		CharBuffer base64Buf = CharBuffer.allocate(
				(cipherBuf.remaining() * 8) / 6 + 3);
		Base64.encode(cipherBuf, base64Buf);
		base64Buf.flip();
		final String tokenP0 = base64Buf.toString();

		// assemble second part of the token
		final byte[] credentials = actor.getCredentials();
		clearBuf = ByteBuffer.allocate(
				8 + 2 + opaqueBytes.length + credentials.length);
		clearBuf.putLong(System.currentTimeMillis());
		clearBuf.putShort((short) opaqueBytes.length);
		if (opaqueBytes.length > 0)
			clearBuf.put(opaqueBytes);
		clearBuf.put(credentials);
		clearBuf.flip();

		// encrypt second part of the token
		SecretKey secretKey = actor.getSecretKey();
		if (secretKey == null)
			secretKey = this.appSecretKey;
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

		// encode second part of the token as Base64
		base64Buf = CharBuffer.allocate((cipherBuf.remaining() * 8) / 6 + 3);
		Base64.encode(cipherBuf, base64Buf);
		base64Buf.flip();
		final String tokenP1 = base64Buf.toString();

		// add authentication info header
		httpResponse.setHeader("Authentication-Info",
				"nexttoken=" + tokenP0 + "." + tokenP1);
	}
}
