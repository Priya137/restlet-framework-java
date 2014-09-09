package org.restlet.ext.odata.json;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.engine.util.Base64;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.odata.internal.edm.ComplexProperty;
import org.restlet.ext.odata.internal.edm.Metadata;
import org.restlet.ext.odata.internal.edm.Property;
import org.restlet.ext.odata.internal.edm.Type;
import org.restlet.ext.odata.internal.edm.TypeUtils;
import org.restlet.ext.odata.validation.annotation.SystemGenerated;

/**
 * 
 * This class can be used for representing the entity on 
 * JSON verbose format for POST operation. *
 */

public class JsonFormatWriter extends JsonRepresentation {

	/** WCF data services metadata namespace. */
	public final static String WCF_DATASERVICES_METADATA_NAMESPACE = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata";
	/** WCF data services namespace. */
	public final static String WCF_DATASERVICES_NAMESPACE = "http://schemas.microsoft.com/ado/2007/08/dataservices";
	/** WCF data services scheme namespace. */
	public final static String WCF_DATASERVICES_SCHEME_NAMESPACE = "http://schemas.microsoft.com/ado/2007/08/dataservices/scheme";
	private static final String DATETIME_JSON_SUFFIX = ")\\/\"";
	private static final String DATETIME_JSON_PREFIX = "\"\\/Date(";
	public Metadata metadata;
	public Object entity;
	public boolean isPostRequest;
	/** The internal logger. */
	private Logger logger;


	/**
	 * Instantiates a new json format writer.
	 *
	 * @param jsonArray the json array
	 * @param metadata the metadata
	 * @param entity the entity
	 * @param isPostRequest the is post request
	 */
	public JsonFormatWriter(Object jsonArray, Metadata metadata,
			Object entity, boolean isPostRequest) {
		super(jsonArray);
		this.metadata = metadata;
		this.entity = entity;
		this.isPostRequest = isPostRequest;
	}

	/* (non-Javadoc)
	 * @see org.restlet.ext.json.JsonRepresentation#write(java.io.Writer)
	 */
	@Override
	public void write(Writer writer) throws IOException {
		JsonWriter jw = new JsonWriter(writer);
		jw.startObject(); // {
		{
			this.writeEntity(jw, entity);
		}
		jw.endObject(); // }
	}

	
	/**
	 * API will write the entity and properties within 
	 * an entity in JSON format.
	 *
	 * @param writer the writer
	 * @param entity the entity
	 */
	private void writeEntity(JsonWriter writer, Object entity) {
		boolean isFirst = true;
		for (Field field : entity.getClass().getDeclaredFields()) {
			SystemGenerated systemGeneratedAnnotation = field
					.getAnnotation(SystemGenerated.class);
			String getter = "get"
					+ field.getName().substring(0, 1).toUpperCase()
					+ field.getName().substring(1);
			Property prop = ((Metadata) getMetadata()).getProperty(entity,
					field.getName());

			if (prop != null && systemGeneratedAnnotation == null
					&& isPostRequest) {
				if (isFirst) {
					isFirst = false;
				} else {
					writer.writeSeparator();
				}
				this.writeProperty(writer, entity, prop, getter);
			} else if (prop != null && !isPostRequest) {
				if (isFirst) {
					isFirst = false;
				} else {
					writer.writeSeparator();
				}
				this.writeProperty(writer, entity, prop, getter);
			}
		}
	}

	/**
	 * Write property.
	 *
	 * @param writer the writer
	 * @param entity the entity
	 * @param prop the prop
	 * @param getter the getter
	 */
	private void writeProperty(JsonWriter writer, Object entity, Property prop,
			String getter) {
		
		for (Method method : entity.getClass().getDeclaredMethods()) {
			if (method.getReturnType() != null
					&& getter.equals(method.getName())
					&& method.getParameterTypes().length == 0) {
				Object value = null;

				try {
					value = method.invoke(entity, (Object[]) null);
				} catch (Exception e) {
					getLogger().warning(
							"Error occurred while invoking the method : "
									+ e.getMessage());
				}

				if (value != null) {
					if (prop instanceof ComplexProperty) { 
						// if this is collection or complex type
						if (value instanceof List) { 
							// collection
							writer.writeName(prop.getName());
							Type type = prop.getType();
							this.writeCollection(writer, type, prop, value, entity);
						} else { 
							// complex type
							writer.writeName(prop.getName());
							this.writeComplexType(writer, value);
						}
					} else {
						writer.writeName(prop.getName());
						Type type = prop.getType();
						this.writePropertyValue(writer, type, prop, value);
					}
				}else{
					// property value is null so write the null value to writer
				    writer.writeName(prop.getName());
					writer.writeNull();
				} 
				break;
			}
		}
	}
	
	/**
	 * Write collection.
	 *
	 * @param writer the writer
	 * @param type the type
	 * @param prop the prop
	 * @param pvalue the pvalue
	 * @param entity the entity
	 */
	protected void writeCollection(JsonWriter writer, Type type, Property prop,
			Object pvalue, Object entity) {
		writer.startObject(); // {
		{
			writer.writeName("results");
			writer.startArray(); // [
			{
				try {
					Field field = entity.getClass().getDeclaredField(
							prop.getName());
					if (field.getGenericType() instanceof ParameterizedType) {
						// determine what type of collection it is
						ParameterizedType listType = (ParameterizedType) field
								.getGenericType();
						Class<?> listClass = (Class<?>) listType
								.getActualTypeArguments()[0];
						// get the parameterized class
						
						boolean isPrimitiveCollection = TypeUtils.isPrimitiveCollection(listClass);
						
						List<?> obj = (List<?>) pvalue;
						boolean isFirst = true;
						@SuppressWarnings("unchecked")
						Iterator<Object> iter = (Iterator<Object>) obj
								.iterator();
						while (iter.hasNext()) {
							Object object = iter.next();
							if (isFirst) {
								isFirst = false;
							} else {
								writer.writeSeparator();
							}
							if (isPrimitiveCollection) {
								// collection of primitive 
								Type collectionType = new Type(TypeUtils.toEdmType(listClass.getName()));
								this.writePropertyValue(writer, collectionType, prop, object);
							} else {
								// complex object which should be 
								// handled as a separate entity
								this.writeComplexType(writer, object);
							}
						}
					}// end if
				} catch (SecurityException e) {
					getLogger().warning(
							"Can't write the collection property: "
									+ e.getMessage());
				} catch (NoSuchFieldException e) {
					getLogger().warning(
							"Can't write the collection property: "
									+ e.getMessage());
				}
				writer.endArray(); // ]
			}// end array
			writer.endObject(); // }
		}// end object
	}
	
	/*
	 * Write complex type.
	 *
	 * @param writer the writer
	 * @param value the value
	 */
	private void writeComplexType(JsonWriter writer, Object value) {
		writer.startObject();
		{
			this.writeEntity(writer, value);
		}
		writer.endObject();
		
	}
	
	/**
	 * API to write the property value.
	 *
	 * @param writer - the writer
	 * @param type - the type
	 * @param prop - the property
	 * @param pvalue - the property value
	 */
	protected void writePropertyValue(JsonWriter writer, Type type, Property prop, Object pvalue) {
		if (pvalue == null) {
			writer.writeNull();
		} else if (type.getName().endsWith("Binary")) {
			writer.writeString(Base64.encode((byte[]) pvalue, false));
		} else if (type.getName().endsWith("Boolean")) {
			writer.writeBoolean((Boolean) pvalue);
		} else if (type.getName().endsWith("Byte")) {
			writer.writeString(Byte.toString((Byte) pvalue));
		} else if (type.getName().endsWith("DateTime")) {
			writer.writeRaw(formatDateTimeForJson((Date) pvalue));
		} else if (type.getName().endsWith("Decimal")) {
			writer.writeString(((BigDecimal) pvalue).toPlainString());
		} else if (type.getName().endsWith("Double")) {
			writer.writeString(pvalue.toString());
		} else if (type.getName().endsWith("Guid")) {
			writer.writeString(pvalue.toString());
		} else if (type.getName().endsWith("Int16")) {
			writer.writeNumber((Short) pvalue);
		} else if (type.getName().endsWith("Int32")) {
			writer.writeNumber((Integer) pvalue);
		} else if (type.getName().endsWith("Int64")) {
			writer.writeNumber((Long) pvalue);
		} else if (type.getName().endsWith("Single")) {
			writer.writeNumber((Float) pvalue);
		} else if (type.getName().endsWith("Time")) {
			writer.writeRaw(formatDateTimeForJson((Date) pvalue));
		} else if (type.getName().endsWith("DateTimeOffset")) {
			writer.writeRaw(formatDateTimeForJson((Date) pvalue));
		} else if (pvalue instanceof List) {
		      writeCollection(writer, type, prop, pvalue, entity);
	    }else {
			String value = pvalue.toString();
			writer.writeString(value);
		}
	}


	/*
	 * Format date time for json.
	 *
	 * @param date the date
	 * @return the string
	 */
	private String formatDateTimeForJson(Date date) {
		return DATETIME_JSON_PREFIX + date.getTime()
				+ DATETIME_JSON_SUFFIX;
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