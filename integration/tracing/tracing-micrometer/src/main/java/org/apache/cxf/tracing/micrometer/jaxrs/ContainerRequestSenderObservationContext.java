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

package org.apache.cxf.tracing.micrometer.jaxrs;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;

import io.micrometer.observation.transport.RequestReplySenderContext;

/**
 *
 */
public class ContainerRequestSenderObservationContext
        extends RequestReplySenderContext<ClientRequestContext, ClientResponseContext> {
    private final ClientRequestContext requestContext;

    public ContainerRequestSenderObservationContext(ClientRequestContext requestContext) {
        super((msg, key, value) -> msg.getHeaders().putSingle(key, value));
        this.requestContext = requestContext;
        setCarrier(requestContext);
    }

    public ClientRequestContext getRequestContext() {
        return requestContext;
    }
}
