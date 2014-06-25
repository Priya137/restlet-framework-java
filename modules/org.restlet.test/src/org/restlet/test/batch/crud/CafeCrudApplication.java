
package org.restlet.test.batch.crud;

import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.mime.MIME;
import org.jvnet.mimepull.Header;
import org.jvnet.mimepull.MIMEMessage;
import org.jvnet.mimepull.MIMEPart;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.engine.local.ClapClientHelper;
import org.restlet.ext.odata.batch.util.BatchConstants;
import org.restlet.ext.odata.batch.util.BodyPart;
import org.restlet.ext.odata.batch.util.Multipart;
import org.restlet.representation.StringRepresentation;
import org.restlet.routing.Router;
import org.restlet.util.Series;
/**
 * Sample application that simulates the CUD operation on entities.
 * 
 */
@SuppressWarnings("unused")
public class CafeCrudApplication extends Application {

	private static class MyClapRestlet extends Restlet {
		String file;
		private static final Logger LOGGER = Logger.getLogger(RestletBatchCafeTestCase.class.getName());
		boolean updatable;

		public MyClapRestlet(Context context, String file, boolean updatable) {
			super(context);
			this.file = file;
			this.updatable = updatable;
		}

		@Override
		public void handle(Request request, Response response) {
			
			if (Method.GET.equals(request.getMethod())) {
				String uri = "/"
						+ this.getClass().getPackage().getName()
								.replace(".", "/") + "/" + file;
			Response r = getContext().getClientDispatcher().handle(
						new Request(Method.GET, LocalReference
								.createClapReference(LocalReference.CLAP_CLASS,
										uri + ".xml")));
				response.setEntity(r.getEntity());
				response.setStatus(r.getStatus());
				

			} 
			else if(Method.POST.equals(request.getMethod())) {
				String rep=null;
				String res= null;
				try {
					String uri = "/"
							+ this.getClass().getPackage().getName()
									.replace(".", "/") + "/" + file;
					rep = request.getEntity().getText();
					if(rep.contains(Method.DELETE.toString())){
						LOGGER.info("This is delete method");
						StringBuilder sb = new StringBuilder();
						String filePath = new File(".").getCanonicalPath()+"/src/org/restlet/test/batch/crud/deleteCafeResponse.xml";
						InputStream is = new FileInputStream(new File(filePath));
						
						InputStreamReader isr = new InputStreamReader(is);
						BufferedReader br = new BufferedReader(isr);
						StringBuilder bs = sb.append(br.readLine());
						while(br.ready()){
							sb.append(br.readLine()).append("\n");
						}
						response.setEntity(new StringRepresentation(sb.toString()));
						
						Series<Parameter> parameters = new Series<Parameter>(Parameter.class);
						parameters.add("boundary","batchresponse_1f82a90b-43f1-4276-b20a-59da3437dfa8");
						MediaType mediaType = new MediaType(MediaType.MULTIPART_MIXED.toString(), parameters);
						response.getEntity().setMediaType(mediaType);
						response.setStatus(Status.SUCCESS_NO_CONTENT);
					}else if(rep.contains(Method.PUT.toString())){
						LOGGER.info("This is put method");
						StringBuilder sb = new StringBuilder();
						String filePath = new File(".").getCanonicalPath()+"/src/org/restlet/test/batch/crud/updateCafeResponse.xml";
						InputStream is = new FileInputStream(new File(filePath));
						
						InputStreamReader isr = new InputStreamReader(is);
						BufferedReader br = new BufferedReader(isr);
						StringBuilder bs = sb.append(br.readLine());
						while(br.ready()){
							sb.append(br.readLine()).append("\n");
						}
						response.setEntity(new StringRepresentation(sb.toString()));
						
						Series<Parameter> parameters = new Series<Parameter>(Parameter.class);
						parameters.add("boundary","batchresponse_1f82a90b-43f1-4276-b20a-59da3437dfa8");
						MediaType mediaType = new MediaType(MediaType.MULTIPART_MIXED.toString(), parameters);
						response.getEntity().setMediaType(mediaType);
					}else if(rep.contains(Method.GET.toString())){
						LOGGER.info("This is get method");
						StringBuilder sb = new StringBuilder();
						String filePath = new File(".").getCanonicalPath()+"/src/org/restlet/test/batch/crud/getCafeResponse.xml";
						InputStream is = new FileInputStream(new File(filePath));
						
						InputStreamReader isr = new InputStreamReader(is);
						BufferedReader br = new BufferedReader(isr);
						StringBuilder bs = sb.append(br.readLine());
						while(br.ready()){
							sb.append(br.readLine()).append("\n");
						}
						response.setEntity(new StringRepresentation(sb.toString()));
						
						Series<Parameter> parameters = new Series<Parameter>(Parameter.class);
						parameters.add("boundary","batchresponse_1f82a90b-43f1-4276-b20a-59da3437dfa8");
						MediaType mediaType = new MediaType(MediaType.MULTIPART_MIXED.toString(), parameters);
						response.getEntity().setMediaType(mediaType);
					}else{
						LOGGER.info("This is post method");
						StringBuilder sb = new StringBuilder();
						String filePath = new File(".").getCanonicalPath()+"/src/org/restlet/test/batch/crud/createCafeResponse.xml";
						InputStream is = new FileInputStream(new File(filePath));
						
						InputStreamReader isr = new InputStreamReader(is);
						BufferedReader br = new BufferedReader(isr);
						StringBuilder bs = sb.append(br.readLine());
						while(br.ready()){
							sb.append(br.readLine()).append("\n");
						}
						response.setEntity(new StringRepresentation(sb.toString()));
						
						Series<Parameter> parameters = new Series<Parameter>(Parameter.class);
						parameters.add("boundary","batchresponse_1f82a90b-43f1-4276-b20a-59da3437dfa8");
						MediaType mediaType = new MediaType(MediaType.MULTIPART_MIXED.toString(), parameters);
						response.getEntity().setMediaType(mediaType);
					}
					
				} catch (IOException e) {
					response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				}
				if(null != rep && !rep.isEmpty()){
					response.setStatus(Status.SUCCESS_OK);
				}
				
			}
		}
	}

	@Override
	public Restlet createInboundRoot() {
		getMetadataService().setDefaultCharacterSet(CharacterSet.ISO_8859_1);
		getConnectorService().getClientProtocols().add(Protocol.CLAP);
		Router router = new Router(getContext());

		router.attach("/$metadata", new MyClapRestlet(getContext(), "metadata",
				true));
		router.attach("/Cafes", new MyClapRestlet(getContext(), "cafes", true));
		router.attach("/Cafes('40')", new MyClapRestlet(getContext(),
				"cafesUpdatedRequest", true));
		router.attach("/$batch", new MyClapRestlet(getContext(), "cafes",
				true));
		

		return router;
	}
	
	/**
	 * Creates the multipart.
	 * 
	 * A Multipart is a logical representation of a batch request or Chnagset.
	 * <br> It is a set of multiple http requests/response. 
	 * 
	 * @param is
	 *            the is
	 * @param mediaType
	 *            the media type
	 * @return the multipart
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static Multipart createMultipart(InputStream is, MediaType mediaType)
			throws IOException {
		// create a multipart
		Multipart multipart = new Multipart();
		// set its mediatype
		multipart.setMediaType(mediaType);

		MIMEMessage mimeMessage = new MIMEMessage(is, mediaType.getParameters()
				.getFirstValue(BatchConstants.BATCH_BOUNDARY));
		List<MIMEPart> attachments = mimeMessage.getAttachments();
		for (MIMEPart mimePart : attachments) {
			BodyPart bodyPart = new BodyPart(mimePart);
			// copy headers into bodyparts
			copyHeaders(bodyPart, mimePart);
			bodyPart.setMediaType(new MediaType(bodyPart.getHeaders().getFirst(
					BatchConstants.HTTP_HEADER_CONTENT_TYPE)));
			multipart.addBodyParts(bodyPart);

		}
		return multipart;
	}

	private static void copyHeaders(BodyPart bodyPart, MIMEPart mimePart) {
		MultivaluedMap<String, String> bpHeaders = bodyPart.getHeaders();
		List<? extends Header> mHeaders = mimePart.getAllHeaders();
		for (Header header : mHeaders) {
			bpHeaders.add(header.getName(), header.getValue());
		}
		
	}

}
