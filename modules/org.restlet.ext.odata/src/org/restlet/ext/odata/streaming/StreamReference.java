package org.restlet.ext.odata.streaming;

import java.io.IOException;
import java.io.InputStream;

import org.restlet.data.Reference;
import org.restlet.ext.odata.Service;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;


/**
 * The Class StreamReference.
 * This class stores the Stream data reference and its contenType.
 * One can read, Create and update stream data by using this class. 
 */
public class StreamReference extends Reference {
	
	/** The input stream. */
	private InputStream inputStream;
	
	/** The content type. */
	private String contentType;
	
	/** isUpdateStreamData needs to be set to true for Stream Upadate */
	private boolean isUpdateStreamData;
	/**
	 * Instantiates a new stream reference.
	 * This method is used to create StreamReference with the URL.
	 *
	 * @param baseRef the base ref
	 * @param uriRef the uri ref
	 */
	   public StreamReference(Reference baseRef, String uriRef) {
		   super(baseRef,uriRef);
	   }
	
	
	   /**
   	 * Instantiates a new stream reference.
   	 * Use this to instantiates stream reference for doing create.
   	 *
   	 * @param contentType the content type
   	 * @param inputStream the input stream
   	 * @param isCreate the is create
   	 */
	public StreamReference(String contentType, InputStream inputStream) {
		this.contentType = contentType;
		this.inputStream = inputStream;
	}


	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}
	/**
	 * @param contentType the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	/**
	 * Gets the input stream.
	 * This method is used to read a InputStream data.
	 * 
	 * @param service the service
	 * @return the inputStream
	 */
	public InputStream getInputStream(Service service) {
		ClientResource cr = service.createResource(this);
		Representation representation = cr.get();
		if (representation != null) {
			try {
				inputStream = representation.getStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return inputStream;
	}	
	
	/**
	 * @param inputStream the inputStream to set
	 */
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}	

	/**
	 * Gets the input stream.
	 *
	 * @return the input stream
	 */
	public InputStream getInputStream() {
		return inputStream;
	}


	/**
	 * Checks if is update stream data.
	 *
	 * @return the isUpdateStreamData
	 */
	public boolean isUpdateStreamData() {
		return isUpdateStreamData;
	}


	/**
	 * Sets the update stream data.
	 *
	 * @param isUpdateStreamData the isUpdateStreamData to set
	 */
	public void setUpdateStreamData(boolean isUpdateStreamData) {
		this.isUpdateStreamData = isUpdateStreamData;
	}
	
}
