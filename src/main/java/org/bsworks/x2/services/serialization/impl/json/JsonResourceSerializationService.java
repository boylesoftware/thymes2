package org.bsworks.x2.services.serialization.impl.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourceHandler;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.serialization.ResourceSerializationService;


/**
 * JSON resource serialization service implementation.
 *
 * @author Lev Himmelfarb
 */
class JsonResourceSerializationService
	implements ResourceSerializationService {

	/**
	 * JSON MIME content type.
	 */
	private static final String CTYPE_JSON = "application/json";

	/**
	 * Format for {@link Date} fields.
	 */
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";


	/**
	 * Application resources manager.
	 */
	private final Resources resources;


	/**
	 * Create new service instance.
	 *
	 * @param resources Application resources manager.
	 */
	JsonResourceSerializationService(final Resources resources) {

		this.resources = resources;
	}


	/* (non-Javadoc)
	 * @see org.bsworks.x2.services.serialization.ResourceSerializationService#getContentType()
	 */
	@Override
	public String getContentType() {

		return CTYPE_JSON;
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.services.serialization.ResourceSerializationService#isText()
	 */
	@Override
	public boolean isText() {

		return true;
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.services.serialization.ResourceSerializationService#serialize(java.io.OutputStream, java.nio.charset.Charset, java.lang.Object, org.bsworks.x2.Actor)
	 */
	@Override
	public void serialize(final OutputStream out, final Charset charset,
			final Object rsrc, final Actor actor)
		throws IOException {

		// check if null
		if (rsrc == null)
			throw new NullPointerException(
					"Resource instance to serialize cannot be null.");

		// get resource handler
		final ResourceHandler<?> rsrcHandler =
			this.resources.getResourceHandler(rsrc.getClass());

		// create date formatter
		final DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		// serialize the resource using the handler
		try (final Writer outWriter = new OutputStreamWriter(out, charset)) {
			try (final JsonGenerator gen = Json.createGenerator(outWriter)) {
				rsrcHandler.getResourceValueHandler().writeValue(
						ResourcePropertyAccess.SEE, rsrc,
						new ResourceWriteSessionImpl(gen, actor, df));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.services.serialization.ResourceSerializationService#deserialize(java.io.InputStream, java.lang.String, java.lang.Class, org.bsworks.x2.Actor)
	 */
	@Override
	public <R> R deserialize(final InputStream in, final String charsetName,
			final Class<R> rsrcClass, final Actor actor)
		throws InvalidResourceDataException, UnsupportedEncodingException,
			IOException {

		// get resource handler
		final ResourceHandler<?> rsrcHandler =
			this.resources.getResourceHandler(rsrcClass);

		// create date formatter
		final DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		// deserialize the resource
		final Object rsrc;
		try (final JsonParser par = (charsetName != null ?
				Json.createParser(new InputStreamReader(in, charsetName)) :
				Json.createParser(in))) {
			rsrc = rsrcHandler.getResourceValueHandler().readValue(
					ResourcePropertyAccess.SUBMIT,
					new ResourceReadSessionImpl(par, actor, df,
							this.resources));
		}

		// return the resource
		return rsrcClass.cast(rsrc);
	}
}
