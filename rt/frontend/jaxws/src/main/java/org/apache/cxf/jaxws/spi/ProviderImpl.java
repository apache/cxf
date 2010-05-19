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

package org.apache.cxf.jaxws.spi;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.spi.ServiceDelegate;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.EndpointUtils;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.wsdl.WSDLManager;

public class ProviderImpl extends javax.xml.ws.spi.Provider {
    public static final String JAXWS_PROVIDER = ProviderImpl.class.getName();
    private static final Logger LOG = LogUtils.getL7dLogger(ProviderImpl.class);
    private static JAXBContext jaxbContext;
    
    @Override
    public ServiceDelegate createServiceDelegate(URL url, QName qname, Class cls) {
        Bus bus = BusFactory.getThreadDefaultBus();
        return new ServiceImpl(bus, url, qname, cls);
    }
    //new in 2.2
    public ServiceDelegate createServiceDelegate(URL wsdlDocumentLocation,
                                                 QName serviceName,
                                                 Class serviceClass,
                                                 WebServiceFeature ... features) {
        Bus bus = BusFactory.getThreadDefaultBus();
        for (WebServiceFeature f : features) {
            if (!f.getClass().getName().startsWith("javax.xml.ws")) {
                throw new WebServiceException("Unknown feature error: " + f.getClass().getName());
            }
        }
        return new ServiceImpl(bus, wsdlDocumentLocation,
                               serviceName, serviceClass, features);
        
    }

    @Override
    public Endpoint createEndpoint(String bindingId, Object implementor) {

        Endpoint ep = null;
        if (EndpointUtils.isValidImplementor(implementor)) {
            Bus bus = BusFactory.getThreadDefaultBus();
            ep = new EndpointImpl(bus, implementor, bindingId);
            return ep;
        } else {
            throw new WebServiceException(new Message("INVALID_IMPLEMENTOR_EXC", LOG).toString());
        }
    }
    //new in 2.2
    public Endpoint createEndpoint(String bindingId,
                                   Object implementor,
                                   WebServiceFeature ... features) {
        EndpointImpl ep = null;
        if (EndpointUtils.isValidImplementor(implementor)) {
            Bus bus = BusFactory.getThreadDefaultBus();
            ep = new EndpointImpl(bus, implementor, bindingId, features);
            return ep;
        } else {
            throw new WebServiceException(new Message("INVALID_IMPLEMENTOR_EXC", LOG).toString());
        }
    }

    @Override
    public Endpoint createAndPublishEndpoint(String url, Object implementor) {
        Endpoint ep = createEndpoint(null, implementor);
        ep.publish(url);
        return ep;
    }
    //new in 2.2
    public Endpoint createAndPublishEndpoint(String address,
                                             Object implementor, WebServiceFeature ... features) {
        Endpoint ep = createEndpoint(null, implementor, features);
        ep.publish(address);
        return ep;
    }

    public W3CEndpointReference createW3CEndpointReference(String address, 
                                                           QName serviceName, 
                                                           QName portName,
                                                           List<Element> metadata,
                                                           String wsdlDocumentLocation,
                                                           List<Element> referenceParameters) {
        return createW3CEndpointReference(address, null, serviceName, portName,
                                          metadata, wsdlDocumentLocation, referenceParameters,
                                          null, null);
    }
    //CHECKSTYLE:OFF - spec requires a bunch of params
    public W3CEndpointReference createW3CEndpointReference(String address,
                                                           QName interfaceName, 
                                                           QName serviceName, 
                                                           QName portName,
                                                           List<Element> metadata,
                                                           String wsdlDocumentLocation,
                                                           List<Element> referenceParameters,
                                                           List<Element> elements,
                                                           Map<QName, String> attributes) {
        //CHECKSTYLE:ON
        if (serviceName != null && portName != null 
            && wsdlDocumentLocation != null && interfaceName == null) {
            Bus bus = BusFactory.getThreadDefaultBus();
            WSDLManager wsdlManager = bus.getExtension(WSDLManager.class);          
            try {
                Definition def = wsdlManager.getDefinition(wsdlDocumentLocation);
                interfaceName = def.getService(serviceName).getPort(portName.getLocalPart()).getBinding()
                    .getPortType().getQName();
            } catch (Exception e) {
                // do nothing
            }
        }
        if (serviceName == null && portName == null && address == null) {
            throw new IllegalStateException("Address in an EPR cannot be null, "
                                            + " when serviceName or portName is null");
        }
        try {
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
            writer.setPrefix(JAXWSAConstants.WSA_PREFIX, JAXWSAConstants.NS_WSA);
            writer.writeStartElement(JAXWSAConstants.WSA_PREFIX, JAXWSAConstants.WSA_ERF_NAME,
                                     JAXWSAConstants.NS_WSA);
            writer.writeNamespace(JAXWSAConstants.WSA_PREFIX, JAXWSAConstants.NS_WSA);

            writer.writeStartElement(JAXWSAConstants.WSA_PREFIX, JAXWSAConstants.WSA_ADDRESS_NAME,
                                     JAXWSAConstants.NS_WSA);
            address = address == null ? "" : address;
            writer.writeCharacters(address);
            writer.writeEndElement();

            if (referenceParameters != null) {
                writer
                    .writeStartElement(JAXWSAConstants.WSA_PREFIX,
                                       JAXWSAConstants.WSA_REFERENCEPARAMETERS_NAME, JAXWSAConstants.NS_WSA);
                for (Element ele : referenceParameters) {
                    StaxUtils.writeElement(ele, writer, true);
                }
                writer.writeEndElement();
            }

            writer.writeStartElement(JAXWSAConstants.WSA_PREFIX, JAXWSAConstants.WSA_METADATA_NAME,
                                     JAXWSAConstants.NS_WSA);
            writer.writeNamespace(JAXWSAConstants.WSAW_PREFIX, JAXWSAConstants.NS_WSAW);
            writer.writeNamespace(JAXWSAConstants.WSAM_PREFIX, JAXWSAConstants.NS_WSAM);
            if (wsdlDocumentLocation != null) {
                writer.writeNamespace(JAXWSAConstants.WSDLI_PFX,
                                      JAXWSAConstants.NS_WSDLI);
                writer.writeAttribute(JAXWSAConstants.WSDLI_PFX,
                                      JAXWSAConstants.NS_WSDLI,
                                      JAXWSAConstants.WSDLI_WSDLLOCATION,
                                      wsdlDocumentLocation);
            }
            if (interfaceName != null) {
                writer.writeStartElement(JAXWSAConstants.WSAM_PREFIX,
                                         JAXWSAConstants.WSAM_INTERFACE_NAME,
                                         JAXWSAConstants.NS_WSAM);
                String portTypePrefix = interfaceName.getPrefix();
                if (portTypePrefix == null || portTypePrefix.equals("")) {
                    portTypePrefix = "ns1";
                }
                writer.writeNamespace(portTypePrefix, interfaceName.getNamespaceURI());
                writer.writeCharacters(portTypePrefix + ":" + interfaceName.getLocalPart());
                writer.writeEndElement();
            }

            
            String serviceNamePrefix = null;

            if (serviceName != null) {
                serviceNamePrefix = (serviceName.getPrefix() == null || serviceName.getPrefix().length() == 0)
                    ? "ns2" : serviceName.getPrefix();

                writer.writeStartElement(JAXWSAConstants.WSAM_PREFIX,
                                         JAXWSAConstants.WSAM_SERVICENAME_NAME,
                                         JAXWSAConstants.NS_WSAM);

                if (portName != null) {
                    writer.writeAttribute(JAXWSAConstants.WSAM_ENDPOINT_NAME, portName.getLocalPart());
                }
                writer.writeNamespace(serviceNamePrefix, serviceName.getNamespaceURI());
                writer.writeCharacters(serviceNamePrefix + ":" + serviceName.getLocalPart());

                writer.writeEndElement();
            }

            if (wsdlDocumentLocation != null) {

                writer.writeStartElement(WSDLConstants.WSDL_PREFIX, WSDLConstants.QNAME_DEFINITIONS
                    .getLocalPart(), WSDLConstants.NS_WSDL11);
                writer.writeNamespace(WSDLConstants.WSDL_PREFIX, WSDLConstants.NS_WSDL11);
                writer.writeStartElement(WSDLConstants.WSDL_PREFIX,
                                         WSDLConstants.QNAME_IMPORT.getLocalPart(),
                                         WSDLConstants.QNAME_IMPORT.getNamespaceURI());
                if (serviceName != null) {
                    writer.writeAttribute(WSDLConstants.ATTR_NAMESPACE, serviceName.getNamespaceURI());
                }
                writer.writeAttribute(WSDLConstants.ATTR_LOCATION, wsdlDocumentLocation);
                writer.writeEndElement();
                writer.writeEndElement();
            }

            if (metadata != null) {
                for (Element e : metadata) {
                    StaxUtils.writeElement(e, writer, true);
                }
            }

            writer.writeEndElement();
            
            if (elements != null) {
                for (Element e : elements) {
                    StaxUtils.writeElement(e, writer, true);
                }
            }
            writer.writeEndElement();
            writer.flush();

            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            return (W3CEndpointReference)unmarshaller.unmarshal(writer.getDocument());
        } catch (Exception e) {
            throw new WebServiceException(new Message("ERROR_UNMARSHAL_ENDPOINTREFERENCE", LOG).toString(),
                                          e);
        }

    }

    public <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface,
                         WebServiceFeature... features) {
        ServiceDelegate sd = createServiceDelegate(null, null, serviceEndpointInterface);
        return sd.getPort(endpointReference, serviceEndpointInterface, features);
    }

    public EndpointReference readEndpointReference(Source eprInfoset) {
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            return (EndpointReference)unmarshaller.unmarshal(StaxUtils.createXMLStreamReader(eprInfoset));
        } catch (JAXBException e) {
            throw new WebServiceException(new Message("ERROR_UNMARSHAL_ENDPOINTREFERENCE", LOG).toString(),
                                          e);
        }
    }

    private JAXBContext getJAXBContext() {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(W3CEndpointReference.class);
            } catch (JAXBException e) {
                throw new WebServiceException(new Message("JAXBCONTEXT_CREATION_FAILED", LOG).toString(), e);
            }
        }
        return jaxbContext;
    }
    /*
    //new in 2.2, but introduces a new class not found in 2.1
    public Endpoint createEndpoint(String bindingId, Class<?> implementorClass,
                                   Invoker invoker, WebServiceFeature ... features) {
        //TODO - JAXWS 2.2
    }
    */
}
