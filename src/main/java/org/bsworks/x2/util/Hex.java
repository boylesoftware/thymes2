package org.bsworks.x2.util;


/**
 * Hexadecimal codec.
 *
 * @author Lev Himmelfarb
 */
public final class Hex {

	/**
	 * Hexadecimal alphabet.
	 */
	private static final char[] HEX = "0123456789abcdef".toCharArray();


	/**
	 * All methods are static.
	 */
	private Hex() {}


	/**
	 * Encode bytes to a hexadecimal string.
	 *
	 * @param bytes Bytes to encode.
	 *
	 * @return The encoded string.
	 */
	public static String encode(final byte[] bytes) {

		final int len = bytes.length * 2;
		final StringBuilder res = new StringBuilder(len > 16 ? len : 16);
		for (final byte b : bytes) {
			res.append(HEX[(b >> 4) & 0xF]);
			res.append(HEX[(b & 0xF)]);
		}
		return res.toString();
	}

	/**
	 * Decode hexadecimal string.
	 *
	 * @param str Hexadecimal string.
	 *
	 * @return The decoded bytes.
	 *
	 * @throws IllegalArgumentException If the string is not a valid hexadecimal
	 * string.
	 */
	public static byte[] decode(final String str) {

		final int strLen = str.length();
		if (strLen % 2 != 0)
			throw new IllegalArgumentException(
					"Hexadecimal string must have even length.");

		final byte[] res = new byte[strLen / 2];
		for (int i = 0; i < strLen; i += 2)
			res[i / 2] = (byte) (hexToBin(str.charAt(i)) * 16 +
					hexToBin(str.charAt(i + 1)));

		return res;
	}

	/**
	 * Convert hexadecimal character to its value.
	 *
	 * @param c The character.
	 *
	 * @return The value.
	 *
	 * @throws IllegalArgumentException If the character is invalid.
	 */
	private static int hexToBin(final char c) {

		if ((c >= '0') && (c <= '9'))
			return c - '0';
		if ((c >= 'A') && (c <= 'F'))
			return c - 'A' + 10;
		if ((c >= 'a') && (c <= 'f'))
			return c - 'a' + 10;

		throw new IllegalArgumentException(
				"Illegal character in hexadecimal string.");
	}
}
