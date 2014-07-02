package org.restlet.ext.odata.xml;

import static org.restlet.ext.xml.format.XmlFormatParser.DATASERVICES_ELEMENT;
import static org.restlet.ext.xml.format.XmlFormatParser.M_TYPE;
import static org.restlet.ext.xml.format.XmlFormatParser.NS_DATASERVICES;
import static org.restlet.ext.xml.format.XmlFormatParser.NS_METADATA;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.restlet.Context;
import org.restlet.ext.odata.internal.edm.TypeUtils;

/**
 * The Class CollectionPropertyHandler which handles all type of collections.
 * 
 * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 */
public class CollectionPropertyCursorHandler {

	/**
	 * Parses the collection of either simple type or complex type
	 *
	 * @param <T> the generic type
	 * @param reader the reader
	 * @param entity the entity
	 * @param startElement the start element
	 * @param parentEntity the parent entity
	 * @return the t
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T parse(XMLStreamReader reader, T entity,
			String startElement, T parentEntity) {

		try {
			Object obj = null;
			String collectionType;
			String currentMType = reader.getAttributeValue(NS_METADATA, M_TYPE.getLocalPart());
			while (reader.hasNext()) {
				reader.next();

				if (reader.isEndElement()
						&& reader.getLocalName().equals(startElement)) {
					return entity;
				}

				if (reader.isStartElement()
						&& reader.getNamespaceURI()
								.equals(NS_DATASERVICES)
						&& reader.getLocalName()
								.equals(DATASERVICES_ELEMENT.getLocalPart())) {
					if (entity instanceof List && currentMType != null) { // check first if it is type of List
						Field field = parentEntity.getClass().getDeclaredField(startElement);
						if (field.getGenericType() instanceof ParameterizedType) {
							// determine what type of collection it is
							ParameterizedType listType = (ParameterizedType) field
									.getGenericType();
							collectionType = TypeUtils
									.getCollectionType(currentMType);
							Class<?> listClass = (Class<?>) listType
									.getActualTypeArguments()[0];
							if (collectionType.toLowerCase().startsWith("edm")) { // simple type
								// just add value to list
								Object value = TypeUtils.convert(listClass,
										reader.getElementText());
								((List) entity).add(value);
							} else { // complex type
								obj = listClass.newInstance();
								// create a new instance and populate the properties
								AtomFeedCursorHandler.parseProperties(reader,
										reader.getLocalName(), obj);
								((List) entity).add(obj);
							}
						}
					}
				}
			}
		} catch (XMLStreamException e) {
			Context.getCurrentLogger().warning(
                    "Cannot parse the collection due to: " + e.getMessage());
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
		}
		return entity;
	}

}
