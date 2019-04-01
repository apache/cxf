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
import java.nio.charset.StandardCharsets;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

/**
 * RS WriterInterceptor which adds digests of the body.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class ClientDigestInterceptor implements WriterInterceptor {
    private static final String DIGEST_HEADER_NAME = "Digest";
    private final String digestAlgorithmName;

    public ClientDigestInterceptor(String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        // don't add digest header if already set or we cannot use stream-cache
        if (context.getHeaders().keySet().stream().noneMatch(DIGEST_HEADER_NAME::equalsIgnoreCase)
            && context.getOutputStream() instanceof CacheAndWriteOutputStream) {
            CacheAndWriteOutputStream cacheAndWriteOutputStream = (CacheAndWriteOutputStream) context.getOutputStream();
            // not so nice - would be better to have a stream
            String digest = SignatureHeaderUtils.createDigestHeader(
                new String(cacheAndWriteOutputStream.getBytes(), StandardCharsets.UTF_8), digestAlgorithmName);
            context.getHeaders().add(DIGEST_HEADER_NAME, digest);
        }
    }
}
