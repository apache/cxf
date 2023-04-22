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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.spi.Invoker;
import jakarta.xml.ws.spi.ServiceDelegate;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.EndpointUtils;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.wsdl.WSDLManager;

public class ProviderImpl extends jakarta.xml.ws.spi.Provider {
    public static final String JAXWS_PROVIDER = ProviderImpl.class.getName();
    protected static final Logger LOG = LogUtils.getL7dLogger(ProviderImpl.class);
    private static JAXBContext jaxbContext;

    @Override
    public ServiceDelegate createServiceDelegate(URL url, QName qname,
                                                 @SuppressWarnings("rawtypes") Class cls) {
        return new ServiceImpl(null, url, qname, cls);
    }
    //new in 2.2
    public ServiceDelegate createServiceDelegate(URL wsdlDocumentLocation,
                                                 QName serviceName,
                                                 @SuppressWarnings("rawtypes") Class serviceClass,
                                                 WebServiceFeature ... features) {
        for (WebServiceFeature f : features) {
            if (!f.getClass().getName().startsWith("jakarta.xml.ws")
                && !(f instanceof Feature)) {
                throw new WebServiceException("Unknown feature error: " + f.getClass().getName());
            }
        }
        return new ServiceImpl(null, wsdlDocumentLocation,
                               serviceName, serviceClass, features);

    }

    protected EndpointImpl createEndpointImpl(Bus bus,
                                              String bindingId,
                                              Object implementor,
                                              WebServiceFeature ... features) {
        return new EndpointImpl(bus, implementor, bindingId, features);
    }

    @Override
    public Endpoint createEndpoint(String bindingId, Object implementor) {

        if (EndpointUtils.isValidImplementor(implementor)) {
            Bus bus = BusFactory.getThreadDefaultBus();
            return createEndpointImpl(bus, bindingId, implementor);
        }
        throw new WebServiceException(new Message("INVALID_IMPLEMENTOR_EXC", LOG).toString());
    }
    //new in 2.2
    public Endpoint createEndpoint(String bindingId,
                                   Object implementor,
                                   WebServiceFeature ... features) {
        if (EndpointUtils.isValidImplementor(implementor)) {
            Bus bus = BusFactory.getThreadDefaultBus();
            return createEndpointImpl(bus, bindingId, implementor, features);
        }
        throw new WebServiceException(new Message("INVALID_IMPLEMENTOR_EXC", LOG).toString());
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

    //new in 2.2
    public Endpoint createEndpoint(String bindingId, Class<?> implementorClass,
                                   Invoker invoker, WebServiceFeature ... features) {
        if (EndpointUtils.isValidImplementor(implementorClass)) {
            Bus bus = BusFactory.getThreadDefaultBus();
            JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
            if (features != null) {
                factory.getJaxWsServiceFactory().setWsFeatures(Arrays.asList(features));
            }
            if (invoker != null) {
                factory.setInvoker(new JAXWSMethodInvoker(invoker));
                try {
                    invoker.inject(new WebServiceContextImpl());
                } catch (Exception e) {
                    throw new WebServiceException(new Message("ENDPOINT_CREATION_FAILED_MSG",
                                                              LOG).toString(), e);
                }
            }
            EndpointImpl ep = new EndpointImpl(bus, null, factory);
            ep.setImplementorClass(implementorClass);
            return ep;
        }
        throw new WebServiceException(new Message("INVALID_IMPLEMENTOR_EXC", LOG).toString());
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

    /**
     * Convert from EndpointReference to CXF internal 2005/08 EndpointReferenceType
     *
     * @param external the jakarta.xml.ws.EndpointReference
     * @return CXF internal 2005/08 EndpointReferenceType
     */
    public static EndpointReferenceType convertToInternal(EndpointReference external) {
        if (external instanceof W3CEndpointReference) {

            Unmarshaller um = null;
            try {
                DocumentFragment frag = DOMUtils.getEmptyDocument().createDocumentFragment();
                DOMResult result = new DOMResult(frag);
                external.writeTo(result);
                W3CDOMStreamReader reader = new W3CDOMStreamReader(frag);

                // CXF internal 2005/08 EndpointReferenceType should be
                // compatible with W3CEndpointReference
                //jaxContext = ContextUtils.getJAXBContext();
                JAXBContext context = JAXBContext
                    .newInstance(new Class[] {org.apache.cxf.ws.addressing.ObjectFactory.class});
                um = context.createUnmarshaller();
                return um.unmarshal(reader, EndpointReferenceType.class).getValue();
            } catch (JAXBException e) {
                throw new IllegalArgumentException("Could not unmarshal EndpointReference", e);
            } finally {
                JAXBUtils.closeUnmarshaller(um);
            }
        }
        return null;
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
            final W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
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

            if (wsdlDocumentLocation != null
                || interfaceName != null
                || serviceName != null
                || (metadata != null && !metadata.isEmpty())) {


                writer.writeStartElement(JAXWSAConstants.WSA_PREFIX, JAXWSAConstants.WSA_METADATA_NAME,
                                         JAXWSAConstants.NS_WSA);
                writer.writeNamespace(JAXWSAConstants.WSAW_PREFIX, JAXWSAConstants.NS_WSAW);
                writer.writeNamespace(JAXWSAConstants.WSAM_PREFIX, JAXWSAConstants.NS_WSAM);

                if (wsdlDocumentLocation != null) {
                    boolean includeLocationOnly = false;
                    org.apache.cxf.message.Message message = PhaseInterceptorChain.getCurrentMessage();
                    if (message != null) {
                        includeLocationOnly = MessageUtils
                            .getContextualBoolean(message, "org.apache.cxf.wsa.metadata.wsdlLocationOnly", false);
                    }
                    String attrubuteValue = serviceName != null && !includeLocationOnly
                            ? serviceName.getNamespaceURI() + " " + wsdlDocumentLocation
                            : wsdlDocumentLocation;
                    writer.writeNamespace(JAXWSAConstants.WSDLI_PFX,
                                          JAXWSAConstants.NS_WSDLI);
                    writer.writeAttribute(JAXWSAConstants.WSDLI_PFX,
                                          JAXWSAConstants.NS_WSDLI,
                                          JAXWSAConstants.WSDLI_WSDLLOCATION,
                                          attrubuteValue);
                }
                if (interfaceName != null) {
                    writer.writeStartElement(JAXWSAConstants.WSAM_PREFIX,
                                             JAXWSAConstants.WSAM_INTERFACE_NAME,
                                             JAXWSAConstants.NS_WSAM);
                    String portTypePrefix = interfaceName.getPrefix();
                    if (portTypePrefix == null || portTypePrefix.isEmpty()) {
                        portTypePrefix = "ns1";
                    }
                    writer.writeNamespace(portTypePrefix, interfaceName.getNamespaceURI());
                    writer.writeCharacters(portTypePrefix + ":" + interfaceName.getLocalPart());
                    writer.writeEndElement();
                }

                if (serviceName != null) {
                    String serviceNamePrefix =
                        (serviceName.getPrefix() == null || serviceName.getPrefix().length() == 0)
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
            }

            if (elements != null) {
                for (Element e : elements) {
                    StaxUtils.writeElement(e, writer, true);
                }
            }
            writer.writeEndElement();
            writer.flush();

            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<W3CEndpointReference>() {
                    public W3CEndpointReference run() throws Exception {
                        Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
                        try {
                            return (W3CEndpointReference)unmarshaller.unmarshal(writer.getDocument());
                        } finally {
                            JAXBUtils.closeUnmarshaller(unmarshaller);
                        }
                    }
                });
            } catch (PrivilegedActionException pae) {
                Exception e = pae.getException();
                if (e instanceof JAXBException) {
                    throw (JAXBException)e;
                }
                throw new SecurityException(e);
            }
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
            final XMLStreamReader reader = StaxUtils.createXMLStreamReader(eprInfoset);
            return AccessController.doPrivileged(new PrivilegedExceptionAction<EndpointReference>() {
                public EndpointReference run() throws Exception {
                    Unmarshaller unmarshaller = null;
                    try {
                        unmarshaller = getJAXBContext().createUnmarshaller();
                        return (EndpointReference)unmarshaller.unmarshal(reader);
                    } finally {
                        try {
                            StaxUtils.close(reader);
                        } catch (XMLStreamException e) {
                            // Ignore
                        }
                        JAXBUtils.closeUnmarshaller(unmarshaller);
                    }
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if (e instanceof JAXBException) {
                throw new WebServiceException(new Message("ERROR_UNMARSHAL_ENDPOINTREFERENCE", LOG)
                                                  .toString(),
                                              e);
            }
            throw new SecurityException(e);
        }
    }

    private static JAXBContext getJAXBContext() {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(W3CEndpointReference.class);
            } catch (JAXBException e) {
                throw new WebServiceException(new Message("JAXBCONTEXT_CREATION_FAILED", LOG).toString(), e);
            }
        }
        return jaxbContext;
    }
}
