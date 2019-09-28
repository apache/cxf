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

import java.util.LinkedList;
import java.util.List;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.factory_pattern.NumberFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

@WebService(serviceName = "NumberFactoryService",
            portName = "NumberFactoryPort",
            endpointInterface = "org.apache.cxf.factory_pattern.NumberFactory",
            targetNamespace = "http://cxf.apache.org/factory_pattern")
public class NumberFactoryImpl implements NumberFactory {
    public static final String FACTORY_NS = "http://cxf.apache.org/factory_pattern";
    public static final String NUMBER_SERVICE_NAME = "NumberService";
    public static final String NUMBER_PORT_NAME = "NumberPort";

    public static final QName NUMBER_SERVICE_QNAME = new QName(FACTORY_NS, NUMBER_SERVICE_NAME);
    public static final QName NUMBER_PORT_TYPE_QNAME = new QName(FACTORY_NS, NUMBER_PORT_NAME);

    protected EndpointReferenceType templateEpr;
    protected NumberImpl servant;
    protected Bus bus;
    protected String port;
    protected List<AutoCloseable> endpoints = new LinkedList<>();

    public NumberFactoryImpl(Bus b, String p) {
        bus = b;
        port = p;
    }

    public void stop() throws Exception {
        for (AutoCloseable ep: endpoints) {
            ep.close();
        }
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
                                                                                      bus);
        Source source = EndpointReferenceUtils.convertToXML(epr);
        return new W3CEndpointReference(source);
    }

    protected synchronized EndpointReferenceType manageNumberServantInitialisation() {
        if (null == templateEpr) {
            initDefaultServant();
        }
        return templateEpr;
    }
    protected String getServantAddressRoot() {
        return "http://localhost:" + port + "/NumberService/NumberPort/";
    }
    protected void initDefaultServant() {

        servant = new NumberImpl();
        String wsdlLocation = "wsdl/factory_pattern.wsdl";
        String bindingId = null;

        EndpointImpl ep = new EndpointImpl(bus,
                                           servant, bindingId, wsdlLocation);
        ep.setEndpointName(new QName(NUMBER_SERVICE_QNAME.getNamespaceURI(), "NumberPort"));
        ep.publish(getServantAddressRoot());
        endpoints.add(ep);

        templateEpr = ep.getServer().getDestination().getAddress();

        // jms port
        EmbeddedJMSBrokerLauncher.updateWsdlExtensors(bus, wsdlLocation);
        ep = new EndpointImpl(bus, servant, bindingId, wsdlLocation);
        ep.setEndpointName(new QName(NUMBER_SERVICE_QNAME.getNamespaceURI(), "NumberPortJMS"));
        ep.setAddress("jms:jndi:dynamicQueues/test.cxf.factory_pattern.queue");
        ep.publish();
        ep.getServer().getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
        ep.getServer().getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
        endpoints.add(ep);
    }

}
