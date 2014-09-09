package org.restlet.ext.odata.json;

import java.io.Reader;
import java.util.ArrayList;

import org.restlet.Context;
import org.restlet.ext.atom.Feed;
import org.restlet.ext.odata.internal.edm.EntityType;
import org.restlet.ext.odata.internal.edm.Metadata;
import org.restlet.ext.odata.json.JsonStreamReaderFactory.JsonStreamReader;
import org.restlet.ext.odata.json.JsonStreamReaderFactory.JsonStreamReader.JsonEvent;
import org.restlet.ext.xml.format.FormatParser;

/**
 * The Class JsonVerboseFeedHandler which parses the Json Verbose response and
 * populates the entity.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 */
@SuppressWarnings("unchecked")
public class JsonVerboseFeedHandler<T> extends JsonFormatParser<T> implements
		FormatParser<T> {

	@Override
	public Feed getFeed() {
		if (this.feed == null) {
			this.feed = new Feed();
		}
		return this.feed;
	}

	/**
	 * Instantiates a new Json verbose feed handler.
	 * 
	 * @param entitySetName
	 *            the entity set name
	 * @param entityType
	 *            the entity type
	 * @param entityClass
	 *            the entity class
	 * @param metadata
	 *            the metadata
	 */
	public JsonVerboseFeedHandler(EntityType entityType, Class<?> entityClass,
			Metadata metadata, Feed feed) {
		// this.entitySetName = entitySetName;
		this.entityType = entityType;
		this.entityClass = entityClass;
		this.entities = new ArrayList<T>();
		this.metadata = metadata;
		this.feed = feed;
	}

	@Override
	public void parse(Reader reader) {
		JsonStreamReader jsr = JsonStreamReaderFactory
				.createJsonStreamReader(reader);
		boolean hasResultsProp = false;
		try {
			// {
			JsonEvent event = null;
			this.ensureStartObject(jsr.nextEvent());

			// "d" :
			this.ensureStartProperty(jsr.nextEvent(), DATA_PROPERTY);

			// {
			this.ensureStartObject(jsr.nextEvent());
			// results only for collections, if it is single entity or
			// property it won't be there
			// "results" :
			event = jsr.nextEvent();
			// if it is start property, check if its results/__metada and
			// then skip them
			if (event.isStartProperty()) {
				if (event.asStartProperty().getName()
						.equals((RESULTS_PROPERTY))) {
					hasResultsProp = true;
					// skip [
					event = jsr.nextEvent();
				} else if (event.asStartProperty().getName()
						.equals(METADATA_PROPERTY)) {
					// this is __metadata, skip it
					event = jsr.nextEvent();
					this.ensureStartObject(event);
					int soCount = 1; // count the start object and end
										// object to know when the
										// __metadata ends
					while (soCount > 0) {
						event = jsr.nextEvent();
						if (event.isStartObject()) {
							soCount++;
						} else if (event.isEndObject()) {
							soCount--;
						}
					}
				}
			}

			// create instance of entity, parse it and add it to list of
			// entities
			if (event.isStartArray()) {
				parseFeed(jsr, entityClass);
				// ] already processed by parseFeed
			} else {
				T entity = (T) entityClass.newInstance();
				this.getEntities().add(parseEntry(jsr, entity));
				// } already processed by parseEntry
			}

			if (hasResultsProp) {
				// EndProperty of "results" :
				this.ensureEndProperty(jsr.nextEvent());
			}

			event = jsr.nextEvent();

			while (event.isStartProperty()) {
				// String pname = event.asStartProperty().getName();
				this.ensureNext(jsr);
				this.ensureEndProperty(event = jsr.nextEvent());
				/*if (NEXT_PROPERTY.equals(pname)) { 
					feed.next = event.asEndProperty().getValue(); 
				} else if (COUNT_PROPERTY.equals(pname)) { 
					feed.inlineCount = Integer.parseInt(event.asEndProperty() .getValue()); 
				}*/
				this.ensureNext(jsr);
				event = jsr.nextEvent();
			}

			if (hasResultsProp) {
				// EndObject and EndProperty of "result" :
				this.ensureEndObject(event);
				this.ensureEndProperty(jsr.nextEvent());
			}

			this.ensureEndObject(jsr.nextEvent());

			if (jsr.hasNext())
				throw new IllegalArgumentException("garbage after the feed");

		} catch (InstantiationException e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the jsonVerbose: " + e.getMessage());
		} catch (IllegalAccessException e) {
			Context.getCurrentLogger().warning(
					"Cannot parse the jsonVerbose: " + e.getMessage());
		} finally {
			jsr.close();
		}
	}
}
