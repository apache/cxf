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

import java.io.StringWriter;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.cxf.wsdl11.WSDLDefinitionBuilder;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.junit.Before;

public abstract class AbstractAegisTest extends AbstractCXFTest {
    protected LocalTransportFactory localTransport;
    private boolean enableJDOM;

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
        //WoodstoxValidationImpl wstxVal = new WoodstoxValidationImpl();
        
        

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

    protected Server createService(Class serviceClass, String address, QName name, AegisDatabinding binding) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, null, address, name, binding);
        return sf.create();
    }

    protected Server createService(Class serviceClass, String address) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, null, address, null, null);
        return sf.create();
    }

    protected Server createService(Class serviceClass) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, null, serviceClass.getSimpleName(), null,
                                                    null);
        return sf.create();
    }

    protected Server createJaxwsService(Class serviceClass, Object serviceBean, String address, QName name) {
        if (address == null) {
            address = serviceClass.getSimpleName();
        }
        JaxWsServiceFactoryBean sf = new JaxWsServiceFactoryBean();
        sf.setDataBinding(new AegisDatabinding());
        JaxWsServerFactoryBean serverFactoryBean = new JaxWsServerFactoryBean();
        serverFactoryBean.setServiceClass(serviceClass);
        
        if (serviceBean != null) {
            serverFactoryBean.setServiceBean(serviceBean);
        }

        serverFactoryBean.setAddress("local://" + address);
            
        serverFactoryBean.setServiceFactory(sf);
        if (name != null) {
            serverFactoryBean.setEndpointName(name);
        }
        return serverFactoryBean.create();
    }

    public Server createService(Class serviceClass, Object serviceBean, String address, QName name) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, serviceBean, address, name, null);
        return sf.create();
    }

    public Server createService(Class serviceClass, Object serviceBean, String address,
                                AegisDatabinding binding) {
        ServerFactoryBean sf = createServiceFactory(serviceClass, serviceBean, address, null, binding);
        return sf.create();
    }

    protected ServerFactoryBean createServiceFactory(Class serviceClass, Object serviceBean, String address,
                                                     QName name, AegisDatabinding binding) {
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
            AegisContext context = new AegisContext();
            if (enableJDOM) {
                context.setEnableJDOMMappings(true);
            }
            binding = new AegisDatabinding();
            // perhaps the data binding needs to do this for itself?
            binding.setBus(BusFactory.getDefaultBus());
            if (enableJDOM) { // this preserves pre-2.1 behavior.
                binding.setAegisContext(context);
            }
        }
        sf.getServiceFactory().getServiceConfigurations()
            .add(0, new org.apache.cxf.aegis.databinding.AegisServiceConfiguration());
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
    
    protected XmlSchema newXmlSchema(String targetNamespace) {
        XmlSchema s = new XmlSchema();
        s.setTargetNamespace(targetNamespace);
        NamespaceMap xmlsNamespaceMap = new NamespaceMap();
        s.setNamespaceContext(xmlsNamespaceMap);

        // tns: is conventional, and besides we have unit tests that are hardcoded to it.
        xmlsNamespaceMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, targetNamespace);
        
        // ditto for xsd: instead of just namespace= for the schema schema.
        xmlsNamespaceMap.add("xsd", XmlSchemaConstants.XSD_NAMESPACE_URI);
        return s;
    }
    
    protected Element createElement(String namespace, String name) {
        return createElement(namespace, name, null);
    }

    protected Element createElement(String namespace, String name, String namespacePrefix) {
        Document doc = DOMUtils.createDocument();

        Element element = doc.createElementNS(namespace, name);
        if (namespacePrefix != null) {
            element.setPrefix(namespacePrefix);
            DOMUtils.addNamespacePrefix(element, namespace, namespacePrefix);
        }

        doc.appendChild(element);
        return element;
    }
    
    protected ElementWriter getElementWriter(Element element) {
        return getElementWriter(element, new MapNamespaceContext());
    }

    protected ElementWriter getElementWriter(Element element, NamespaceContext namespaceContext) {
        XMLStreamWriter writer = new W3CDOMStreamWriter(element);
        try {
            writer.setNamespaceContext(namespaceContext);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return new ElementWriter(writer);
    }

    protected Element writeObjectToElement(AegisType type, Object bean) {
        return writeObjectToElement(type, bean, getContext());
    }

    protected Element writeObjectToElement(AegisType type, Object bean, Context context) {
        Element element = createElement("urn:Bean", "root", "b");
        ElementWriter writer = getElementWriter(element, new MapNamespaceContext());
        type.writeObject(bean, writer, getContext());
        writer.close();
        return element;
    }

    protected boolean isEnableJDOM() {
        return enableJDOM;
    }

    protected void setEnableJDOM(boolean enableJDOM) {
        this.enableJDOM = enableJDOM;
    }
    
    protected String renderWsdl(Document wsdlDoc) throws XMLStreamException {
        StringWriter out = new StringWriter();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
        StaxUtils.writeNode(wsdlDoc, writer, true);
        writer.flush();
        return out.toString();
    }
    
}
