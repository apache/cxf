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

package org.apache.cxf.systest.factory_pattern;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

@WebService(serviceName = "NumberFactoryService", 
            portName = "NumberFactoryPort", 
            endpointInterface = "org.apache.cxf.factory_pattern.NumberFactory", 
            targetNamespace = "http://cxf.apache.org/factory_pattern")
public class HttpNumberFactoryImpl extends NumberFactoryImpl {

    public HttpNumberFactoryImpl() {
        super();
        // do servant creation up front so manual epr creation test has endpoint to talk to.
        // the manual epr will not come from create()
        //
        manageNumberServantInitialisation();
    }
    
    public W3CEndpointReference create(String id) {
        String portName = null;
        EndpointReferenceType epr = EndpointReferenceUtils.getEndpointReferenceWithId(NUMBER_SERVICE_QNAME, 
                                                                 portName, 
                                                                 id,
                                                                 BusFactory.getDefaultBus());
        
        Source source = EndpointReferenceUtils.convertToXML(epr);
        return  new W3CEndpointReference(source);
        
    }

    protected void initDefaultServant() {
        servant = new HttpNumberImpl();

        String wsdlLocation = "testutils/factory_pattern.wsdl";
        String bindingId = null;
        EndpointImpl ep = 
            new EndpointImpl(BusFactory.getDefaultBus(), servant, bindingId, wsdlLocation);
        ep.setEndpointName(new QName(NUMBER_SERVICE_QNAME.getNamespaceURI(), "NumberPort"));
        ep.publish();
        templateEpr = ep.getServer().getDestination().getAddress();        
    }
}
