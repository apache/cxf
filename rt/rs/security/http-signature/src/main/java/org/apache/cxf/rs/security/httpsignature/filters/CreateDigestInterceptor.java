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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.common.util.MessageDigestInputStream;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

/**
 * RS WriterInterceptor which adds digests of the body.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CreateDigestInterceptor extends AbstractSignatureOutFilter implements WriterInterceptor {
    private static final String DIGEST_HEADER_NAME = "Digest";
    private final String digestAlgorithmName;

    @Context
    private UriInfo uriInfo;

    public CreateDigestInterceptor() {
        this(MessageDigestInputStream.ALGO_SHA_256);
    }

    public CreateDigestInterceptor(String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
    }

    private boolean shouldAddDigest(WriterInterceptorContext context) {
        return null != context.getEntity()
            && context.getHeaders().keySet().stream().noneMatch(DIGEST_HEADER_NAME::equalsIgnoreCase);
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        // skip digest if already set or we actually don't have a body
        if (shouldAddDigest(context)) {
            addDigest(context);
        } else {
            sign(context);
            context.proceed();
        }
    }

    private void sign(WriterInterceptorContext writerInterceptorContext) {
        String method = HttpUtils.getProtocolHeader(JAXRSUtils.getCurrentMessage(),
            Message.HTTP_REQUEST_METHOD, "");
        performSignature(writerInterceptorContext.getHeaders(), uriInfo.getRequestUri().getPath(), method);
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
        // not so nice - would be better to have a stream
        String digest = SignatureHeaderUtils.createDigestHeader(
            new String(cachedOutputStream.getBytes(), encoding), digestAlgorithmName);

        // add header
        context.getHeaders().add(DIGEST_HEADER_NAME, digest);
        sign(context);

        // write the contents
        context.setOutputStream(originalOutputStream);
        IOUtils.copy(cachedOutputStream.getInputStream(), originalOutputStream);
    }


}
