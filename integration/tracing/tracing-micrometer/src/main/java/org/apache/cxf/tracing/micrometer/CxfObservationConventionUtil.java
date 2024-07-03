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

package org.apache.cxf.tracing.micrometer;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.tracing.micrometer.CxfObservationDocumentation.LowCardinalityKeys;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

/**
 *
 */
final class CxfObservationConventionUtil {

    private static final Logger LOG = LogUtils.getL7dLogger(CxfObservationConventionUtil.class);

    private CxfObservationConventionUtil() {
        throw new IllegalStateException("Can't instantiate a utility class");
    }

    static KeyValues getLowCardinalityKeyValues(Message msg) {
        KeyValue rpcSystem = LowCardinalityKeys.RPC_SYSTEM.withValue("cxf");
        KeyValue rpcService = LowCardinalityKeys.RPC_SERVICE
                .withValue(msg.getExchange().getService().getName().getLocalPart());
        KeyValues keyValues = KeyValues.of(rpcSystem, rpcService);
        BindingOperationInfo bindingOperationInfo = msg.getExchange().getBindingOperationInfo();
        if (bindingOperationInfo != null) {
            keyValues = keyValues.and(LowCardinalityKeys.RPC_METHOD
                                          .withValue(bindingOperationInfo.getName().getLocalPart()));
        }
        String endpointAdress = url(msg);
        if (endpointAdress != null) {
            try {
                URI uri = URI.create(endpointAdress);
                KeyValue serverAddress = LowCardinalityKeys.SERVER_ADDRESS.withValue(uri.getHost());
                KeyValue serverPort = LowCardinalityKeys.SERVER_PORT.withValue(String.valueOf(uri.getPort()));
                return keyValues.and(serverAddress, serverPort);
            } catch (Exception ex) {
                LOG.log(Level.FINE, ex, () 
                        -> "Exception occurred while trying to parse the URI from [" + endpointAdress + "] address");
                return keyValues;
            }
        }
        return keyValues;
    }

    private static String url(Message message) {
        String address = (String) message.getExchange().get(Message.ENDPOINT_ADDRESS);
        if (address != null) {
            return address;
        }
        return (String) message.get(Message.REQUEST_URL);
    }

    static String getContextualName(Exchange exchange) {
        BindingOperationInfo bindingOperationInfo = exchange.getBindingOperationInfo();
        if (bindingOperationInfo != null) {
            String serviceName = bindingOperationInfo.getBinding().getService().getName().getLocalPart();
            String operationName = bindingOperationInfo.getOperationInfo().getName().getLocalPart();
            return serviceName + "/" + operationName;
        }
        if (exchange.getOutMessage() != null) {
            return (String) exchange.getOutMessage().get(Message.HTTP_REQUEST_METHOD);
        }
        return null;
    }
}
