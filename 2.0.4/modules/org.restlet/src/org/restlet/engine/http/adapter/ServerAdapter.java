/**
 * Copyright 2005-2010 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.engine.http.adapter;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.List;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.engine.http.HttpRequest;
import org.restlet.engine.http.HttpResponse;
import org.restlet.engine.http.ServerCall;
import org.restlet.engine.http.header.HeaderConstants;
import org.restlet.engine.http.header.HeaderUtils;
import org.restlet.representation.Representation;
import org.restlet.util.Series;

// [excludes gwt]
/**
 * Converter of low-level HTTP server calls into high-level uniform calls.
 * 
 * @author Jerome Louvel
 */
public class ServerAdapter extends Adapter {

    /**
     * Constructor.
     * 
     * @param context
     *            The client context.
     */
    public ServerAdapter(Context context) {
        super(context);
    }

    /**
     * Adds the entity headers for the handled uniform call.
     * 
     * @param response
     *            The response returned.
     */
    protected void addEntityHeaders(HttpResponse response) {
        Series<Parameter> responseHeaders = response.getHttpCall()
                .getResponseHeaders();
        Representation entity = response.getEntity();
        HeaderUtils.addEntityHeaders(entity, responseHeaders);
    }

    /**
     * Adds the response headers for the handled uniform call.
     * 
     * @param response
     *            The response returned.
     */
    protected void addResponseHeaders(HttpResponse response) {
        try {
            // Add all the necessary headers
            HeaderUtils.addGeneralHeaders(response, response.getHttpCall()
                    .getResponseHeaders());
            HeaderUtils.addResponseHeaders(response, response.getHttpCall()
                    .getResponseHeaders());

            // Set the status code in the response
            if (response.getStatus() != null) {
                response.getHttpCall().setStatusCode(
                        response.getStatus().getCode());
                response.getHttpCall().setReasonPhrase(
                        response.getStatus().getName());
            }
        } catch (Exception e) {
            getLogger().log(Level.INFO,
                    "Exception intercepted while adding the response headers",
                    e);
            response.getHttpCall().setStatusCode(
                    Status.SERVER_ERROR_INTERNAL.getCode());
            response.getHttpCall().setReasonPhrase(
                    Status.SERVER_ERROR_INTERNAL.getName());
        }
    }

    /**
     * Commits the changes to a handled uniform call back into the original HTTP
     * call. The default implementation first invokes the "addResponseHeaders"
     * then asks the "htppCall" to send the response back to the client.
     * 
     * @param response
     *            The high-level response.
     */
    public void commit(HttpResponse response) {
        try {
            if ((response.getRequest().getMethod() != null)
                    && response.getRequest().getMethod().equals(Method.HEAD)) {
                addEntityHeaders(response);
                response.setEntity(null);
            } else if (Method.GET.equals(response.getRequest().getMethod())
                    && Status.SUCCESS_OK.equals(response.getStatus())
                    && (!response.isEntityAvailable())) {
                addEntityHeaders(response);
                getLogger()
                        .warning(
                                "A response with a 200 (Ok) status should have an entity. Make sure that resource \""
                                        + response.getRequest()
                                                .getResourceRef()
                                        + "\" returns one or sets the status to 204 (No content).");
            } else if (response.getStatus().equals(Status.SUCCESS_NO_CONTENT)) {
                addEntityHeaders(response);

                if (response.isEntityAvailable()) {
                    getLogger()
                            .fine(
                                    "Responses with a 204 (No content) status generally don't have an entity. Only adding entity headers for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + "\".");
                    response.setEntity(null);
                }
            } else if (response.getStatus()
                    .equals(Status.SUCCESS_RESET_CONTENT)) {
                if (response.isEntityAvailable()) {
                    getLogger()
                            .warning(
                                    "Responses with a 205 (Reset content) status can't have an entity. Ignoring the entity for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + "\".");
                    response.setEntity(null);
                }
            } else if (response.getStatus().equals(
                    Status.REDIRECTION_NOT_MODIFIED)) {
                if (response.getEntity() != null) {
                    HeaderUtils.addNotModifiedEntityHeaders(response
                            .getEntity(), response.getHttpCall()
                            .getResponseHeaders());
                    response.setEntity(null);
                }
            } else if (response.getStatus().isInformational()) {
                if (response.isEntityAvailable()) {
                    getLogger()
                            .warning(
                                    "Responses with an informational (1xx) status can't have an entity. Ignoring the entity for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + "\".");
                    response.setEntity(null);
                }
            } else {
                addEntityHeaders(response);

                if (!response.isEntityAvailable()) {
                    if ((response.getEntity() != null)
                            && (response.getEntity().getSize() != 0)) {
                        getLogger()
                                .warning(
                                        "A response with an unavailable and potentially non empty entity was returned. Ignoring the entity for resource \""
                                                + response.getRequest()
                                                        .getResourceRef()
                                                + "\".");
                    }

                    response.setEntity(null);
                }
            }

            // Add the response headers
            addResponseHeaders(response);

            // Send the response to the client
            response.getHttpCall().sendResponse(response);
        } catch (Throwable t) {
            // [ifndef gae]
            if (response.getHttpCall().isConnectionBroken(t)) {
                getLogger()
                        .log(
                                Level.INFO,
                                "The connection was broken. It was probably closed by the client.",
                                t);
            } else
            // [enddef]
            {
                getLogger().log(Level.SEVERE,
                        "An exception occured writing the response entity", t);
                response.getHttpCall().setStatusCode(
                        Status.SERVER_ERROR_INTERNAL.getCode());
                response.getHttpCall().setReasonPhrase(
                        "An exception occured writing the response entity");
                response.setEntity(null);

                try {
                    response.getHttpCall().sendResponse(response);
                } catch (IOException ioe) {
                    getLogger().log(Level.WARNING,
                            "Unable to send error response", ioe);
                }
            }
        } finally {
            response.getHttpCall().complete();

            if (response.getOnSent() != null) {
                response.getOnSent().handle(response.getRequest(), response);
            }
        }
    }

    /**
     * Converts a low-level HTTP call into a high-level uniform request.
     * 
     * @param httpCall
     *            The low-level HTTP call.
     * @return A new high-level uniform request.
     */
    @SuppressWarnings("deprecation")
    public HttpRequest toRequest(ServerCall httpCall) {
        HttpRequest result = new HttpRequest(getContext(), httpCall);
        result.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,
                httpCall.getRequestHeaders());

        if (httpCall.getVersion() != null) {
            result.getAttributes().put(HeaderConstants.ATTRIBUTE_VERSION,
                    httpCall.getVersion());
        }

        if (httpCall.isConfidential()) {
            final List<Certificate> clientCertificates = httpCall
                    .getSslClientCertificates();
            if (clientCertificates != null) {
                result.getAttributes().put(
                        HeaderConstants.ATTRIBUTE_HTTPS_CLIENT_CERTIFICATES,
                        clientCertificates);
            }

            final String cipherSuite = httpCall.getSslCipherSuite();
            if (cipherSuite != null) {
                result.getAttributes().put(
                        HeaderConstants.ATTRIBUTE_HTTPS_CIPHER_SUITE,
                        cipherSuite);
            }

            final Integer keySize = httpCall.getSslKeySize();
            if (keySize != null) {
                result.getAttributes().put(
                        HeaderConstants.ATTRIBUTE_HTTPS_KEY_SIZE, keySize);
            }

            final String sslSessionId = httpCall.getSslSessionId();
            if (sslSessionId != null) {
                result.getAttributes().put(
                        HeaderConstants.ATTRIBUTE_HTTPS_SSL_SESSION_ID,
                        sslSessionId);
            }
        }

        return result;
    }
}
