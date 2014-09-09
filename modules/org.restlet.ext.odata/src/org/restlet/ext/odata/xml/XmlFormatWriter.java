package org.restlet.ext.odata.xml;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.logging.Logger;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.ext.odata.internal.edm.ComplexProperty;
import org.restlet.ext.odata.internal.edm.EntityType;
import org.restlet.ext.odata.internal.edm.Metadata;
import org.restlet.ext.odata.internal.edm.Property;
import org.restlet.ext.odata.internal.edm.TypeUtils;
import org.restlet.ext.odata.validation.annotation.SystemGenerated;
import org.restlet.ext.xml.SaxRepresentation;
import org.restlet.ext.xml.XmlWriter;
import org.restlet.ext.xml.format.XmlFormatParser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class will be used to write the entry in XML format.
 *
 */
public class XmlFormatWriter extends SaxRepresentation {
	
	/** WCF data services metadata namespace. */
    public final static String WCF_DATASERVICES_METADATA_NAMESPACE = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata";

    /** WCF data services namespace. */
    public final static String WCF_DATASERVICES_NAMESPACE = "http://schemas.microsoft.com/ado/2007/08/dataservices";

    /** WCF data services scheme namespace. */
    public final static String WCF_DATASERVICES_SCHEME_NAMESPACE = "http://schemas.microsoft.com/ado/2007/08/dataservices/scheme";

	public Metadata metadata;
	
	public Object entity;
	
	public boolean isPostRequest;
	
	/** The internal logger. */
    private Logger logger;
 

    public XmlFormatWriter(Metadata metadata, Object entity,
			boolean isPostRequest) {
    	super(MediaType.APPLICATION_XML);
		this.metadata = metadata;
		this.entity = entity;
		this.isPostRequest = isPostRequest;
	}

	@Override
    public void write(XmlWriter writer) throws IOException {
        try {
            // Attribute for nullable values.
            AttributesImpl nullAttrs = new AttributesImpl();
            nullAttrs.addAttribute(
                    WCF_DATASERVICES_METADATA_NAMESPACE,
                    "null", null, "boolean", "true");
            writer.forceNSDecl(
                    WCF_DATASERVICES_METADATA_NAMESPACE, "m");
            writer.forceNSDecl(WCF_DATASERVICES_NAMESPACE, "d");
            writer.startElement(
                    WCF_DATASERVICES_METADATA_NAMESPACE,
                    "properties");
            this.write(writer, entity, nullAttrs);
            writer.endElement(
                    WCF_DATASERVICES_METADATA_NAMESPACE,
                    "properties");
        } catch (SAXException e) {
            throw new IOException(e.getMessage());
        }
    }

    private void write(XmlWriter writer, Object entity,
            AttributesImpl nullAttrs) throws SAXException {
        for (Field field : entity.getClass()
                .getDeclaredFields()) {
        	SystemGenerated systemGeneratedAnnotation = field
        			.getAnnotation(SystemGenerated.class);
            String getter = "get"
                    + field.getName().substring(0, 1)
                            .toUpperCase()
                    + field.getName().substring(1);
            Property prop = ((Metadata) getMetadata())
                    .getProperty(entity, field.getName());

			if (prop != null && systemGeneratedAnnotation == null && isPostRequest) {
                this.writeProperty(writer, entity, prop, getter,
                        nullAttrs);
            }
			else if (prop != null && !isPostRequest) {
				this.writeProperty(writer, entity, prop, getter,
                        nullAttrs);
            }
        }
    }
    
    /**
     * Write collection property element.
     *
     * @param writer the writer
     * @param entity the entity
     * @param value the value
     * @param prop the prop
     * @param nullAttrs the null attrs
     * @throws SAXException the SAX exception
     */
    private void writeCollectionProperty(XmlWriter writer, Object entity, Object value, Property prop, AttributesImpl nullAttrs) throws SAXException {
		if (value instanceof List) {
			try {
				Field field = entity.getClass().getDeclaredField(
						prop.getName());
				if (field.getGenericType() instanceof ParameterizedType) {
					// determine what type of collection it is
					ParameterizedType listType = (ParameterizedType) field
							.getGenericType();
					Class<?> listClass = (Class<?>) listType
							.getActualTypeArguments()[0]; // get the parameterized class 
					String mType = null;
					boolean isPrimitiveCollection = TypeUtils.isPrimitiveCollection(listClass);
					AttributesImpl typeAttr = new AttributesImpl();
					if(isPrimitiveCollection){ // collection of primitives
						mType = "Collection("+ TypeUtils.toEdmType(listClass.getName()) + ")";
					}else{	// collection of complex
						String[] className = listClass.getName().split("\\.");
						String nameSpace = !getMetadata().getSchemas()
								.isEmpty() ? (getMetadata().getSchemas().get(0))
								.getNamespace().getName() : "";
						String collectionType = !nameSpace.equals("") ? nameSpace
								+ "." + className[3]
								: className[3];
						mType = "Collection(" + collectionType + ")";
					}
					List<?> obj = (List<?>) value;
					// write collection property tag 
					typeAttr.addAttribute(
						WCF_DATASERVICES_METADATA_NAMESPACE,
						"type", "type", "string", mType);
					if(obj.size() == 0){
						typeAttr.addAttribute(
                                WCF_DATASERVICES_METADATA_NAMESPACE,
                                "null", null, "boolean", "true");
					}
					writer.startElement(
						WCF_DATASERVICES_NAMESPACE,
						prop.getName(), prop.getName(),
						typeAttr);
					// write element tags
					for (Object object : obj) {
						if(isPrimitiveCollection){
							if(object.toString().length()>0){
								writer.dataElement(WCF_DATASERVICES_NAMESPACE, XmlFormatParser.DATASERVICES_ELEMENT.getLocalPart(), object.toString());
							}else{
								writer.emptyElement(
										WCF_DATASERVICES_NAMESPACE,
										XmlFormatParser.DATASERVICES_ELEMENT.getLocalPart(), XmlFormatParser.DATASERVICES_ELEMENT.getLocalPart(),
										nullAttrs);
							}
						}else{ // complex collection
							writer.startElement(
									WCF_DATASERVICES_NAMESPACE,
									XmlFormatParser.DATASERVICES_ELEMENT.getLocalPart());
							// write complex property under <element></element>
							this.write(writer, object, nullAttrs);
							writer.endElement(
									WCF_DATASERVICES_NAMESPACE,
									XmlFormatParser.DATASERVICES_ELEMENT.getLocalPart());
						}
					}
				}
			} catch (SecurityException e) {
				getLogger().warning(
                        "Can't write the collection property: " + e.getMessage());
			} catch (NoSuchFieldException e) {
				getLogger().warning(
                        "Can't write the collection property: " + e.getMessage());
			}
		}
    }

    private void writeProperty(XmlWriter writer, Object entity,
            Property prop, String getter,
            AttributesImpl nullAttrs) throws SAXException {
        for (Method method : entity.getClass()
                .getDeclaredMethods()) {
            if (method.getReturnType() != null
                    && getter.equals(method.getName())
                    && method.getParameterTypes().length == 0) {
                Object value = null;

				try {
					value = method.invoke(entity,
							(Object[]) null);
				} catch (Exception e) {
					getLogger().warning(
	                        "Error occurred while invoking the method : " + e.getMessage());
				}

				if (value != null) {
					AttributesImpl typeAttr = new AttributesImpl();
					if (prop instanceof ComplexProperty) { // if this is collection or complex type
						if (value instanceof List) { // collection
							this.writeCollectionProperty(writer,
									entity, value, prop, nullAttrs);
						} else { // complex type
							EntityType type = ((Metadata) getMetadata()).getEntityType(entity.getClass());
							// prefix the namespace for m:type 
							String packageName = type.getSchema().getNamespace().getName() + "." ;
							typeAttr.addAttribute(
									WCF_DATASERVICES_METADATA_NAMESPACE,
									"type", "type", "string",packageName + 
									((ComplexProperty) prop)
											.getComplexType()
											.getName());
							writer.startElement(
									WCF_DATASERVICES_NAMESPACE,
									prop.getName(), prop.getName(),
									typeAttr);
							// write data
							this.write(writer, value, nullAttrs);
						}
					} else {
						typeAttr.addAttribute(
								WCF_DATASERVICES_METADATA_NAMESPACE,
								"type", "type", "string", prop
										.getType().getName());
						writer.startElement(
								WCF_DATASERVICES_NAMESPACE,
								prop.getName(), prop.getName(),
								typeAttr);
						writer.characters(TypeUtils.toEdm(
								value, prop.getType()));
					}

					writer.endElement(
							WCF_DATASERVICES_NAMESPACE,
							prop.getName());
				} else {
					if (prop.isNullable()) {
						writer.emptyElement(
								WCF_DATASERVICES_NAMESPACE,
								prop.getName(), prop.getName(),
								nullAttrs);
					} else {
						getLogger().warning(
								"The following property has a null value but is not marked as nullable: "
										+ prop.getName());
						writer.emptyElement(
								WCF_DATASERVICES_NAMESPACE,
								prop.getName());
					}
				}
				break;
			}
        }
    }

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public Object getEntity() {
		return entity;
	}

	public void setEntity(Object entity) {
		this.entity = entity;
	}

	public boolean isPostRequest() {
		return isPostRequest;
	}

	public void setPostRequest(boolean isPostRequest) {
		this.isPostRequest = isPostRequest;
	}
    
	/**
     * Returns the current logger.
     * 
     * @return The current logger.
     */
    private Logger getLogger() {
        if (logger == null) {
            logger = Context.getCurrentLogger();
        }
        return logger;
    }


}
