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

package org.apache.cxf.observation;

import java.net.URI;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.observation.CxfObservationDocumentation.LowCardinalityKeys;
import org.apache.cxf.service.model.BindingOperationInfo;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

/**
 *
 */
final class CxfObservationConventionUtil {

    private CxfObservationConventionUtil() {
        throw new IllegalStateException("Can't instantiate a utility class");
    }

    static KeyValues getLowCardinalityKeyValues(Message msg) {
        KeyValue rpcSystem = LowCardinalityKeys.RPC_SYSTEM.withValue("cxf");
        KeyValue rpcService = LowCardinalityKeys.RPC_SERVICE.withValue(msg.getExchange().getService().getName().getLocalPart());
        KeyValue rpcMethod = LowCardinalityKeys.RPC_METHOD.withValue(msg.getExchange().getBindingOperationInfo().getName().getLocalPart());
        String endpointAdress = url(msg);
        if (endpointAdress != null) {
            try {
                URI uri = URI.create(endpointAdress);
                KeyValue serverAddress = LowCardinalityKeys.SERVER_ADDRESS.withValue(uri.getHost());
                KeyValue serverPort = LowCardinalityKeys.SERVER_PORT.withValue(String.valueOf(uri.getPort()));
                return KeyValues.of(rpcSystem, rpcService, rpcMethod, serverAddress, serverPort);
            } catch (Exception ex) {
                // TODO: Log this out
                return KeyValues.of(rpcSystem, rpcService, rpcMethod);
            }
        }
        return KeyValues.of(rpcSystem, rpcService, rpcMethod);
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
        if (bindingOperationInfo == null) {
            return null;
        }
        String serviceName = bindingOperationInfo.getBinding().getService().getName().getLocalPart();
        String operationName = bindingOperationInfo.getOperationInfo().getName().getLocalPart();
        return serviceName + "/" + operationName;
    }
}
