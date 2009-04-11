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
package org.apache.cxf.aegis;

import java.util.ArrayList;
import java.util.Collection;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.cxf.wsdl11.WSDLDefinitionBuilder;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.jdom.Element;
import org.jdom.output.DOMOutputter;
import org.junit.Before;

public abstract class AbstractAegisTest extends AbstractCXFTest {
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
        dfm.registerDestinationFactory(SoapBindingConstants.SOAP11_BINDING_ID, soapDF);
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
        extension.registerConduitInitiator(SoapBindingConstants.SOAP11_BINDING_ID, localTransport);
        
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
    
    public Server createService(Class serviceClass, QName name) {
        return createService(serviceClass, null, name);
    }
    
    public Server createService(Class serviceClass, Object serviceBean, QName name) {
        return createService(serviceClass, serviceBean, serviceClass.getSimpleName(), name);
    }
    
    protected Server createService(Class serviceClass, QName name, AegisDatabinding binding) {
        return createService(serviceClass, serviceClass.getSimpleName(), name, binding);
    }

    protected Server createService(Class serviceClass, 
                                   String address, QName name, 
                                    AegisDatabinding binding) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, null, address, name, binding);
        return sf.create();
    }
    
    protected Server createService(Class serviceClass, String address) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, null, address, null, null);
        return sf.create();
    }
    
    protected Server createService(Class serviceClass) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, null, 
                                                    serviceClass.getSimpleName(), null, null);
        return sf.create();
    }
    
    public Server createService(Class serviceClass,
                                Object serviceBean, 
                                String address,
                                QName name) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, serviceBean, address, name, null);
        return sf.create();
    }
    
    public Server createService(Class serviceClass,
                                Object serviceBean, 
                                String address,
                                AegisDatabinding binding) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, serviceBean, address, null, binding);
        return sf.create();
    }

    protected ServerFactoryBean createServiceFactory(Class serviceClass, 
                                                     Object serviceBean, 
                                                     String address, 
                                                     QName name,
                                                     AegisDatabinding binding) {
        ServerFactoryBean sf = new ServerFactoryBean();
        sf.setServiceClass(serviceClass);
        if (serviceBean != null) {
            sf.setServiceBean(serviceBean);
        }    
        sf.getServiceFactory().setServiceName(name);
        sf.setAddress("local://" + address);
        setupAegis(sf, binding);
        return sf;
    }
    protected void setupAegis(AbstractWSDLBasedEndpointFactory sf) { 
        setupAegis(sf, null);
    }
    @SuppressWarnings("deprecation")
    protected void setupAegis(AbstractWSDLBasedEndpointFactory sf, AegisDatabinding binding) {
        if (binding == null) {
            binding = new AegisDatabinding();
        }
        sf.getServiceFactory().getServiceConfigurations().add(0, 
            new org.apache.cxf.aegis.databinding.AegisServiceConfiguration());
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
        return null;
        
    }
    
    protected void assertXPathEquals(String xpath, String value, Element element) throws Exception {
        org.w3c.dom.Document doc = new DOMOutputter().output(element.getDocument());
        assertXPathEquals(xpath, value, doc);
    }
    
    protected NodeList assertValid(String xpath, Element element) throws Exception {
        org.w3c.dom.Document doc = new DOMOutputter().output(element.getDocument());
        
        return assertValid(xpath, doc);
    }

    protected void assertInvalid(String xpath, Element element) throws Exception {
        org.w3c.dom.Document doc = new DOMOutputter().output(element.getDocument());
        
        assertInvalid(xpath, doc);
    }
    
    protected Document getWSDLDocument(String string) throws WSDLException {
        Definition definition = getWSDLDefinition(string);
        if (definition == null) {
            return null;
        }
        WSDLWriter writer = WSDLFactory.newInstance().newWSDLWriter();
        return writer.getDocument(definition);
    }
    
    protected Context getContext() {
        AegisContext globalContext = new AegisContext();
        globalContext.initialize();
        return new Context(globalContext);
    }
    
    
    protected org.w3c.dom.Element createElement(String namespace, String name) {
        return createElement(namespace, name, null);
    }

    protected org.w3c.dom.Element createElement(String namespace, String name, String namespacePrefix) {
        Document doc = DOMUtils.createDocument();

        org.w3c.dom.Element element = doc.createElementNS(namespace, name);
        if (namespacePrefix != null) {
            element.setPrefix(namespacePrefix);
            DOMUtils.addNamespacePrefix(element, namespace, namespacePrefix);
        }

        doc.appendChild(element);
        return element;
    }

    protected ElementWriter getElementWriter(org.w3c.dom.Element element) {
        return getElementWriter(element, new MapNamespaceContext());
    }

    protected ElementWriter getElementWriter(org.w3c.dom.Element element, 
                                             NamespaceContext namespaceContext) {
        XMLStreamWriter writer = new W3CDOMStreamWriter(element);
        try {
            writer.setNamespaceContext(namespaceContext);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return new ElementWriter(writer);
    }

    protected org.w3c.dom.Element writeObjectToElement(Type type, Object bean) {
        return writeObjectToElement(type, bean, getContext());
    }

    protected org.w3c.dom.Element writeObjectToElement(Type type, Object bean, Context context) {
        org.w3c.dom.Element element = createElement("urn:Bean", "root", "b");
        ElementWriter writer = getElementWriter(element, new MapNamespaceContext());
        type.writeObject(bean, writer, getContext());
        writer.close();
        return element;
    }

    
}
