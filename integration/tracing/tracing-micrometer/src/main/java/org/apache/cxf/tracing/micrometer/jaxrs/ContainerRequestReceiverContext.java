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

import io.micrometer.observation.transport.RequestReplyReceiverContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

/**
 *
 */
public class ContainerRequestReceiverContext
        extends RequestReplyReceiverContext<ContainerRequestContext, ContainerResponseContext> {
    private final ContainerRequestContext requestContext;

    public ContainerRequestReceiverContext(ContainerRequestContext requestContext) {
        super(ContainerRequestContext::getHeaderString);
        this.requestContext = requestContext;
        setCarrier(requestContext);
    }

    public ContainerRequestContext getRequestContext() {
        return requestContext;
    }
}
