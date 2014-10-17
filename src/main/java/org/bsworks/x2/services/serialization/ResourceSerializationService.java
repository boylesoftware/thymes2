package org.bsworks.x2.services.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EssentialService;
import org.bsworks.x2.resource.InvalidResourceDataException;


/**
 * Service responsible for serialization and deserialization of application
 * resources sent and received via the application API.
 *
 * @author Lev Himmelfarb
 */
public interface ResourceSerializationService
	extends EssentialService {

	/**
	 * MIME content type of the serialized resource data.
	 *
	 * @return Name of the MIME content type.
	 */
	String getContentType();

	/**
	 * Tell if the serialized resource data is textual, in which case it
	 * requires character encoding.
	 *
	 * @return {@code true} for textual serialized data.
	 */
	boolean isText();

	/**
	 * Convert specified resource instance to its serialized form.
	 *
	 * @param out Output stream, to which to write the serialized resource data.
	 * @param charset Character set for the serialized resource data. Must be
	 * specified if {@link #isText()} returns {@code true}. Ignored if
	 * {@link #isText()} returns {@code false}.
	 * @param rsrc Resource instance to serialize.
	 * @param actor Actor that is going to consume the serialized resource data.
	 *
	 * @throws IOException If an error happens writing to the output stream.
	 * @throws NullPointerException If the specified resource instance is
	 * {@code null}.
	 * @throws IllegalArgumentException If the specified object is not a valid
	 * resource instance.
	 */
	void serialize(OutputStream out, Charset charset, Object rsrc, Actor actor)
		throws IOException;

	/**
	 * Create resource instance from its serialized form.
	 *
	 * @param in Input stream, from which to read the serialized resource data.
	 * @param charsetName Character encoding name, or {@code null} if
	 * inapplicable or to auto-detect.
	 * @param rsrcClass Resource class.
	 * @param actor Actor that is providing the serialized resource data or
	 * {@code null} if unauthenticated.
	 *
	 * @return The resource instance.
	 *
	 * @throws InvalidResourceDataException If the serialized data is invalid.
	 * @throws UnsupportedEncodingException If specified character set is
	 * unknown.
	 * @throws IOException If an error happens reading from the input stream.
	 * @throws IllegalArgumentException If the specified class is not a valid
	 * resource class.
	 */
	<R> R deserialize(InputStream in, String charsetName,
			final Class<R> rsrcClass, final Actor actor)
		throws InvalidResourceDataException, UnsupportedEncodingException,
			IOException;
}
