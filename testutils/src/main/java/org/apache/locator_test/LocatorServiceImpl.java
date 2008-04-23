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

/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package org.apache.locator_test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.apache.locator.EndpointNotExistFault;
import org.apache.locator.LocatorService;
import org.apache.locator.types.EndpointIdentity;
import org.apache.locator.types.ListEndpointsResponse.Endpoint;
import org.apache.locator.types.QueryEndpoints;
import org.apache.locator.types.QueryEndpointsResponse;


/**
 * 
 */

@javax.jws.WebService(name = "LocatorService",  
                      serviceName = "LocatorService", 
                      portName = "LocatorServicePort", 
                      targetNamespace = "http://apache.org/locator", 
                      endpointInterface = "org.apache.locator.LocatorService",
                      wsdlLocation = "testutils/locator.wsdl")
public class LocatorServiceImpl implements LocatorService {

    static final Logger LOG = Logger.getLogger(LocatorServiceImpl.class.getName());

    public void registerPeerManager(
                                    javax.xml.ws.wsaddressing.W3CEndpointReference peerManager,
                                    javax.xml.ws.Holder<
                                    javax.xml.ws.wsaddressing.W3CEndpointReference> 
                                            peerManagerReference,
                                    javax.xml.ws.Holder<java.lang.String> nodeId) {
        LOG.info("Executing operation registerPeerManager");
    }

    public void deregisterPeerManager(java.lang.String nodeId) {
        LOG.info("Executing operation deregisterPeerManager");
    }

    public void registerEndpoint(EndpointIdentity endpointId,
                                 javax.xml.ws.wsaddressing.W3CEndpointReference endpointReference) {
        LOG.info("Executing operation registerEndpoint");
    }

    public void deregisterEndpoint(EndpointIdentity endpointId,
                                   javax.xml.ws.wsaddressing.W3CEndpointReference endpointReference) {
        LOG.info("Executing operation deregisterEndpoint");
    }

    public javax.xml.ws.wsaddressing.W3CEndpointReference lookupEndpoint(
        javax.xml.namespace.QName serviceQname)
        throws EndpointNotExistFault {
        LOG.info("Executing operation lookupEndpoint");
        W3CEndpointReferenceBuilder eprBuilder = new  W3CEndpointReferenceBuilder();
        return eprBuilder.build();
    }

    
    public java.util.List<
        Endpoint> listEndpoints() {
        LOG.info("Executing operation listEndpoints");
        return new ArrayList<Endpoint>();
    }

    
    
    public QueryEndpointsResponse queryEndpoints(QueryEndpoints parameters) {
        LOG.info("Executing operation queryEndpoints");
        return null;
    }

}
