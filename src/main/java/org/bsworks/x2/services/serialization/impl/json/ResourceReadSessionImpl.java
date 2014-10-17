package org.bsworks.x2.services.serialization.impl.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.NoSuchElementException;

import javax.json.JsonException;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceReadSessionCache;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.resource.TypePropertyHandler;


/**
 * Resource read session implementation for deserializing resource from JSON.
 *
 * @author Lev Himmelfarb
 */
class ResourceReadSessionImpl
	implements ResourceReadSession {

	/**
	 * JSON parser.
	 */
	private final JsonParser par;

	/**
	 * The providing actor.
	 */
	private final Actor actor;

	/**
	 * Parser for {@link Date} fields.
	 */
	private final DateFormat df;

	/**
	 * Resources manager.
	 */
	private final Resources resources;

	/**
	 * Tells if the last {@code null} value was because of the current
	 * collection end.
	 */
	private boolean wasCollectionEnd;


	/**
	 * Create new session.
	 *
	 * @param par JSON parser.
	 * @param actor The providing actor, or {@code null}.
	 * @param df Parser for {@link Date} fields.
	 * @param resources Resources manager.
	 */
	ResourceReadSessionImpl(final JsonParser par, final Actor actor,
			final DateFormat df, final Resources resources) {

		this.par = par;
		this.actor = actor;
		this.df = df;
		this.resources = resources;
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
	public Date stringToDate(final String dateStr)
		throws InvalidResourceDataException {

		try {
			return this.df.parse(dateStr);
		} catch (final ParseException e) {
			throw new InvalidResourceDataException(
					"Invalid date value " + dateStr + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceReadSessionCache getSessionCache() {

		// caching is not used
		return null;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String nextProperty()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case KEY_NAME:
				return this.par.getString();
			case END_OBJECT:
				return null;
			default:
				throw new IllegalStateException("Received " + ev + ".");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void swallowValue()
		throws InvalidResourceDataException, IOException {

		try {
			this.swallowValueInternal();
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/**
	 * Recursively swallow next value in the parser.
	 *
	 * @return Last swallowed parser event.
	 */
	private Event swallowValueInternal() {

		Event ev = this.par.next();
		switch (ev) {
		case START_ARRAY:
			while ((ev = this.swallowValueInternal())
					!= Event.END_ARRAY)  { /* nothing */ }
			break;
		case START_OBJECT:
			while ((ev = this.swallowValueInternal())
					!= Event.END_OBJECT)  { /* nothing */ }
			break;
		case END_ARRAY:
		case END_OBJECT:
		case KEY_NAME:
			throw new IllegalStateException("Received " + ev + ".");
		default:
		}

		return ev;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean enterObject()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case START_OBJECT:
				return true;
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return false;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return false;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid object or map property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean enterCollection()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case START_ARRAY:
				return true;
			case VALUE_NULL:
				return false;
			case KEY_NAME:
			case END_ARRAY:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid collection property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean enterMap()
		throws InvalidResourceDataException, IOException {

		return this.enterObject();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String readObjectType(final TypePropertyHandler typePropHandler)
		throws InvalidResourceDataException, IOException {

		try {
			Event ev = this.par.next();
			if (ev != Event.KEY_NAME)
				throw new IllegalStateException("Received " + ev + ".");
			final String typePropName = typePropHandler.getName();
			if (!this.par.getString().equals(typePropName))
				throw new InvalidResourceDataException("First property of the"
						+ " polymorphic object must be " + typePropName + ".");
			ev = this.par.next();
			if (ev != Event.VALUE_STRING)
				throw new InvalidResourceDataException("Value of "
						+ typePropName + " property must be string.");
			return this.par.getString();
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String readKey()
		throws InvalidResourceDataException, IOException {

		return this.nextProperty();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String readStringValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_STRING:
				return this.par.getString();
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Byte readByteValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_NUMBER:
				final int val = this.par.getInt();
				if ((val < Byte.MIN_VALUE) || (val > Byte.MAX_VALUE))
					throw new InvalidResourceDataException(
							"Property value out of range.");
				return Byte.valueOf((byte) val);
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Short readShortValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_NUMBER:
				final int val = this.par.getInt();
				if ((val < Short.MIN_VALUE) || (val > Short.MAX_VALUE))
					throw new InvalidResourceDataException(
							"Property value out of range.");
				return Short.valueOf((short) val);
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Integer readIntegerValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_NUMBER:
				return Integer.valueOf(this.par.getInt());
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Long readLongValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_NUMBER:
				return Long.valueOf(this.par.getLong());
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Boolean readBooleanValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_TRUE:
				return Boolean.TRUE;
			case VALUE_FALSE:
				return Boolean.FALSE;
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Float readFloatValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_NUMBER:
				final double val = this.par.getBigDecimal().doubleValue();
				if ((val < Float.MIN_VALUE) || (val > Float.MAX_VALUE))
					throw new InvalidResourceDataException(
							"Property value out of range.");
				return Float.valueOf((float) val);
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Double readDoubleValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_NUMBER:
				return Double.valueOf(this.par.getBigDecimal().doubleValue());
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public BigDecimal readBigDecimalValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_NUMBER:
				return this.par.getBigDecimal();
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <E extends Enum<E>> E readEnumValue(final Class<E> enumClass)
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_STRING:
				return Enum.valueOf(enumClass, this.par.getString());
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final IllegalArgumentException e) {
			throw new InvalidResourceDataException(
					"Invalid enumeration property value.", e);
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Date readDateValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_STRING:
				return this.df.parse(this.par.getString());
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final ParseException e) {
			throw new InvalidResourceDataException(
					"Invalid date property value.", e);
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Ref<?> readRefValue()
		throws InvalidResourceDataException, IOException {

		try {
			final Event ev = this.par.next();
			switch (ev) {
			case VALUE_STRING:
				return this.resources.parseRef(this.par.getString());
			case VALUE_NULL:
				this.wasCollectionEnd = false;
				return null;
			case END_ARRAY:
				this.wasCollectionEnd = true;
				return null;
			case KEY_NAME:
			case END_OBJECT:
				throw new IllegalStateException("Received " + ev + ".");
			default:
				throw new InvalidResourceDataException(
						"Invalid property value type.");
			}
		} catch (final IllegalArgumentException e) {
			throw new InvalidResourceDataException(
					"Invalid reference property value.", e);
		} catch (final JsonParsingException | NoSuchElementException e) {
			throw new InvalidResourceDataException("Invalid JSON.", e);
		} catch (final JsonException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean wasCollectionEnd() {

		return this.wasCollectionEnd;
	}
}
