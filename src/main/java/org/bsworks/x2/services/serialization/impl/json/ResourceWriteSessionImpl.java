package org.bsworks.x2.services.serialization.impl.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

import javax.json.JsonException;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.resource.TypePropertyHandler;


/**
 * Resource write session implementation for serializing resource into JSON.
 *
 * @author Lev Himmelfarb
 */
class ResourceWriteSessionImpl
	implements ResourceWriteSession {

	/**
	 * JSON generator.
	 */
	private final JsonGenerator gen;

	/**
	 * The consuming actor.
	 */
	private final Actor actor;

	/**
	 * Tells if null-valued properties should be dropped.
	 */
	private final boolean dropNulls;

	/**
	 * Formatter for {@link Date} fields.
	 */
	private final DateFormat df;

	/**
	 * Current property name, or {@code null} if in a collection writing
	 * context.
	 */
	private String curPropName = null;


	/**
	 * Create new session.
	 *
	 * @param gen JSON generator.
	 * @param actor The consuming actor, or {@code null}.
	 * @param dropNulls {@code true} to suggest the session user to drop
	 * null-valued properties.
	 * @param df Formatter for {@link Date} fields.
	 */
	ResourceWriteSessionImpl(final JsonGenerator gen, final Actor actor,
			final boolean dropNulls, final DateFormat df) {

		this.gen = gen;
		this.actor = actor;
		this.dropNulls = dropNulls;
		this.df = df;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Actor getActor() {

		return this.actor;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isNullDropped() {

		return this.dropNulls;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String dateToString(final Date date) {

		return this.df.format(date);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession addProperty(final String propName) {

		if (this.curPropName != null)
			throw new IllegalStateException("Expecting property value.");

		this.curPropName = propName;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession startObject(final Object obj)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.writeStartObject(this.curPropName);
				this.curPropName = null;
			} else {
				this.gen.writeStartObject();
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession startObject(final Object obj,
			final TypePropertyHandler typePropHandler, final String valueType)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.writeStartObject(this.curPropName);
				this.curPropName = null;
			} else {
				this.gen.writeStartObject();
			}
			this.gen.write(typePropHandler.getName(), valueType);
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession endObject()
		throws IOException {

		try {
			this.gen.writeEnd();
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession startCollection()
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.writeStartArray(this.curPropName);
				this.curPropName = null;
			} else {
				this.gen.writeStartArray();
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession endCollection()
		throws IOException {

		try {
			this.gen.writeEnd();
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession startMap()
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.writeStartObject(this.curPropName);
				this.curPropName = null;
			} else {
				this.gen.writeStartObject();
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession endMap()
		throws IOException {

		try {
			this.gen.writeEnd();
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeKey(final String key) {

		if (key == null)
			throw new NullPointerException("Map entry key may not be null.");

		if (this.curPropName != null)
			throw new IllegalStateException("Expecting map entry value.");

		this.curPropName = key;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeNullValue()
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.writeNull(this.curPropName);
				this.curPropName = null;
			} else {
				this.gen.writeNull();
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final String val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final byte val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final short val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final int val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final long val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final boolean val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final float val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final double val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final BigDecimal val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val);
				this.curPropName = null;
			} else {
				this.gen.write(val);
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final Enum<?> val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val.toString());
				this.curPropName = null;
			} else {
				this.gen.write(val.toString());
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final Date val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, this.df.format(val));
				this.curPropName = null;
			} else {
				this.gen.write(this.df.format(val));
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceWriteSession writeValue(final Ref<?> val)
		throws IOException {

		try {
			if (this.curPropName != null) {
				this.gen.write(this.curPropName, val.toString());
				this.curPropName = null;
			} else {
				this.gen.write(val.toString());
			}
		} catch (final JsonGenerationException e) {
			throw new IllegalStateException(e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}

		return this;
	}
}
