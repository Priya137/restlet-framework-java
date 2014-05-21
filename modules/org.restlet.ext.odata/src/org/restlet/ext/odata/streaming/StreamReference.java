package org.restlet.ext.odata.streaming;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.Reference;
import org.restlet.ext.odata.Service;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * This class stores the Stream data reference and its contentType.One can read, Create and update stream data by using
 * this class.<br>
 * <br>
 * 
 * The {@link InputStream} to be read as Media Link Entry(MLE)/Stream from the server is fetched lazily using a call to
 * the {@link #getInputStream(Service)} method. Please note that it is essential to pass the {@link Service} to the
 * method as the service instance holds the authenticated credentials that needs to be passed as part of this client
 * request.<br>
 * <br>
 * 
 * <b>DO NOT</b> use the {@link #getInputStream()} method for lazy fetching a stream; this method is used internally by
 * the framework while saving the stream.When saving a MLE, please use the {@link #setInputStream(InputStream)} method
 * to set the reference to the {@link InputStream} that you want to persist on the server.<br>
 * <br>
 * 
 * You can use the {@link #getNonBlockingInputStream(Service)} method to lazily read non protocol buffer streams from
 * the OData server.
 */
public class StreamReference extends Reference {

	/** The Constant COPY_BUFFER_SIZE that holds the data for copy from input to output stream; set to 64KB */
	private static final int COPY_BUFFER_SIZE = 1024 * 8 * 8;

	/** The Constant INITIAL_BUFFER_SIZE that is used to initialize the buffer; set to 64 MB */
	private static final int INITIAL_BUFFER_SIZE = 1024 * 1024 * 64;

	/** The input stream. */
	private InputStream inputStream;

	/** The content type. */
	private String contentType;

	/** isUpdateStreamData needs to be set to true for Stream Update */
	private boolean isUpdateStreamData;

	/**
	 * Instantiates a new stream reference. This method is used to create StreamReference with the URL.
	 * 
	 * @param baseRef
	 *            the base ref
	 * @param uriRef
	 *            the uri ref
	 */
	public StreamReference(Reference baseRef, String uriRef) {
		super(baseRef, uriRef);
	}

	/**
	 * Instantiates a new stream reference. Use this to instantiates stream reference for doing create.
	 * 
	 * @param contentType
	 *            the content type
	 * @param inputStream
	 *            the input stream
	 * @param isCreate
	 *            the is create
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
	 * @param contentType
	 *            the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Gets the blocking input stream that is made available by reading the stream contents in memory. This is required
	 * for protocol buffers that send multiple protobuf objects as part of one HTTP response.<br>
	 * <br>
	 * 
	 * This method should <b>be used</b> when the client depends on the entire inputstream being available for
	 * processing say in case of processing Google Protocol Buffer streams.
	 * 
	 * This method should <b>NOT be used</b> when you are downloading file or document based streams that don't need to
	 * be available for client program to process as continous inputstream.
	 * 
	 * @param identifier
	 *            the identifier
	 * @param password
	 *            the password
	 * @return the inputStream
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public InputStream getInputStream(Service service) throws IOException {

		try {
			ClientResource clientResource = service.createResource(this);
			Representation representation = clientResource.get();
			if (representation != null) {
				final InputStream inputStreamFromServer = representation.getStream();
				ByteArrayOutputStream outputByteArrayStream = new ByteArrayOutputStream(
						StreamReference.INITIAL_BUFFER_SIZE);
				// Copy the inout stream to the byte array
				this.copyInputStreamToOutput(inputStreamFromServer, outputByteArrayStream);
				// Provide a stream around the byte array
				this.inputStream = new java.io.ByteArrayInputStream(outputByteArrayStream.toByteArray());
			}
		} catch (IOException ioException) {
			Context.getCurrentLogger().log(Level.WARNING,
					"IO Exception while retrieving the blocking streaming data: " + ioException.getMessage(),
					ioException);
			throw ioException;
		}
		return this.inputStream;
	}

	/**
	 * Gets the non blocking input stream.
	 * 
	 * @param service
	 *            the service
	 * @return the non blocking input stream
	 * @throws Exception
	 *             the exception
	 */
	public InputStream getNonBlockingInputStream(Service service) throws Exception {
		try {
			ClientResource clientResource = service.createResource(this);
			Representation representation = clientResource.get();
			if (representation != null) {
				// Provide a stream based on what is returned by the server
				this.inputStream = representation.getStream();
			}
		} catch (Exception exception) {
			Context.getCurrentLogger().log(Level.WARNING,
					"Exception while retrieving the non blocking streaming data: " + exception.getMessage(), exception);
			throw exception;
		}
		return this.inputStream;
	}

	/**
	 * Copy input stream to output.
	 * 
	 * @param inputStream
	 *            the input stream
	 * @param outStream
	 *            the out stream
	 * @return the long
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void copyInputStreamToOutput(InputStream inputStream, OutputStream outStream) throws IOException {
		byte[] buf = new byte[StreamReference.COPY_BUFFER_SIZE];
		try {
			int n;
			while ((n = inputStream.read(buf)) != -1) {
				outStream.write(buf, 0, n);
			}
		} catch (IOException ioException) {
			throw ioException;
		} finally {
			try {
				outStream.flush();
				inputStream.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * @param inputStream
	 *            the inputStream to set
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
	 * @param isUpdateStreamData
	 *            the isUpdateStreamData to set
	 */
	public void setUpdateStreamData(boolean isUpdateStreamData) {
		this.isUpdateStreamData = isUpdateStreamData;
	}

}
