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

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;

@Priority(Priorities.JWS_CLIENT_READ_PRIORITY)
public class JwsClientResponseFilter extends AbstractJwsReaderProvider implements ClientResponseFilter {
    @Override
    public void filter(ClientRequestContext req, ClientResponseContext res) throws IOException {
        if (isMethodWithNoContent(req.getMethod())
            || isCheckEmptyStream() && !res.hasEntity()) {
            return;
        }
        JwsCompactConsumer p = new JwsCompactConsumer(IOUtils.readStringFromStream(res.getEntityStream()));
        JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier(p.getJwsHeaders());
        if (!p.verifySignatureWith(theSigVerifier)) {
            throw new JwsException(JwsException.Error.INVALID_SIGNATURE);
        }
        
        byte[] bytes = p.getDecodedJwsPayloadBytes();
        res.setEntityStream(new ByteArrayInputStream(bytes));
        res.getHeaders().putSingle("Content-Length", Integer.toString(bytes.length));
        String ct = JoseUtils.checkContentType(p.getJwsHeaders().getContentType(), getDefaultMediaType());
        if (ct != null) {
            res.getHeaders().putSingle("Content-Type", ct);
        }
        
        if (super.isValidateHttpHeaders()) {
            super.validateHttpHeadersIfNeeded(res.getHeaders(), p.getJwsHeaders());
        }
    }

    protected boolean isMethodWithNoContent(String method) {
        return HttpMethod.DELETE.equals(method) || HttpUtils.isMethodWithNoResponseContent(method);
    }
}
