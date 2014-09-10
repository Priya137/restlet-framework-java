package org.restlet.ext.odata.json;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.ext.atom.Feed;
import org.restlet.ext.odata.internal.edm.ComplexProperty;
import org.restlet.ext.odata.internal.edm.ComplexType;
import org.restlet.ext.odata.internal.edm.EntityType;
import org.restlet.ext.odata.internal.edm.Metadata;
import org.restlet.ext.odata.internal.edm.Property;
import org.restlet.ext.odata.internal.edm.Type;
import org.restlet.ext.odata.internal.edm.TypeUtils;
import org.restlet.ext.odata.internal.reflect.ReflectUtils;
import org.restlet.ext.odata.json.JsonStreamReaderFactory.JsonStreamReader;
import org.restlet.ext.odata.json.JsonStreamReaderFactory.JsonStreamReader.JsonEvent;

/**
 * The Class JsonFormatParser is base abstract class to all supported json
 * types. This class is responsible for parsing the json and giving the response
 * back to respective handler.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 */
public abstract class JsonFormatParser<T> {

	/** The metadata. */
	protected Metadata metadata;

	/** The entities. */
	protected List<T> entities;

	/** The entity type. */
	protected EntityType entityType;

	/** The entity class. */
	protected Class<?> entityClass;

	/** The feed. */
	protected Feed feed;

	/**
	 * Gets the feed.
	 * 
	 * @return the feed
	 */
	abstract public Feed getFeed();

	/**
	 * Sets the feed.
	 * 
	 * @param feed
	 *            the new feed
	 */
	public void setFeed(Feed feed) {
		this.feed = feed;
	}

	/**
	 * Gets the entities.
	 * 
	 * @return the entities
	 */
	public List<T> getEntities() {
		if (this.entities == null) {
			this.entities = new ArrayList<T>();
		}
		return entities;
	}

	static class JsonEntryMetaData {
		String uri;
		String type;
		String etag;
		String mediaSrc = null;
		String mediaContentType = MediaType.APPLICATION_OCTET_STREAM.getName(); // default to "application/octet-stream"
		// TODO:Onkar : parse metadata about actions/functions later
	}

	protected static final String METADATA_PROPERTY = "__metadata";
	protected static final String DEFERRED_PROPERTY = "__deferred";
	protected static final String NEXT_PROPERTY = "__next";
	protected static final String COUNT_PROPERTY = "__count";

	protected static final String URI_PROPERTY = "uri";
	protected static final String TYPE_PROPERTY = "type";
	protected static final String ETAG_PROPERTY = "etag";
	protected static final String ACTIONS_PROPERTY = "actions";
	protected static final String FUNCTIONS_PROPERTY = "functions";
	protected static final String RESULTS_PROPERTY = "results";
	protected static final String DATA_PROPERTY = "d";
	protected static final String MEDIA_SOURCE = "media_src";
	protected static final String MEDIA_CONTENT_TYPE = "content_type";

	protected Feed parseFeed(JsonStreamReader jsr, Class<?> entityClass) {
		try {
			while (jsr.hasNext()) {
				JsonEvent event = jsr.nextEvent();

				if (event.isStartObject()) {
					@SuppressWarnings("unchecked")
					T entity = (T) entityClass.newInstance();
					this.parseEntry(jsr, entity);
					this.getEntities().add(entity);
				} else if (event.isEndArray()) {
					break;
				}
			}
		} catch (InstantiationException e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the feed due to: " + e.getMessage());
		} catch (IllegalAccessException e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the feed due to: " + e.getMessage());
		}

		return this.getFeed();
	}

	protected T parseEntry(JsonStreamReader jsr, T entity) {
		// TODO: Onkar parse links later
		while (jsr.hasNext()) {
			JsonEvent event = jsr.nextEvent();
			if (event.isStartProperty()) {
				this.addProperty(entity, event.asStartProperty().getName(), jsr);
				// TODO: Onkar : Media entity later
			} else if (event.isEndObject()) {
				break;
			}
		}

		return entity;
	}

	protected JsonEntryMetaData parseMetadata(JsonStreamReader jsr) {
		JsonEntryMetaData jemd = new JsonEntryMetaData();
		this.ensureStartObject(jsr.nextEvent());

		while (jsr.hasNext()) {
			JsonEvent event = jsr.nextEvent();
			this.ensureNext(jsr);

			if (event.isStartProperty()
					&& URI_PROPERTY.equals(event.asStartProperty().getName())) {
				this.ensureEndProperty(event = jsr.nextEvent());
				jemd.uri = event.asEndProperty().getValue();
			} else if (event.isStartProperty()
					&& TYPE_PROPERTY.equals(event.asStartProperty().getName())) {
				this.ensureEndProperty(event = jsr.nextEvent());
				jemd.type = event.asEndProperty().getValue();
			} else if (event.isStartProperty()
					&& ETAG_PROPERTY.equals(event.asStartProperty().getName())) {
				this.ensureEndProperty(event = jsr.nextEvent());
				jemd.etag = event.asEndProperty().getValue();
			} else if (event.isStartProperty()
					&& MEDIA_SOURCE.equals(event.asStartProperty().getName())) {
				this.ensureEndProperty(event = jsr.nextEvent());
				jemd.mediaSrc = event.asEndProperty().getValue();
			} else if (event.isStartProperty()
					&& MEDIA_CONTENT_TYPE.equals(event.asStartProperty()
							.getName())) {
				this.ensureEndProperty(event = jsr.nextEvent());
				jemd.mediaContentType = event.asEndProperty().getValue();
			} else if (event.isStartProperty()
					&& ACTIONS_PROPERTY.equals(event.asStartProperty()
							.getName())) {
				// parseFunctions();
				// TODO: Onkar functions later
			} else if (event.isStartProperty()
					&& FUNCTIONS_PROPERTY.equals(event.asStartProperty()
							.getName())) {
				// parseFunctions();
			} else if (event.isStartProperty() || event.isStartObject()
					|| event.isStartArray()) {
				// ignore unsupported metadata, i.e. everything besides uri, type and etag
				jsr.skipNestedEvents();
			} else if (event.isEndObject()) {
				break;
			}
		}
		// eat the EndProperty event
		this.ensureEndProperty(jsr.nextEvent());

		return jemd;
	}

	/**
	 * adds the property. This property can be a navigation property too. In
	 * this case a link will be added. If it's the meta data the information
	 * will be added to the entry too.
	 */
	protected void addProperty(T entity, String name, JsonStreamReader jsr) {

		try {
			if (METADATA_PROPERTY.equals(name)) {
				this.parseMetadata(jsr);
				JsonEvent event = jsr.nextEvent();
				this.ensureStartProperty(event);
				name = event.asStartProperty().getName();
			}
			Object value = null;

			JsonEvent event = jsr.nextEvent();

			if (event.isEndProperty()) {
				// scalar property
				Property property = this.metadata.getProperty(entity, name);
				if (property == null) {
					// AssociationEnd association = this.metadata.getAssociation(entityType, name);
					// FIXME Onkar : Handle inline entities later
				} else {
					Type type = property.getType();
					if (TypeUtils.isEdmSimpleType(type.getName())) { // EDM Type
						value = TypeUtils.fromEdm(event.asEndProperty()
								.getValue(), type.getName());
						if (value != null) {
							String propertyName = ReflectUtils.normalize(name);
							// Check if that property is java reserved key word.
							// If so then prefix it with '_'
							if (ReflectUtils.isReservedWord(propertyName)) {
								propertyName = "_" + propertyName;
							}
							ReflectUtils.invokeSetter(entity, propertyName,
									value);
						}
					} else { // collection type or complex type
						// the only context that lands us here is a null value
						// for a complex property
						// do nothing as we already have null set for it.
					}
				}
			} else if (event.isStartObject()) {
				// reference deferred or inlined

				this.getValue(event, name, jsr, entity);

				if (event.isStartArray()) {
					this.ensureNext(jsr);
					event = jsr.nextEvent();

					if (event.isValue()) {
						throw new IllegalArgumentException(
								"arrays of primitive types not supported! property "
										+ "." + name);
					} else if (event.isStartObject()) {
						do {
							event = jsr.nextEvent();
						} while (!event.isEndArray());
					} else {
						throw new IllegalArgumentException("What's that?");
					}

					this.ensureEndProperty(jsr.nextEvent());
				}
			}
		} catch (Exception e) {
			Context.getCurrentLogger().warning(
					"Cannot add property: " + name + " " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	protected void getValue(JsonEvent event, String name, JsonStreamReader jsr,
			T entity) {

		this.ensureStartObject(event);

		event = jsr.nextEvent();
		this.ensureStartProperty(event);

		try {
			// "__deferred":
			if (DEFERRED_PROPERTY.equals(event.asStartProperty().getName())) {
				// deferred feed or entity

				// {
				this.ensureStartObject(jsr.nextEvent());

				// "uri" :
				this.ensureStartProperty(jsr.nextEvent(), URI_PROPERTY);
				// "uri" property value
				/* String uri = */
				jsr.nextEvent().asEndProperty().getValue();

				// rt.uri = uri;

				// }
				this.ensureEndObject(jsr.nextEvent());

				// eat EndObject event and EndProperty event for "__deferred"
				// }
				this.ensureEndProperty(jsr.nextEvent());
				this.ensureEndObject(jsr.nextEvent());

				// "results" :
			} else if (RESULTS_PROPERTY.equals(event.asStartProperty()
					.getName())) {
				// inlined feed or a collection property

				Property property = this.metadata.getProperty(entity, name);
				if (property == null) {
					// [
					this.ensureStartArray(jsr.nextEvent());

					// AssociationEnd association =
					// this.metadata.getAssociation(entityType, name);
					Object o = ReflectUtils.getPropertyObject(entity, name);
					this.parseInlineEntities(jsr, name, entity, o);
				} else {
					// if this is collection or complex type
					if (property instanceof ComplexProperty) { 
						ComplexType complexType = ((ComplexProperty) property)
								.getComplexType();
						if (TypeUtils.startsWithList(complexType.getName())) {// collection type
							Object o = ReflectUtils.getPropertyObject(entity,
									name);
							JsonCollectionPropertyHandler<T> jfp = new JsonCollectionPropertyHandler<T>(
									metadata, name, (T) o, entity);
							jfp.parseCollection(jsr);
						}
					}
				}

				this.ensureEndProperty(jsr.nextEvent());
				this.ensureEndObject(jsr.nextEvent());

			} else if (METADATA_PROPERTY.equals(event.asStartProperty()
					.getName())) {
				// inlined entity or link starting with meta data, not if the
				// value
				// is a complex type
				this.parseMetadata(jsr);
				Property property = this.metadata.getProperty(entity, name);
				if (property == null) {
					// AssociationEnd association =
					// this.metadata.getAssociation(entityType, name);
					Object o = ReflectUtils.getPropertyObject(entity, name);
					this.parseInlineEntities(jsr, name, entity, o);
				} else {
					// if this is collection or complex type
					if (property instanceof ComplexProperty) { 
						ComplexType complexType = ((ComplexProperty) property)
								.getComplexType();
						if (TypeUtils.startsWithList(complexType.getName())) {// collection type
							Object o = ReflectUtils.getPropertyObject(entity,
									name);
							JsonCollectionPropertyHandler<T> jfp = new JsonCollectionPropertyHandler<T>(metadata, 
									name, (T) o, entity);
							jfp.parseCollection(jsr);
						}
					}
				}
			} else if (event.isStartProperty()) {
				// inlined entity or complex object
				// EntityType entityType =
				// this.metadata.getEntityType(entity.getClass());
				Property property = this.metadata.getProperty(entity, name);
				if (property == null) {
					do {
						addProperty(entity, event.asStartProperty().getName(),
								jsr);
						event = jsr.nextEvent();
					} while (!event.isEndObject());
				} else {
					// if this inline complex type
					if (property instanceof ComplexProperty) { 
						Object o = ReflectUtils.getPropertyObject(entity, name);
						this.parseInlineEntities(jsr, name, entity, o);
					}
				}
			} else {
				throw new IllegalArgumentException("What's that?");
			}
		} catch (SecurityException e) {
			Context.getCurrentLogger().warning(
					"Exception in getting value for property " + name + " "
							+ e.getMessage());
		} catch (Exception e) {
			Context.getCurrentLogger().warning(
					"Exception in getting value for property " + name + " "
							+ e.getMessage());
		}

		this.ensureEndProperty(jsr.nextEvent());
	}

	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	protected void parseInlineEntities(JsonStreamReader jsr,
			String propertyName, T entity, Object o) {
		try {
			// Collection of complex i.e. one to many association
			if (o instanceof List) { 
				Field field = entity.getClass().getDeclaredField(
						ReflectUtils.normalize(propertyName));
				if (field.getGenericType() instanceof ParameterizedType) {
					// determine what type of collection it is
					ParameterizedType listType = (ParameterizedType) field
							.getGenericType();
					Class<?> listClass = (Class<?>) listType
							.getActualTypeArguments()[0];

					while (jsr.hasNext()) {
						JsonEvent event = jsr.nextEvent();
						if (event.isStartObject()) {
							// Create new Item Instance
							Object obj = listClass.newInstance();
							// create a new instance and populate the properties
							this.parseEntry(jsr, (T) obj);
							((List) o).add(obj);
						} else if (event.isEndArray()) {
							break;
						}
					}
				}
			} else { // complex object i.e. embedded object in parent entity
				this.parseEntry(jsr, (T) o);
				// set it back to parent entity
				ReflectUtils.invokeSetter(entity,
						ReflectUtils.normalize(propertyName), o);
			}
		} catch (InstantiationException e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the inline entities due to: "
							+ e.getMessage());
		} catch (IllegalAccessException e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the inline entities due to: "
							+ e.getMessage());
		} catch (NoSuchFieldException e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the inline entities due to: "
							+ e.getMessage());
		} catch (SecurityException e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the inline entities due to: "
							+ e.getMessage());
		} catch (Exception e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the inline entities due to: "
							+ e.getMessage());
		}
	}

	protected void ensureNext(JsonStreamReader jsr) {
		if (!jsr.hasNext()) {
			throw new IllegalArgumentException(
					"no valid JSON format exepected at least one more event");
		}
	}

	protected void ensureStartProperty(JsonEvent event) {
		if (!event.isStartProperty()) {
			throw new IllegalArgumentException(
					"no valid OData JSON format (expected StartProperty got "
							+ event + ")");
		}
	}

	protected void ensureStartProperty(JsonEvent event, String name) {
		if (!(event.isStartProperty() && name.equals(event.asStartProperty()
				.getName()))) {
			throw new IllegalArgumentException(
					"no valid OData JSON format (expected StartProperty "
							+ name + " got " + event + ")");
		}
	}

	protected void ensureEndProperty(JsonEvent event) {
		if (!event.isEndProperty()) {
			throw new IllegalArgumentException(
					"no valid OData JSON format (expected EndProperty got "
							+ event + ")");
		}
	}

	protected void ensureStartObject(JsonEvent event) {
		if (!event.isStartObject()) {
			throw new IllegalArgumentException(
					"no valid OData JSON format expected StartObject got "
							+ event + ")");
		}
	}

	protected void ensureEndObject(JsonEvent event) {
		if (!event.isEndObject()) {
			throw new IllegalArgumentException(
					"no valid OData JSON format expected EndObject got "
							+ event + ")");
		}
	}

	protected void ensureStartArray(JsonEvent event) {
		if (!event.isStartArray()) {
			throw new IllegalArgumentException(
					"no valid OData JSON format expected StartArray got "
							+ event + ")");
		}
	}

	protected void ensureEndArray(JsonEvent event) {
		if (!event.isEndArray()) {
			throw new IllegalArgumentException(
					"no valid OData JSON format expected EndArray got " + event
							+ ")");
		}
	}
}
