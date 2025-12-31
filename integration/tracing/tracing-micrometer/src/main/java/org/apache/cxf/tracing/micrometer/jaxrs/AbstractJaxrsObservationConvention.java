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

import java.net.URI;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tracing.micrometer.jaxrs.JaxrsObservationDocumentation.CommonHighCardinalityKeys;
import org.apache.cxf.tracing.micrometer.jaxrs.JaxrsObservationDocumentation.CommonLowCardinalityKeys;
import org.jspecify.annotations.Nullable;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

/**
 *
 */
class AbstractJaxrsObservationConvention {

    KeyValues lowCardinalityKeyValues(String method, URI uri, @Nullable Integer responseStatus) {
        KeyValue requestMethod = CommonLowCardinalityKeys.HTTP_REQUEST_METHOD.withValue(method);
        KeyValue network = CommonLowCardinalityKeys.NETWORK_PROTOCOL_NAME.withValue("http");
        KeyValue serverAddress = CommonLowCardinalityKeys.SERVER_ADRESS.withValue(uri.getHost());
        KeyValue serverPort = CommonLowCardinalityKeys.SERVER_PORT.withValue(String.valueOf(uri.getPort()));
        KeyValue urlScheme = CommonLowCardinalityKeys.URL_SCHEME.withValue(String.valueOf(uri.getScheme()));
        KeyValues keyValues = KeyValues.of(requestMethod, network, serverAddress, serverPort, urlScheme);
        if (responseStatus != null) {
            keyValues = keyValues.and(CommonLowCardinalityKeys.HTTP_RESPONSE_STATUS_CODE.withValue(
                    String.valueOf(responseStatus)));
        }
        return keyValues;
    }

    KeyValues highCardinalityKeyValues(int requestLength,
                                       @Nullable Integer responseLength,
                                       @Nullable String userAgentHeader) {
        KeyValues keyValues = KeyValues.empty();
        KeyValue requestBody = requestLength != -1 ? CommonHighCardinalityKeys.REQUEST_BODY_SIZE.withValue(
                String.valueOf(requestLength)) : null;
        if (requestBody != null) {
            keyValues = keyValues.and(requestBody);
        }
        KeyValue responseBody = null;
        if (responseLength != null && responseLength != -1) {
            responseBody =  CommonHighCardinalityKeys.RESPONSE_BODY_SIZE.withValue(
                    String.valueOf(responseLength));
        }
        if (responseBody != null) {
            keyValues = keyValues.and(responseBody);
        }
        if (!StringUtils.isEmpty(userAgentHeader)) {
            keyValues = keyValues.and(CommonHighCardinalityKeys.USER_AGENT_ORIGINAL.withValue(userAgentHeader));
        }
        return keyValues;
    }
}
