package org.restlet.ext.odata.factory;

import org.restlet.data.MediaType;
import org.restlet.ext.atom.Feed;
import org.restlet.ext.odata.internal.edm.EntityType;
import org.restlet.ext.odata.internal.edm.Metadata;
//import org.restlet.ext.odata.json.JsonVerboseFeedHandler;
import org.restlet.ext.odata.xml.AtomFeedHandler;
import org.restlet.ext.xml.format.FormatParser;
import org.restlet.ext.xml.format.FormatType;

/**
 * A factory for creating FeedHandler objects.
 * 
 * @param <T>
 *            the generic feed handler type
 * 
 * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 */
public class FeedHandlerFactory {

	/**
	 * Gets the parser.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param formatType
	 *            the format type
	 * @return the parser
	 */
	public static <T> FormatParser<T> getParser(FormatType formatType,
			EntityType entityType, Class<?> entityClass, Metadata metadata, Feed feed) {
		if (formatType.equals(FormatType.ATOM)) {
			return (FormatParser<T>) new AtomFeedHandler<T>(entityType,
					entityClass, metadata, feed);
		} else if (formatType.equals(FormatType.JSONVERBOSE)) {
			/*return (FormatParser<T>) new JsonVerboseFeedHandler<T>(entityType,
					entityClass, metadata, feed);*/
		}
		throw new IllegalArgumentException("Unable to locate format parser for " + formatType.toString());
	}

	/**
	 * Gets the parser.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param mediaType
	 *            the media type
	 * @return the parser
	 */
	public static <T> FormatParser<T> getParser(MediaType mediaType,
			EntityType entityType, Class<?> entityClass, Metadata metadata, Feed feed) {
		return FeedHandlerFactory.getParser(
				FormatType.parse(mediaType.getName()), entityType, entityClass,
				metadata, feed);
	}
}
