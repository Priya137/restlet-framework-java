package org.restlet.ext.odata.batch.request.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.ext.odata.Service;
import org.restlet.ext.odata.batch.request.BatchRequest;
import org.restlet.ext.odata.batch.request.ChangeSetRequest;
import org.restlet.ext.odata.batch.request.ClientBatchRequest;
import org.restlet.ext.odata.batch.response.BatchResponse;
import org.restlet.ext.odata.batch.util.BatchConstants;
import org.restlet.ext.odata.batch.util.BodyPart;
import org.restlet.ext.odata.batch.util.Multipart;
import org.restlet.ext.odata.batch.util.RestletBatchRequestHelper;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.util.WrapperRepresentation;

/**
 * The Class BatchRequestImpl forms the base class for batch request. <br>
 * It maintains the list of clientBatchRequests within a batch.
 * 
 * copyright 2014 Halliburton
 * 
 * @author <a href="mailto:Amit.Jahagirdar@synerzip.com">Amit.Jahagirdar</a>
 */
public class BatchRequestImpl implements BatchRequest {

	/** The service. */
	private Service service;

	/** The requests. */
	private List<ClientBatchRequest> requests = new ArrayList<ClientBatchRequest>();

	/**
	 * Instantiates a new batch request impl.
	 * 
	 * @param service
	 *            the service
	 */
	public BatchRequestImpl(Service service) {
		this.service = service;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.ext.odata.batch.request.BatchRequest#addRequest(org.restlet
	 * .ext.odata.batch.request.impl.GetEntityRequest)
	 */
	public BatchRequestImpl addRequest(GetEntityRequest getEntityRequest) {
		requests.add(getEntityRequest);
		return this;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.ext.odata.batch.request.BatchRequest#addRequest(org.restlet
	 * .ext.odata.batch.request.ChangeSetRequest)
	 */
	public BatchRequestImpl addRequest(ChangeSetRequest changeSetRequest) {
		requests.add(changeSetRequest);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.ext.odata.batch.request.BatchRequest#execute()
	 */
	public List<BatchResponse> execute() {

		ClientResource clientResource = service.createResource(new Reference(
				service.getServiceRef()));
		clientResource.getRequest().getResourceRef().setLastSegment("");
		// create the client Info
		ClientInfo clientInfo = new ClientInfo();
		clientResource.getRequest().setClientInfo(clientInfo);

		Context context = new Context();
		Client client = new Client(context, Protocol.HTTP);
		client.getContext().getParameters()
				.add("useForwardedForHeader", "false");

		Reference resourceRef = clientResource.getRequest().getResourceRef();
		clientResource.getRequest().setResourceRef(
				new Reference(resourceRef.getTargetRef()
						+ BatchConstants.BATCH_ENDPOINT_URI));
		clientResource.getRequest().setMethod(Method.POST);
		clientResource.setNext(client);

		String batchId = BatchConstants.BATCH_UNDERSCORE
				+ UUID.randomUUID().toString();
		StringBuilder sb = new StringBuilder();
		List<ClientBatchRequest> list = this.requests;

		for (ClientBatchRequest restletBatchRequest : list) {
			if (restletBatchRequest instanceof GetEntityRequest) {
				sb.append("\n--").append(batchId).append("\n");
				sb.append(restletBatchRequest.format(MediaType.APPLICATION_ATOM
						.toString()));
			} else if (restletBatchRequest instanceof ChangeSetRequest) {
				sb.append("\n--").append(batchId).append("\n");
				sb.append(restletBatchRequest.format(MediaType.APPLICATION_ATOM
						.toString()));
			}
		}
		sb.append("\n--").append(batchId).append("--\n");
		//Finally posting the batch request.
		Representation r = clientResource.post(new StringRepresentation(sb
				.toString(), new MediaType(MediaType.MULTIPART_MIXED
				+ ";boundary=" + batchId)));
		
		List<BatchResponse> batchResponses = null;
		try {
			batchResponses = parseRepresentation(r, list);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return batchResponses;
	}

	/**
	 * This method parses the representation and returns the list of batch
	 * responses.
	 * 
	 * @param r
	 *            representation
	 * @param list
	 *            the list
	 * @return list of batchResponses.
	 * @throws IOException
	 */
	private List<BatchResponse> parseRepresentation(Representation r,
			List<ClientBatchRequest> list) throws IOException {

		// This would hold individual batch responses
		List<BatchResponse> batchResultList = new ArrayList<BatchResponse>(
				list.size());

		MediaType mediaType = ((WrapperRepresentation) r).getMediaType();
		Multipart baseMultiPart = RestletBatchRequestHelper.createMultipart(
				r.getStream(), mediaType);
		int i = 0;
		BatchResponse bResponse = null;
		List<BodyPart> subBodyParts = baseMultiPart.getBodyParts();
		for (BodyPart bp : subBodyParts) {
			// Its a changeset
			if (bp.getMediaType().isCompatible(MediaType.MULTIPART_MIXED)) {
				Multipart mp = RestletBatchRequestHelper.createMultipart(
						bp.getInputStream(), bp.getMediaType());
				List<String> contentList = new ArrayList<String>();
				List<BodyPart> bodyParts = mp.getBodyParts();
				for (BodyPart bodyPart : bodyParts) {
					contentList
							.add(RestletBatchRequestHelper
									.getStringFromInputStream(bodyPart
											.getInputStream()));
				}
				ChangeSetRequest csr = (ChangeSetRequest) list.get(i);
				bResponse = RestletBatchRequestHelper.parseChangeSetResponse(
						BatchConstants.ODATAVERSION, contentList, csr,
						mediaType, service);
			} else {
				ClientBatchRequest batchRequestOfTypeGet = list.get(i);
				String content = RestletBatchRequestHelper
						.getStringFromInputStream(bp.getInputStream());
				bResponse = RestletBatchRequestHelper
						.parseSingleOperationResponse(
								BatchConstants.ODATAVERSION, content,
								batchRequestOfTypeGet, bp.getMediaType(),
								service);
			}
			batchResultList.add(bResponse);
			i++;
		}
		return batchResultList;

	}

}
