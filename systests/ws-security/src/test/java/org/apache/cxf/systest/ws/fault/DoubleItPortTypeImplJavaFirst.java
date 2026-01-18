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
package org.apache.cxf.systest.ws.fault;

import java.security.Principal;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;
import org.apache.cxf.annotations.Policy.Placement;
import org.apache.cxf.feature.Features;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItPortType;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            portName = "DoubleItSoap11NoPolicyBinding",
            name = "DoubleItSoap11NoPolicyBinding",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class DoubleItPortTypeImplJavaFirst implements DoubleItPortType {
    @Resource
    WebServiceContext wsContext;

    @Policies({
        @Policy(uri = "classpath:/org/apache/cxf/systest/ws/fault/SymmetricUTPolicy.xml"),
        @Policy(uri = "classpath:/org/apache/cxf/systest/ws/fault/SignedEncryptedPolicy.xml",
                placement = Placement.BINDING_OPERATION_OUTPUT)
    })
    public int doubleIt(int numberToDouble) throws DoubleItFault {

        Principal pr = wsContext.getUserPrincipal();
        if ("alice".equals(pr.getName())) {
            return numberToDouble * 2;
        }

        org.example.schema.doubleit.DoubleItFault internalFault =
            new org.example.schema.doubleit.DoubleItFault();
        internalFault.setMajor((short)124);
        internalFault.setMinor((short)1256);
        throw new DoubleItFault("This is a fault", internalFault);
    }

}
