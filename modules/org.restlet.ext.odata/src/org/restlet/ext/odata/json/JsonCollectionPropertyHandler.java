package org.restlet.ext.odata.json;

import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.restlet.Context;
import org.restlet.ext.atom.Feed;
import org.restlet.ext.odata.internal.edm.Metadata;
import org.restlet.ext.odata.internal.edm.TypeUtils;
import org.restlet.ext.odata.json.JsonStreamReaderFactory.JsonStreamReader;
import org.restlet.ext.odata.json.JsonStreamReaderFactory.JsonStreamReader.JsonEvent;
import org.restlet.ext.xml.format.FormatParser;

/**
 * Parse the Collection in JSON format.
 * 
 * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 */
@SuppressWarnings("unchecked")
public class JsonCollectionPropertyHandler<T> extends JsonFormatParser<T> implements FormatParser<T> {

	private String propertyName;
	private T entity;
	private T parentEntity;

	public JsonCollectionPropertyHandler(Metadata metadata, String propertyName, T entity, T parentEntity) {
		this.metadata = metadata;
		this.propertyName = propertyName;
		this.entity = entity;
		this.parentEntity = parentEntity;
	}

	@Override
	public Feed getFeed() {
		if (this.feed == null) {
			this.feed = new Feed();
		}
		return this.feed;
	}

	@Override
	public void parse(Reader reader) {

		JsonStreamReader jsr = JsonStreamReaderFactory
				.createJsonStreamReader(reader);
		try {
			this.ensureNext(jsr);
			this.ensureStartObject(jsr.nextEvent()); // the response object

	        // "d" property
	        this.ensureNext(jsr);
	        this.ensureStartProperty(jsr.nextEvent(), DATA_PROPERTY);

			// parse the collection
			this.parseCollection(jsr);

			// the "d" property was our object...it is also a property.
			this.ensureNext(jsr);
			this.ensureEndProperty(jsr.nextEvent());

			this.ensureNext(jsr);
			this.ensureEndObject(jsr.nextEvent()); // the response object

		} finally {
			jsr.close();
		}
	}

	/**
	 * Parses the collection of either simple type or complex type
	 * @param jsr the jsr
	 * @return the t
	 */
	@SuppressWarnings("rawtypes")
	public T parseCollection(JsonStreamReader jsr) {
		try {
			int startObjCount = 0;
			// an array of objects:
			this.ensureNext(jsr);
			JsonEvent event = jsr.nextEvent();
			while (!event.isStartArray()) {
				if (event.isStartObject()) {
					startObjCount++;
				} else if (event.isEndObject()) {
					startObjCount--;
				}

				this.ensureNext(jsr);
				event = jsr.nextEvent();
			}
			this.ensureStartArray(event);

			if (this.entity instanceof List) { // check first if it is type of List
				Field field = this.parentEntity.getClass().getDeclaredField(
						this.propertyName);
				if (field.getGenericType() instanceof ParameterizedType) {
					ParameterizedType listType = (ParameterizedType) field
						.getGenericType();
					Class<?> listClass = (Class<?>) listType
						.getActualTypeArguments()[0];
					boolean isPrimitiveCollection = TypeUtils.isPrimitiveCollection(listClass);
					if (isPrimitiveCollection) { // simple type
						String edmType = TypeUtils.toEdmType(listClass.getName());
						// just add value to list
						while (jsr.hasNext()) {
							JsonEvent event2 = jsr.nextEvent();
							if (event2.isEndArray()) {
								break;
							} 
							Object value = TypeUtils
									.fromEdm(event2.asValue().getValue(),edmType);
							((List) this.entity).add(value);
						}
					} else { // complex type
						// determine what type of collection it is
						while (jsr.hasNext()) {
							JsonEvent event2 = jsr.nextEvent();
							if (event2.isStartObject()) {
								Object obj = listClass.newInstance();
								// populate the object
								this.parseEntry(jsr, (T) obj);
								((List) this.entity).add(obj);
							} else if (event2.isEndArray()) {
								break;
							}
						}
					}
				}
			}

			// we should see the end of the array
			this.ensureEndArray(jsr.previousEvent());

			while (startObjCount > 0) {
				event = jsr.nextEvent();
				if (event.isStartObject()) {
					startObjCount++;
				} else if (event.isEndObject()) {
					startObjCount--;
				}
			}
			return entity; 
		} catch (SecurityException e) {
			Context.getCurrentLogger().warning(
                    "Cannot parse the collection due to: " + e.getMessage());
		} catch (NoSuchFieldException e) {
			Context.getCurrentLogger().warning(
                    "Cannot parse the collection due to: " + e.getMessage());
		} catch (InstantiationException e) {
			Context.getCurrentLogger().warning(
                    "Cannot parse the collection due to: " + e.getMessage());
		} catch (IllegalAccessException e) {
			Context.getCurrentLogger().warning(
                    "Cannot parse the collection due to: " + e.getMessage());
		} catch (Exception e) {
			Context.getCurrentLogger().warning(
                    "Cannot parse the collection due to: " + e.getMessage());
		}
		return entity;
	}
}
