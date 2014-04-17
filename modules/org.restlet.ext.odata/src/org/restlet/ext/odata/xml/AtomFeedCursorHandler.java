package org.restlet.ext.odata.xml;

import java.io.Reader;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.restlet.data.MediaType;
import org.restlet.data.Metadata;
import org.restlet.data.Reference;
import org.restlet.ext.atom.Entry;
import org.restlet.ext.atom.Feed;
import org.restlet.ext.atom.Link;
import org.restlet.ext.atom.Relation;
import org.restlet.ext.odata.internal.edm.EntitySet;
import org.restlet.ext.odata.internal.edm.EntityType;
import org.restlet.ext.odata.internal.edm.TypeUtils;
import org.restlet.ext.odata.internal.reflect.ReflectUtils;
import org.restlet.ext.xml.format.FormatParser;
import org.restlet.ext.xml.format.XmlFormatParser;

/**
 * The Class AtomFeedHandler which parses the AtomFeed using STAX Event Iterator model.
 *
 * @param <T> the generic type 
 * 
 * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 */
public class AtomFeedCursorHandler<T> extends XmlFormatParser implements
		FormatParser<Feed> {

	/** The metadata. */
	protected Metadata metadata;
	
	/** The entity set name. */
	protected String entitySetName;
	
	/** The entity type. */
	private EntityType entityType;
	
	/** The entity class. */
	private Class<?> entityClass;
	
	/** The entities. */
	private List<T> entities;
	
	/** The references from the entry to Web resources. */
    private volatile List<Link> links;
    
    /** The feed. */
    private Feed feed;
	
	/**
	 * Gets the entities.
	 *
	 * @return the entities
	 */
	public List<T> getEntities() {
		return entities;
	}
	
	/**
	 * Gets the feed.
	 *
	 * @return the feed
	 */
	public Feed getFeed(){
		if(feed == null){
			feed = new Feed();
		}
		return feed;
	}
	
	/**
	 * Sets the feed.
	 *
	 * @param feed the new feed
	 */
	public void setFeed(Feed feed){
		this.feed = feed;
	}
	
	/**
     * Returns the references from the entry to Web resources.
     * 
     * @return The references from the entry to Web resources.
     */
    public List<Link> getLinks() {
        // Lazy initialization with double-check.
        List<Link> l = this.links;
        if (l == null) {
            synchronized (this) {
                l = this.links;
                if (l == null) {
                    this.links = l = new ArrayList<Link>();
                }
            }
        }
        return l;
    }

	/**
	 * Instantiates a new atom feed handler.
	 *
	 * @param entitySetName the entity set name
	 * @param entityType the entity type
	 * @param entityClass the entity class
	 */
	public AtomFeedCursorHandler(String entitySetName, EntityType entityType, Class<?> entityClass) {
		this.entitySetName = entitySetName;
		this.entityType = entityType;
		this.entityClass = entityClass;
		this.entities = new ArrayList<T>();
	}

	/* (non-Javadoc)
	 * @see org.restlet.ext.xml.format.FormatParser#parse(java.io.Reader)
	 */
	public Feed parse(Reader reader) {
		return parseFeed(reader, getEntitySet());
	}

	/**
	 * Parses the feed.
	 *
	 * @param reader the reader
	 * @param entitySet the entity set
	 * @return the feed
	 */
	public Feed parseFeed(Reader reader, EntitySet entitySet) {
		try {

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(reader);

			while (xmlStreamReader.hasNext()) {
				xmlStreamReader.next();

				if (isStartElement(xmlStreamReader, ATOM_ENTRY)) {
					@SuppressWarnings("unchecked")
					// create instance of entity, parse it and add it to list of entities
					T entity = (T) entityClass.newInstance();
					this.parseEntry(xmlStreamReader, xmlStreamReader.getLocalName(),
							entitySet, entity);
					this.entities.add(entity);

				} else if (isStartElement(xmlStreamReader, ATOM_LINK)) {
					if ("next".equals(getAttributeValueIfExists(xmlStreamReader,"rel"))) {
						this.getFeed().setBaseReference(getAttributeValueIfExists(xmlStreamReader, "href"));
					}
				} else if (isEndElement(xmlStreamReader, ATOM_FEED)) {
					// return from a sub feed, if we went down the hierarchy
					break;
				}

			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return feed;

	}

	/**
	 * Parses all properties of an entity.
	 *
	 * @param <T> the generic type
	 * @param reader the reader
	 * @param propertiesElement the properties element
	 * @param entity the entity
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public static <T> T parseProperties(
			XMLStreamReader reader, String propertiesElement, T entity) {

		try {
			String propertyName = null;
			while (reader.hasNext()) {
				reader.next();
				if (reader.isEndElement() && reader.getLocalName().equals(propertiesElement)) {
					return entity;
				}
				
				if (reader.isStartElement()
						&& reader.getNamespaceURI()
								.equals(NS_DATASERVICES)) {
	
					String name = reader.getLocalName();
					String typeAttribute = getAttributeValueIfExists(reader, M_TYPE.getLocalPart());
					String nullAttribute = getAttributeValueIfExists(reader, M_NULL.getLocalPart());
					boolean isNull = nullAttribute != null
							&& "true".equals(nullAttribute);
	

					propertyName = ReflectUtils.normalize(name);
					@SuppressWarnings("unused")
					Class<?> javaClass;
					Object value = null;
					if(typeAttribute == null){ // Simple String
						value = reader.getElementText();;
					}else if(typeAttribute.toLowerCase().startsWith("edm") && !isNull){ // EDM Type
						javaClass = TypeUtils.toJavaClass(typeAttribute);
						value = TypeUtils.fromEdm(reader.getElementText(), typeAttribute);
					}else if(typeAttribute.toLowerCase().startsWith("collection")){// collection type
						Object o = ReflectUtils.getPropertyObject(entity, propertyName);
						// Delegate the collection handling to respective handler.
						CollectionPropertyCursorHandler.parse(reader, o, reader.getLocalName(), entity); 
					} else if(!isNull){// complex type
						// get or create the property instance
						Object o = ReflectUtils.getPropertyObject(entity, propertyName);
						// populate the object
						parseProperties(reader, reader.getLocalName(), (T) o);
						// set it back to parent entity
						ReflectUtils.invokeSetter(entity, propertyName, o);
					}
					
					if(value!= null){
						ReflectUtils.invokeSetter(entity, propertyName, value);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return entity;
	}

	/**
	 * Parses the ds atom entry.
	 *
	 * @param entityType the entity type
	 * @param reader the reader
	 * @param event the event
	 * @param entity the entity
	 */
	private void parseDSAtomEntry(EntityType entityType, XMLStreamReader reader, String startElement, T entity) {
		// as end element is not included in parseProperties, we need a wrapper method around it to handle it.
		AtomFeedCursorHandler.parseProperties(reader, startElement, entity);
	}

	//TODO: Onkar implement this method later
	/*private static String innerText(XMLEventReader reader, StartElement element) {
		try {
			StringWriter sw = new StringWriter();
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			XMLEventWriter writer;

			writer = factory.createXMLEventWriter(sw);
			while (reader.hasNext()) {

				XMLEvent event = reader.nextEvent();
				if (event.isEndElement()
						&& event.asEndElement().getName()
								.equals(element.getName())) {

					return sw.toString();
				} else {
					writer.add(event);
				}

			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		throw new RuntimeException();
	}*/



	/**
	 * Gets the entity set.
	 *
	 * @return the entity set
	 */
	private EntitySet getEntitySet() {		
		EntitySet entitySet = new EntitySet(entitySetName);
		entitySet.setType(entityType);
		
		return entitySet;
	}

	/**
	 * Parses the entry.
	 *
	 * @param reader the reader
	 * @param entryElement the entry element
	 * @param entitySet the entity set
	 * @param entity the entity
	 * @return the t
	 */
	private T parseEntry(XMLStreamReader reader,
			String entryElement, EntitySet entitySet, T entity) {

		String id = null;
		String title = null;
		String summary = null;
		String updated = null;
		String contentType = null;
		List<Link> atomLinks = new ArrayList<Link>();

		//String etag = getAttributeValueIfExists(entryElement, M_ETAG);

		Entry rt = null;

		try {
			while (reader.hasNext()) {
				reader.next();
				if (isEndElement(reader, ATOM_ENTRY)) {
					rt.setId(id); // http://localhost:8810/Oneoff01.svc/Comment(1)
					rt.setTitle(title);
					rt.setSummary(summary);
					rt.setUpdated(Date.valueOf(updated.split("T")[0]));	
					return entity;
				}
	
				if (isStartElement(reader, ATOM_ID)) {
					id = reader.getElementText();
				} else if (isStartElement(reader, ATOM_TITLE)) {
					title = reader.getElementText();
				} else if (isStartElement(reader, ATOM_SUMMARY)) {
					summary = reader.getElementText();
				} else if (isStartElement(reader, ATOM_UPDATED)) {
					updated = reader.getElementText();
				} else if (isStartElement(reader, ATOM_LINK)) {
					Link link = parseAtomLink(reader, reader.getLocalName(),
							entitySet, entity);
					atomLinks.add(link);
				} else if (isStartElement(reader, M_PROPERTIES)) {
					parseDSAtomEntry(entitySet.getType(), reader, reader.getLocalName(), entity);
				/*} else if (isStartElement(event, M_ACTION)) {
					AtomFunction function = parseAtomFunction(reader,
							event.asStartElement());
					actions.put(function.getFQFunctionName(), metadata
							.findFunctionImport(function.title,
									entitySet.getType(), FunctionKind.Action));
				} else if (isStartElement(event, M_FUNCTION)) {
					AtomFunction function = parseAtomFunction(reader,
							event.asStartElement());
					functions.put(function.getFQFunctionName(), metadata
							.findFunctionImport(function.title,
									entitySet.getType(), FunctionKind.Function));*/
				} else if (isStartElement(reader, ATOM_CONTENT)) {
					contentType = getAttributeValueIfExists(reader, M_TYPE.getLocalPart());
					if (MediaType.APPLICATION_XML.getName().equals(contentType)) {
						while (reader.hasNext()) {
							try {
								// handle content in separate handlers
								reader.next();
								if (reader.isStartElement()) {
									if (isStartElement(reader, M_PROPERTIES)) {
										rt = new Entry();
										this.parseDSAtomEntry(entitySet.getType(), reader, reader.getLocalName(), entity);
									} else {
										// TODO: Onkar : Set Basic content by implementing innerText method later
										//BasicAtomEntry bae = new BasicAtomEntry();
										Entry bae = new Entry();
										//bae.content = innerText(reader, event2.asStartElement());
										rt = bae;
									}
								}
								if (isEndElement(reader, ATOM_CONTENT)) {
									break;
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} else {
						Entry e = new Entry();
						// TODO: Onkar : Set Basic content by implementing innerText method later
						//e.setContent(innerText(reader, event.asStartElement()));
						rt = e;
					}
				}
			} // end while
		} catch (XMLStreamException e1) {
			e1.printStackTrace();
		}
		throw new RuntimeException();
	}
	
	/**
	 * Parses the atom link.
	 *
	 * @param reader the reader
	 * @param linkElement the link element
	 * @param entitySet the entity set
	 * @param entity the entity
	 * @return the link
	 */
	@SuppressWarnings("unchecked")
	private Link parseAtomLink(XMLStreamReader reader, String linkElement,
			EntitySet entitySet, T entity) {

		try {
			Link rt = new Link();
			rt.setRel(Relation.valueOf(getAttributeValueIfExists(reader,
					"rel")));
			rt.setType(MediaType.valueOf(getAttributeValueIfExists(reader,
					"type")));
			rt.setTitle(getAttributeValueIfExists(reader, "title"));
			rt.setHref(new Reference(getAttributeValueIfExists(reader,
					"href")));
			//boolean inlineContent = false;

			// expected cases:
			// 1. </link> - no inlined content, i.e. deferred
			// 2. <m:inline/></link> - inlined content but null entity or empty feed
			// 3. <m:inline><feed>...</m:inline></link> - inlined content with 1 or more items
			// 4. <m:inline><entry>..</m:inline></link> - inlined content 1 an item

			while (reader.hasNext()) {
				reader.next();

				if (reader.isEndElement()
						&& reader.getLocalName().equals(linkElement)) {
					break;
				} else if (isStartElement(reader, XmlFormatParser.M_INLINE)) {
					//inlineContent = true; // may be null content.
				} else if (isStartElement(reader, ATOM_FEED)) {
					// rt.inlineFeed = parseFeed(reader, targetEntitySet);
				} else if (isStartElement(reader, ATOM_ENTRY)) { //handle the inline entity  
					String propertyName = rt.getHref().getLastSegment();
					// create a property object
					Object o = ReflectUtils.getPropertyObject(entity, propertyName);
					// populate the object 
					this.parseEntry(reader, reader.getLocalName(), entitySet, (T) o);
					// set it back to parent entity
					ReflectUtils.invokeSetter(entity, propertyName, o);
				}
			}
			return rt;
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
