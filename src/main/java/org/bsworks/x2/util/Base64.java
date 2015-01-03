package org.bsworks.x2.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;


/**
 * Base64 codec.
 *
 * @author Lev Himmelfarb
 */
public final class Base64 {

	/**
	 * Lookup table that translates 6-bit positive integer index values into
	 * their "Base64 Alphabet" equivalents as specified in
	 * "Table 1: The Base64 Alphabet" of RFC 2045 (and RFC 4648).
	 */
	private static final char[] TO_BASE64 = {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
		'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
		'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
	};

	/**
	 * Lookup table for decoding Unicode characters drawn from the
	 * "Base64 Alphabet" (as specified in Table 1 of RFC 2045) into their 6-bit
	 * positive integer equivalents. Characters that are not in the Base64
	 * alphabet but fall within the bounds of the array are encoded to -1.
	 */
	private static final int[] FROM_BASE64 = new int[256];
	static {
		Arrays.fill(FROM_BASE64, -1);
		for (int i = 0; i < TO_BASE64.length; i++)
			FROM_BASE64[TO_BASE64[i]] = i;
		FROM_BASE64['='] = -2;
	}


	/**
	 * All methods are static.
	 */
	private Base64() {}


	/**
	 * Encode bytes in the specified byte array and return the Base64 string.
	 *
	 * @param src Bytes to encode.
	 *
	 * @return Encoded Base64 string.
	 */
	public static String encode(final byte[] src) {

		final CharBuffer res = CharBuffer.allocate(
				(src.length * 8) / 6 + 3);

		Base64.encode(ByteBuffer.wrap(src), res);

		return res.toString();
	}

	/**
	 * Encode remaining bytes in the specified byte buffer and return the Base64
	 * string.
	 *
	 * @param src Buffer with bytes to encode.
	 *
	 * @return Encoded Base64 string.
	 */
	public static String encode(final ByteBuffer src) {

		final CharBuffer res = CharBuffer.allocate(
				(src.remaining() * 8) / 6 + 3);

		Base64.encode(src, res);
		res.flip();

		return res.toString();
	}

	/**
	 * Encode remaining bytes in the specified byte buffer and write the Base64
	 * characters to the specified character buffer.
	 *
	 * @param src Buffer with bytes to encode.
	 * @param dst Buffer, to which to write the result.
	 *
	 * @return Number of characters written to the output buffer.
	 */
	public static int encode(final ByteBuffer src, final CharBuffer dst) {

		final byte[] sa = src.array();
		int sl = src.arrayOffset() + src.limit();
		int sp = src.arrayOffset() + src.position();
		final char[] da = dst.array();
		final int dl = dst.arrayOffset() + dst.limit();
		int dp = dst.arrayOffset() + dst.position();
		final int dp00 = dp;
		try {
			sl = sp + (sl - sp) / 3 * 3;
			while (sp < sl) {
				final int slen = sl - sp;
				final int sl0 = Math.min(sp + slen, sl);
				for (int sp0 = sp, dp0 = dp; sp0 < sl0;) {
					if (dp0 + 4 > dl) {
						sp = sp0;
						dp = dp0;
						return  dp0 - dp00;
					}
					final int bits = (sa[sp0++] & 0xff) << 16 |
							(sa[sp0++] & 0xff) << 8 |
							(sa[sp0++] & 0xff);
					da[dp0++] = TO_BASE64[(bits >>> 18) & 0x3f];
					da[dp0++] = TO_BASE64[(bits >>> 12) & 0x3f];
					da[dp0++] = TO_BASE64[(bits >>> 6) & 0x3f];
					da[dp0++] = TO_BASE64[bits & 0x3f];
				}
				final int n = (sl0 - sp) / 3 * 4;
				dp += n;
				sp = sl0;
			}
			sl = src.arrayOffset() + src.limit();
			if ((sp < sl) && (dl >= dp + 4)) { // 1 or 2 leftover bytes
				final int b0 = sa[sp++] & 0xff;
				da[dp++] = TO_BASE64[b0 >> 2];
				if (sp == sl) {
					da[dp++] = TO_BASE64[(b0 << 4) & 0x3f];
					da[dp++] = '=';
					da[dp++] = '=';
				} else {
					final int b1 = sa[sp++] & 0xff;
					da[dp++] = TO_BASE64[(b0 << 4) & 0x3f | (b1 >> 4)];
					da[dp++] = TO_BASE64[(b1 << 2) & 0x3f];
					da[dp++] = '=';
				}
			}
			return dp - dp00;
		} finally {
			src.position(sp - src.arrayOffset());
			dst.position(dp - dst.arrayOffset());
		}
	}

	/**
	 * Decode Base64 characters in the specified character buffer and write the
	 * decoded bytes to the specified byte buffer.
	 *
	 * @param src Buffer with Base64 characters to decode.
	 * @param dst Buffer, to which to write the result.
	 *
	 * @return Number of bytes written to the output buffer.
	 */
	public static int decode(final CharBuffer src, final ByteBuffer dst) {

		int bits = 0;
		int shiftto = 18; // position of the first byte of a 4-byte atom
		final char[] sa = src.array();
		final int sl = src.arrayOffset() + src.limit();
		int sp = src.arrayOffset() + src.position();
		final byte[] da = dst.array();
		final int dl = dst.arrayOffset() + dst.limit();
		int dp = dst.arrayOffset() + dst.position();
		final int dp0 = dp;
		int mark = sp;
		try {
			while (sp < sl) {
				int b = sa[sp++] & 0xff;
				if ((b = FROM_BASE64[b]) < 0) {
					if (b == -2) { // padding byte
						if ((shiftto == 6) &&
								((sp == sl) || (sa[sp++] != '=')) ||
								(shiftto == 18))
							throw new IllegalArgumentException("Input byte" +
								" array has wrong 4-byte ending unit.");
						break;
					}
					throw new IllegalArgumentException(
							"Illegal base64 character " +
									Integer.toString(sa[sp - 1], 16) + ".");
				}
				bits |= (b << shiftto);
				shiftto -= 6;
				if (shiftto < 0) {
					if (dl < dp + 3)
						return dp - dp0;
					da[dp++] = (byte)(bits >> 16);
					da[dp++] = (byte)(bits >> 8);
					da[dp++] = (byte)(bits);
					shiftto = 18;
					bits = 0;
					mark = sp;
				}
			}
			if (shiftto == 6) {
				if (dl - dp < 1)
					return dp - dp0;
				da[dp++] = (byte)(bits >> 16);
			} else if (shiftto == 0) {
				if (dl - dp < 2)
					return dp - dp0;
				da[dp++] = (byte)(bits >> 16);
				da[dp++] = (byte)(bits >> 8);
			} else if (shiftto == 12) {
				throw new IllegalArgumentException(
						"Last unit does not have enough valid bits.");
			}
			if (sp < sl)
				throw new IllegalArgumentException(
						"Input byte array has incorrect ending byte at " + sp +
						".");
			mark = sp;
			return dp - dp0;
		} finally {
			src.position(mark);
			dst.position(dp);
		}
	}
}
