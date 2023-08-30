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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tracing.micrometer.jaxrs.JaxrsObservationDocumentation.ClientHighCardinalityKeys;

import io.micrometer.common.KeyValues;

/**
 *
 */
public class DefaultContainerRequestSenderObservationConvention extends AbstractJaxrsObservationConvention
        implements ContainerRequestSenderObservationConvention {

    public static final DefaultContainerRequestSenderObservationConvention
            INSTANCE = new DefaultContainerRequestSenderObservationConvention();

    @Override
    public KeyValues getLowCardinalityKeyValues(ContainerRequestSenderObservationContext context) {
        return lowCardinalityKeyValues(context.getRequestContext().getMethod(),
                                       context.getRequestContext().getUri(),
                                       context.getResponse() != null 
                                           ? context.getResponse().getStatus() : null);
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ContainerRequestSenderObservationContext context) {
        String contentLength = context.getRequestContext().getHeaderString("Content-Length");
        int requestLength = StringUtils.isEmpty(contentLength) ? -1 : Integer.parseInt(contentLength);
        Integer responseLength = context.getResponse() != null
                                 ? context.getResponse().getLength() : null;
        KeyValues keyValues = highCardinalityKeyValues(requestLength, responseLength,
                                                       context.getRequestContext()
                                                              .getHeaderString("User-Agent"));
        return keyValues.and(
                ClientHighCardinalityKeys.URL_FULL.withValue(context.getRequestContext().getUri().toString()));
    }

    @Override
    public String getName() {
        return "http.client.duration";
    }

    @Override
    public String getContextualName(ContainerRequestSenderObservationContext context) {
        return context.getRequestContext().getMethod();
    }
}
