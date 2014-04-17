package org.restlet.ext.odata.xml;

import static org.restlet.ext.xml.format.XmlFormatParser.DATASERVICES_ELEMENT;
import static org.restlet.ext.xml.format.XmlFormatParser.M_TYPE;
import static org.restlet.ext.xml.format.XmlFormatParser.NS_DATASERVICES;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.restlet.ext.odata.internal.edm.TypeUtils;

/**
 * The Class CollectionPropertyHandler which handles all type of collections.
 * 
 * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 */
public class CollectionPropertyHandler {

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
	public static <T> T parse(XMLEventReader reader, T entity,
			StartElement startElement, T parentEntity) {

		try {
			Object obj = null;
			String collectionType;
			Boolean isComplexCollection = false;
			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();

				if (event.isEndElement()
						&& event.asEndElement().getName()
								.equals(startElement.getName())) {
					return entity;
				}

				if (event.isCharacters()) {
					// tagContent = reader.getElementText().trim();
				}

				if (event.isStartElement()
						&& event.asStartElement().getName().getNamespaceURI()
								.equals(NS_DATASERVICES)
						&& event.asStartElement().getName()
								.equals(DATASERVICES_ELEMENT)) {
					if (entity instanceof List) { // check first if it is type of List
						Field field = parentEntity.getClass().getDeclaredField(
								startElement.getName().getLocalPart());
						String currentMType = startElement.getAttributeByName(
								M_TYPE).getValue();
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
								isComplexCollection = true;
								obj = listClass.newInstance();
								// create a new instance and populate the properties
								AtomFeedHandler.parseProperties(reader,
										event.asStartElement(), obj);
								((List) entity).add(obj);
							}
						}
					}
				}
				//TODO: Check for Multiple elements in the collection of complex.
				if (event.isEndElement()
						&& event.asEndElement().getName()
								.equals(startElement.getName())
						&& event.asEndElement().getName()
								.equals(DATASERVICES_ELEMENT)) {
					if (isComplexCollection) {
						((List) entity).add(obj);
					}
				}
			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return entity;
	}

}
