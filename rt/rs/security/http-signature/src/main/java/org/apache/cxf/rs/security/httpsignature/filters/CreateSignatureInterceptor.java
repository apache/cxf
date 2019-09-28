/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.httpsignature.filters;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.httpsignature.HTTPSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.DefaultSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

/**
 * RS WriterInterceptor + ClientRequestFilter for outbound HTTP Signature. For requests with no Body
 * (e.g. GET requests), the ClientRequestFilter/ContainerResponseFilter implementation is invoked to sign
 * the request. All other requests are handled by the WriterInterceptor implementation, which digests
 * the body before signing the headers.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CreateSignatureInterceptor extends AbstractSignatureOutFilter
    implements WriterInterceptor, ClientRequestFilter, ContainerResponseFilter {
    private static final String DIGEST_HEADER_NAME = "Digest";
    private String digestAlgorithmName;

    @Context
    private UriInfo uriInfo;

    private boolean addDigest = true;

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        // Only sign the request if we have a Body.
        if (context.getEntity() != null) {
            // skip digest if already set
            if (addDigest
                && context.getHeaders().keySet().stream().noneMatch(DIGEST_HEADER_NAME::equalsIgnoreCase)) {
                addDigest(context);
            } else {
                sign(context);
                context.proceed();
            }
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        // Only sign the request if we have no Body.
        if (requestContext.getEntity() == null) {
            String method = requestContext.getMethod();
            String path = requestContext.getUri().getPath();

            performSignature(requestContext.getHeaders(), path, method);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {
        // Only sign the response if we have no Body.
        if (responseContext.getEntity() == null) {
            // We don't pass the HTTP method + URI for the response case
            performSignature(responseContext.getHeaders(), "", "");
        }
    }

    protected void sign(WriterInterceptorContext writerInterceptorContext) {
        Message m = JAXRSUtils.getCurrentMessage();
        String method = "";
        String path = "";
        // We don't pass the HTTP method + URI for the response case
        if (MessageUtils.isRequestor(m)) {
            method = HttpUtils.getProtocolHeader(JAXRSUtils.getCurrentMessage(),
                                                 Message.HTTP_REQUEST_METHOD, "");
            path = uriInfo.getRequestUri().getPath();
        }

        performSignature(writerInterceptorContext.getHeaders(), path, method);
    }

    private void addDigest(WriterInterceptorContext context) throws IOException {
        // make sure we have all content
        OutputStream originalOutputStream = context.getOutputStream();
        CachedOutputStream cachedOutputStream = new CachedOutputStream();
        context.setOutputStream(cachedOutputStream);

        context.proceed();
        cachedOutputStream.flush();

        // then digest using requested encoding
        String encoding = context.getMediaType().getParameters()
            .getOrDefault(MediaType.CHARSET_PARAMETER, StandardCharsets.UTF_8.toString());

        String digestAlgorithm = digestAlgorithmName;
        if (digestAlgorithm == null) {
            Message m = PhaseInterceptorChain.getCurrentMessage();
            digestAlgorithm =
                (String)m.getContextualProperty(HTTPSignatureConstants.RSSEC_HTTP_SIGNATURE_DIGEST_ALGORITHM);
            if (digestAlgorithm == null) {
                digestAlgorithm = DefaultSignatureConstants.DIGEST_ALGORITHM;
            }
        }

        // not so nice - would be better to have a stream
        String digest = SignatureHeaderUtils.createDigestHeader(
            new String(cachedOutputStream.getBytes(), encoding), digestAlgorithm);

        // add header
        context.getHeaders().add(DIGEST_HEADER_NAME, digest);
        sign(context);

        // write the contents
        context.setOutputStream(originalOutputStream);
        IOUtils.copy(cachedOutputStream.getInputStream(), originalOutputStream);
    }

    public String getDigestAlgorithmName() {
        return digestAlgorithmName;
    }

    public void setDigestAlgorithmName(String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
    }

    public boolean isAddDigest() {
        return addDigest;
    }

    public void setAddDigest(boolean addDigest) {
        this.addDigest = addDigest;
    }

}
