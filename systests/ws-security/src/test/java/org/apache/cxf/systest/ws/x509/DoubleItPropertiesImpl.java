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
package org.apache.cxf.systest.ws.x509;

import jakarta.jws.WebService;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.feature.Features;
import org.apache.cxf.rt.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItPortType;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(features = "org.apache.cxf.feature.LoggingFeature")

@EndpointProperties({
    @EndpointProperty(key = SecurityConstants.ENCRYPT_USERNAME, value = "alice"),
    @EndpointProperty(key = SecurityConstants.ENCRYPT_PROPERTIES, value = "alice.properties"),
    @EndpointProperty(key = SecurityConstants.SIGNATURE_PROPERTIES, value = "bob.properties"),
    @EndpointProperty(key = SecurityConstants.CALLBACK_HANDLER,
                      value = "org.apache.cxf.systest.ws.common.KeystorePasswordCallback")
})
public class DoubleItPropertiesImpl implements DoubleItPortType {

    public int doubleIt(int numberToDouble) throws DoubleItFault {
        if (numberToDouble == 0) {
            throw new DoubleItFault("0 can't be doubled!");
        }
        return numberToDouble * 2;
    }

}
