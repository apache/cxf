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

import jakarta.ws.rs.container.ContainerRequestContext;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.tracing.micrometer.jaxrs.JaxrsObservationDocumentation.ServerLowCardinalityKeys;

import io.micrometer.common.KeyValues;

/**
 *
 */
public class DefaultContainerRequestReceiverObservationConvention extends AbstractJaxrsObservationConvention
        implements ContainerRequestReceiverObservationConvention {

    public static final DefaultContainerRequestReceiverObservationConvention
            INSTANCE = new DefaultContainerRequestReceiverObservationConvention();

    @Override
    public KeyValues getLowCardinalityKeyValues(ContainerRequestReceiverContext context) {
        KeyValues keyValues = lowCardinalityKeyValues(context.getRequestContext().getMethod(),
                                                      context.getRequestContext().getUriInfo().getBaseUri(),
                                                      context.getResponse() != null 
                                                          ? context.getResponse().getStatus() : null);
        String pathTemplate = pathTemplate(context.getRequestContext());
        if (pathTemplate != null) {
            keyValues = keyValues.and(ServerLowCardinalityKeys.HTTP_ROUTE.withValue(pathTemplate));
        }
        return keyValues;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ContainerRequestReceiverContext context) {
        return highCardinalityKeyValues(context.getRequestContext().getLength(),
                                        context.getResponse() != null 
                                            ? context.getResponse().getLength() : null, 
                                        context.getRequestContext().getHeaderString("User-Agent"));
    }

    @Override
    public String getName() {
        return "http.server.duration";
    }

    @Override
    public String getContextualName(ContainerRequestReceiverContext context) {
        String defaultName = context.getRequestContext().getMethod();
        String pathTemplate = pathTemplate(context.getRequestContext());
        if (pathTemplate != null) {
            return defaultName + " " + pathTemplate;
        }
        return defaultName;
    }

    private String pathTemplate(ContainerRequestContext context) {
        if (context instanceof ContainerRequestContextImpl) {
            return (String) ((ContainerRequestContextImpl) context).getMessage().get(
                    URITemplate.URI_TEMPLATE);
        }
        return null;
    }
}
