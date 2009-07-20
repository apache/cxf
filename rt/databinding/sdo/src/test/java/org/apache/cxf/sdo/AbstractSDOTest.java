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
package org.apache.cxf.sdo;


import java.util.ArrayList;
import java.util.Collection;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.cxf.wsdl11.WSDLDefinitionBuilder;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.junit.Before;

public abstract class AbstractSDOTest extends AbstractCXFTest {
    protected LocalTransportFactory localTransport;


    @Before
    public void setUp() throws Exception {
        super.setUpBus();
        
        SoapBindingFactory bindingFactory = new SoapBindingFactory();
        bindingFactory.setBus(bus);

        bus.getExtension(BindingFactoryManager.class)
            .registerBindingFactory("http://schemas.xmlsoap.org/wsdl/soap/", bindingFactory);

        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);

        SoapTransportFactory soapDF = new SoapTransportFactory();
        soapDF.setBus(bus);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/", soapDF);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/", soapDF);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/local", soapDF);
        
        localTransport = new LocalTransportFactory();
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/http", localTransport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/http", localTransport);
        dfm.registerDestinationFactory("http://cxf.apache.org/bindings/xformat", localTransport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/local", localTransport);

        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator(LocalTransportFactory.TRANSPORT_ID, localTransport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/", localTransport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http", localTransport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/", localTransport);
        
        bus.setExtension(new WSDLManagerImpl(), WSDLManager.class);
        

        addNamespace("wsdl", SOAPConstants.WSDL11_NS);
        addNamespace("wsdlsoap", SOAPConstants.WSDL11_SOAP_NS);
        addNamespace("xsd", SOAPConstants.XSD);
    }

    @Override
    protected Bus createBus() throws BusException {
        ExtensionManagerBus bus = new ExtensionManagerBus();
        BusFactory.setDefaultBus(bus);
        return bus;
    }
    
    protected Node invoke(String service, String message) throws Exception {
        return invoke("local://" + service, LocalTransportFactory.TRANSPORT_ID, message);
    }
    protected Node invoke(String service, byte[] message) throws Exception {
        return invoke("local://" + service, LocalTransportFactory.TRANSPORT_ID, message);
    }
    
    public Server createService(Class serviceClass, QName name) {
        return createService(serviceClass, null, name);
    }
    
    public Server createService(Class serviceClass, Object serviceBean, QName name) {
        return createService(serviceClass, serviceBean, serviceClass.getSimpleName(), name);
    }
    
    protected Server createService(Class serviceClass, QName name, SDODataBinding binding) {
        return createService(serviceClass, serviceClass.getSimpleName(), name, binding);
    }

    protected Server createService(Class serviceClass, 
                                   String address, QName name, 
                                   SDODataBinding binding) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, null, address, name, binding);
        return sf.create();
    }
    
    public Server createService(Class serviceClass,
                                Object serviceBean, 
                                String address,
                                QName name) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, serviceBean, address, name, null);
        return sf.create();
    }

    protected ServerFactoryBean createServiceFactory(Class serviceClass, 
                                                     Object serviceBean, 
                                                     String address, 
                                                     QName name,
                                                     SDODataBinding binding) {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(serviceClass);
        if (serviceBean != null) {
            sf.setServiceBean(serviceBean);
        }    
        sf.getServiceFactory().setServiceName(name);
        sf.setAddress("local://" + address);
        sf.getServiceFactory().setQualifyWrapperSchema(true);
        setupSDO(sf, binding);
        return sf;
    }
    protected void setupSDO(AbstractWSDLBasedEndpointFactory sf) { 
        setupSDO(sf, null);
    }
    protected void setupSDO(AbstractWSDLBasedEndpointFactory sf, SDODataBinding binding) {
        if (binding == null) {
            binding = new SDODataBinding();
        }
        sf.getServiceFactory().setDataBinding(binding);
    }

    protected Collection<Document> getWSDLDocuments(String string) throws WSDLException {
        WSDLWriter writer = WSDLFactory.newInstance().newWSDLWriter();        

        Collection<Document> docs = new ArrayList<Document>();
        Definition definition = getWSDLDefinition(string);
        if (definition == null) {
            return null;
        }
        docs.add(writer.getDocument(definition));
        
        for (Import wsdlImport : WSDLDefinitionBuilder.getImports(definition)) {
            docs.add(writer.getDocument(wsdlImport.getDefinition()));
        }
        return docs;
    }

    protected Definition getWSDLDefinition(String string) throws WSDLException {
        ServerRegistry svrMan = getBus().getExtension(ServerRegistry.class);
        for (Server s : svrMan.getServers()) {
            Service svc = s.getEndpoint().getService();
            if (svc.getName().getLocalPart().equals(string)) {
                ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus, svc.getServiceInfos());
                return builder.build();
            }
        }
        String localString = "local://" + string;
        for (Server s : svrMan.getServers()) {
            String s2 = s.getDestination().getAddress().getAddress().getValue();
            if (localString.equals(s2)) {
                Service svc = s.getEndpoint().getService();
                ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus, svc.getServiceInfos());
                return builder.build();
            }
        }
        return null;
        
    }
    
    protected Document getWSDLDocument(String string) throws WSDLException {
        Definition definition = getWSDLDefinition(string);
        if (definition == null) {
            return null;
        }
        WSDLWriter writer = WSDLFactory.newInstance().newWSDLWriter();
        return writer.getDocument(definition);
    }
}
