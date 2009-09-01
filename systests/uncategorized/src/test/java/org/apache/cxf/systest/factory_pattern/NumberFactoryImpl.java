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
import org.apache.cxf.factory_pattern.NumberFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

@WebService(serviceName = "NumberFactoryService", 
            portName = "NumberFactoryPort", 
            endpointInterface = "org.apache.cxf.factory_pattern.NumberFactory", 
            targetNamespace = "http://cxf.apache.org/factory_pattern")
public class NumberFactoryImpl implements NumberFactory {

    public static final String FACTORY_ADDRESS = 
        "http://localhost:9006/NumberFactoryService/NumberFactoryPort";
    public static final String NUMBER_SERVANT_ADDRESS_ROOT = 
        "http://localhost:9006/NumberService/NumberPort/";
    public static final String FACTORY_NS = "http://cxf.apache.org/factory_pattern";
    public static final String NUMBER_SERVICE_NAME = "NumberService";
    public static final String NUMBER_PORT_NAME = "NumberPort";

    public static final QName NUMBER_SERVICE_QNAME = new QName(FACTORY_NS, NUMBER_SERVICE_NAME);
    public static final QName NUMBER_PORT_TYPE_QNAME = new QName(FACTORY_NS, NUMBER_PORT_NAME);

    protected EndpointReferenceType templateEpr;
    protected NumberImpl servant;

    public NumberFactoryImpl() {
    }

    public W3CEndpointReference create(String id) {

        manageNumberServantInitialisation();
        int val = Integer.valueOf(id);

        // allow clients to drive test scenarios with val
        String portName = "NumberPort";
        if (val >= 30) {
            // use jms transport
            portName = "NumberPortJMS";
        }
        EndpointReferenceType epr = EndpointReferenceUtils.getEndpointReferenceWithId(NUMBER_SERVICE_QNAME,
                                                                                      portName, id,
                                                                                      BusFactory
                                                                                          .getDefaultBus());
        Source source = EndpointReferenceUtils.convertToXML(epr);
        return new W3CEndpointReference(source);
    }

    protected synchronized EndpointReferenceType manageNumberServantInitialisation() {
        if (null == templateEpr) {
            initDefaultServant();
        }
        return templateEpr;
    }
    
    protected void initDefaultServant() {

        servant = new NumberImpl();
        String wsdlLocation = "testutils/factory_pattern.wsdl";
        String bindingId = null;

        EndpointImpl ep = new EndpointImpl(BusFactory.getDefaultBus(), 
                                           servant, bindingId, wsdlLocation);
        ep.setEndpointName(new QName(NUMBER_SERVICE_QNAME.getNamespaceURI(), "NumberPort"));
        ep.publish();
        templateEpr = ep.getServer().getDestination().getAddress();

        // jms port
        ep = new EndpointImpl(BusFactory.getDefaultBus(), servant, bindingId, wsdlLocation);
        ep.setEndpointName(new QName(NUMBER_SERVICE_QNAME.getNamespaceURI(), "NumberPortJMS"));
        ep.publish();
        ep.getServer().getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
        ep.getServer().getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
    }
}
