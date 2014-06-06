package org.restlet.ext.odata.batch.request.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.restlet.data.MediaType;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.odata.batch.request.ChangeSetRequest;
import org.restlet.ext.odata.batch.request.ClientBatchRequest;
import org.restlet.ext.odata.batch.util.BatchConstants;

/**
 * The Class ChangeSetRequestImpl.
 * 
 * 
 * copyright 2014 Halliburton
 * 
 * @author <a href="mailto:Amit.Jahagirdar@synerzip.com">Amit.Jahagirdar</a>
 */
public class ChangeSetRequestImpl implements ChangeSetRequest {

	/** The reqs. */
	private List<ClientBatchRequest> reqs = new ArrayList<ClientBatchRequest>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.ext.odata.batch.request.ChangeSetRequest#addRequest(org.restlet
	 * .ext.odata.batch.request.impl.CreateEntityRequest)
	 */
	public ChangeSetRequest addRequest(CreateEntityRequest createEntityRequest) {
		reqs.add(createEntityRequest);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.ext.odata.batch.request.ChangeSetRequest#getReqs()
	 */
	public List<ClientBatchRequest> getReqs() {
		return reqs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.ext.odata.batch.request.RestletBatchRequest#format(java.lang
	 * .String)
	 */
	public String format(String formatType) {

		StringBuilder sb = new StringBuilder();
		// nothing to add
		if (reqs == null || reqs.size() == 0) {
			return "";
		}

		String boundary = BatchConstants.CHANGESET
				+ UUID.randomUUID().toString();
		String cType = MediaType.MULTIPART_MIXED + "; "
				+ BatchConstants.BOUNDARY + "=" + boundary;
		sb.append(HeaderConstants.HEADER_CONTENT_TYPE).append(": ")
				.append(cType).append("\n");
		sb.append("\n");

		for (ClientBatchRequest req : reqs) {
			sb.append("\n--").append(boundary).append("\n");
			sb.append(req.format(formatType));
		}

		// ending the change set
		sb.append("\n--").append(boundary).append("--\n");

		return sb.toString();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.ext.odata.batch.request.ChangeSetRequest#addRequest(org.restlet
	 * .ext.odata.batch.request.impl.UpdateEntityRequest)
	 */
	@Override
	public ChangeSetRequest addRequest(UpdateEntityRequest updateEntityRequest) {
		reqs.add(updateEntityRequest);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.ext.odata.batch.request.ChangeSetRequest#addRequest(org.restlet
	 * .ext.odata.batch.request.impl.DeleteEntityRequest)
	 */
	@Override
	public ChangeSetRequest addRequest(DeleteEntityRequest deleteEntityRequest) {
		reqs.add(deleteEntityRequest);
		return this;
	}

}
