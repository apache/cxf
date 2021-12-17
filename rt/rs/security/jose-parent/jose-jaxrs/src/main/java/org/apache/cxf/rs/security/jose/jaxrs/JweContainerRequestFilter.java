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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;

@PreMatching
@Priority(Priorities.JWE_SERVER_READ_PRIORITY)
public class JweContainerRequestFilter extends AbstractJweDecryptingFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        if (isMethodWithNoContent(context.getMethod())
            || isCheckEmptyStream() && !context.hasEntity()) {
            return;
        }
        final byte[] encryptedContent = IOUtils.readBytesFromStream(context.getEntityStream());
        if (encryptedContent.length == 0) {
            return;
        }
        JweDecryptionOutput out = decrypt(encryptedContent);
        byte[] bytes = out.getContent();
        context.setEntityStream(new ByteArrayInputStream(bytes));
        context.getHeaders().putSingle("Content-Length", Integer.toString(bytes.length));
        String ct = JoseUtils.checkContentType(out.getHeaders().getContentType(), getDefaultMediaType());
        if (ct != null) {
            context.getHeaders().putSingle("Content-Type", ct);
        }
        if (super.isValidateHttpHeaders()) {
            super.validateHttpHeadersIfNeeded(context.getHeaders(), out.getHeaders());
        }
    }
    
    protected boolean isMethodWithNoContent(String method) {
        return HttpMethod.DELETE.equals(method) || HttpUtils.isMethodWithNoRequestContent(method);
    }
}
