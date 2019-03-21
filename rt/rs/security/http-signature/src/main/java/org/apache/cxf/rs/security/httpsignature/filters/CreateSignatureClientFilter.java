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

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

/**
 * RS CXF client Filter which signs outgoing messages. It does not create a digest header
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public final class CreateSignatureClientFilter extends AbstractSignatureOutFilter implements ClientRequestFilter {

    public CreateSignatureClientFilter() {
        super();
    }

    @Override
    public void filter(ClientRequestContext requestCtx) {
        performSignature(requestCtx.getHeaders(), requestCtx.getUri().getPath(), requestCtx.getMethod());
    }

}
