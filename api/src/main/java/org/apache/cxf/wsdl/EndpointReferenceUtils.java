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

package org.apache.cxf.wsdl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.EndpointResolverRegistry;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MultiplexDestination;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MetadataType;
import org.apache.cxf.ws.addressing.wsdl.AttributedQNameType;
import org.apache.cxf.ws.addressing.wsdl.ServiceNameType;
import org.apache.ws.commons.schema.XmlSchema;

/**
 * Provides utility methods for obtaining endpoint references, wsdl definitions, etc.
 */
public final class EndpointReferenceUtils {

    /**
     * We want to load the schemas, including references to external schemas, into a SchemaFactory
     * to validate. There seem to be bugs in resolving inter-schema references in Xerces, so even when we are 
     * handing the factory all the schemas, interrelated with &lt;import&gt; elements, we need
     * to also hand over extra copies (!) as character images when requested.
     * 
     * To do this, we use the DOM representation kept in the SchemaInfo. This has the bonus
     * of benefiting from the use of the catalog resolver in there, which is missing from
     * the code in here.
     */
    private static final class SchemaLSResourceResolver implements LSResourceResolver {
        private final Map<String, String> schemas;
        private final Set<String> done = new HashSet<String>();
        private final ExtendedURIResolver resolver = new ExtendedURIResolver();
        
        private SchemaLSResourceResolver(Map<String, String> schemas) {
            this.schemas = schemas;
        }
        
        public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                       String systemId, String baseURI) {

            String newId = systemId;
            if (baseURI != null && systemId != null) {  //add additional systemId null check
                try {
                    URI uri = new URI(baseURI);
                    uri = uri.resolve(systemId);
                    newId = uri.toString();
                    if (newId.equals(systemId)) {
                        URL url = new URL(baseURI);
                        url = new URL(url, systemId);
                        newId = url.toExternalForm();
                    }
                } catch (URISyntaxException e) {
                    //ignore
                } catch (MalformedURLException e) {
                    //ignore
                }
            }
            if (done.contains(newId + ":" + namespaceURI)) {
                return null;
            }
            if (schemas.containsKey(newId + ":" + namespaceURI)) {
                String ds = schemas.remove(newId + ":" + namespaceURI);
                LSInputImpl impl = new LSInputImpl();
                impl.setSystemId(newId);
                impl.setBaseURI(newId);
                impl.setCharacterStream(new StringReader(ds));
                done.add(newId + ":" + namespaceURI);
                return impl;
            }
            if (schemas.containsKey(newId + ":null")) {
                String ds = schemas.get(newId + ":null");
                LSInputImpl impl = new LSInputImpl();
                impl.setSystemId(newId);
                impl.setBaseURI(newId);
                impl.setCharacterStream(new StringReader(ds));
                done.add(newId + ":" + namespaceURI);
                return impl;                
            }
                
            //REVIST - we need to get catalogs in here somehow  :-(
            if (systemId == null) {
                systemId = publicId;
            }
            if (systemId != null) {
                InputSource source = resolver.resolve(systemId, baseURI);
                if (source != null) {
                    LSInputImpl impl = new LSInputImpl();
                    impl.setByteStream(source.getByteStream());
                    impl.setSystemId(source.getSystemId());
                    impl.setPublicId(source.getPublicId());
                    return impl;
                }
            }
            LOG.warning("Could not resolve Schema for " + systemId);
            return null;
        }
    }

    public static final String ANONYMOUS_ADDRESS = "http://www.w3.org/2005/08/addressing/anonymous";

    private static final Logger LOG = LogUtils.getL7dLogger(EndpointReferenceUtils.class);

    private static final String NS_WSAW_2005 = "http://www.w3.org/2005/02/addressing/wsdl";
    private static final String WSDL_INSTANCE_NAMESPACE = 
        "http://www.w3.org/2006/01/wsdl-instance";
    
    private static final QName WSA_WSDL_NAMESPACE_NS =
        new QName("xmlns:" + JAXWSAConstants.WSAW_PREFIX);
    private static final String XML_SCHEMA_NAMESPACE =
        "http://www.w3.org/2001/XMLSchema";
    private static final String XML_SCHEMA_NAMESPACE_PREFIX = "xs";
    private static final QName XML_SCHEMA_NAMESPACE_NS =
        new QName("xmlns:" + XML_SCHEMA_NAMESPACE_PREFIX);
    private static final String XML_SCHEMA_INSTANCE_NAMESPACE =
        "http://www.w3.org/2001/XMLSchema-instance";
    private static final QName WSDL_LOCATION =
        new QName(WSDL_INSTANCE_NAMESPACE, "wsdlLocation");
    private static final QName XSI_TYPE = 
        new QName(XML_SCHEMA_INSTANCE_NAMESPACE, "type", "xsi");
    
    private static final org.apache.cxf.ws.addressing.wsdl.ObjectFactory WSA_WSDL_OBJECT_FACTORY = 
        new org.apache.cxf.ws.addressing.wsdl.ObjectFactory();
    private static final org.apache.cxf.ws.addressing.ObjectFactory WSA_OBJECT_FACTORY = 
        new org.apache.cxf.ws.addressing.ObjectFactory();

    
    private EndpointReferenceUtils() {
        // Utility class - never constructed
    }
    
    private static Transformer getTransformer() throws EndpointUtilsException {
        //To Support IBM JDK 
        //If use the default transformFactory ,org.apache.xalan.processor.TransformerFactoryImpl \
        //when transform stuff will lost attributes 
        if (System.getProperty("java.vendor").indexOf("IBM") > -1) {
            System.setProperty("javax.xml.transform.TransformerFactory", 
                               "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        }
        
        try {
            return TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException tce) {
            throw new EndpointUtilsException(new Message("COULD_NOT_CREATE_TRANSFORMER", LOG),
                                                         tce);
        }
        
    }
    
    
    /**
     * Sets the service and port name of the provided endpoint reference. 
     * @param ref the endpoint reference.
     * @param serviceName the name of service.
     * @param portName the port name.
     */
    public static void setServiceAndPortName(EndpointReferenceType ref, 
                                             QName serviceName, 
                                             String portName) {
        if (null != serviceName) {
            JAXBElement<ServiceNameType> jaxbElement = getServiceNameType(serviceName, portName);
            MetadataType mt = ref.getMetadata();
            if (null == mt) {
                mt = new MetadataType();
                ref.setMetadata(mt);
            }

            mt.getAny().add(jaxbElement);
        }
    }
    
    public static JAXBElement<ServiceNameType> getServiceNameType(QName serviceName, String portName) {
        ServiceNameType serviceNameType = WSA_WSDL_OBJECT_FACTORY.createServiceNameType();
        serviceNameType.setValue(serviceName);
        serviceNameType.setEndpointName(portName);
        serviceNameType.getOtherAttributes().put(WSA_WSDL_NAMESPACE_NS, JAXWSAConstants.NS_WSAW);
        serviceNameType.getOtherAttributes().put(XSI_TYPE, 
                                                 JAXWSAConstants.WSAW_PREFIX + ":" 
                                                 + serviceNameType.getClass().getSimpleName());
        return WSA_WSDL_OBJECT_FACTORY.createServiceName(serviceNameType);
    }
    
    /**
     * Gets the service name of the provided endpoint reference. 
     * @param ref the endpoint reference.
     * @return the service name.
     */
    public static QName getServiceName(EndpointReferenceType ref, Bus bus) {
        MetadataType metadata = ref.getMetadata();
        if (metadata == null) {
            return null;
        }
        for (Object obj : metadata.getAny()) {
            if (obj instanceof Element) {
                Node node = (Element)obj;
                if ((node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAW)
                    || node.getNamespaceURI().equals(NS_WSAW_2005))
                    && node.getLocalName().equals("ServiceName")) {
                    String content = node.getTextContent();
                    String namespaceURI = node.getFirstChild().getNamespaceURI();
                    String service = content;
                    if (content.contains(":")) {
                        namespaceURI = getNameSpaceUri(node, content, namespaceURI);
                        if (StringUtils.isEmpty(namespaceURI)) {
                            namespaceURI = findNamespaceHack(ref, bus);                                
                        }
                        service = getService(content);
                    } else {
                        Node nodeAttr = node.getAttributes().getNamedItem("xmlns");
                        namespaceURI = nodeAttr.getNodeValue();
                    }
                    
                    return new QName(namespaceURI, service);
                }
            } else if (obj instanceof JAXBElement) {
                Object val = ((JAXBElement)obj).getValue();
                if (val instanceof ServiceNameType) {
                    return ((ServiceNameType)val).getValue();
                }
            } else if (obj instanceof ServiceNameType) {
                return ((ServiceNameType)obj).getValue();
            }
        }
        return null;
    }
    
    /**
     * Gets the port name of the provided endpoint reference.
     * @param ref the endpoint reference.
     * @return the port name.
     */
    public static String getPortName(EndpointReferenceType ref) {
        MetadataType metadata = ref.getMetadata();
        if (metadata != null) {
            for (Object obj : metadata.getAny()) {
                if (obj instanceof Element) {
                    Node node = (Element)obj;
                    if ((node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAW)
                        || node.getNamespaceURI().equals(NS_WSAW_2005))
                        && node.getNodeName().contains("ServiceName")) {
                        Node item = node.getAttributes().getNamedItem("EndpointName");
                        return item != null ? item.getTextContent() : null;
                    }
                } else if (obj instanceof JAXBElement) {
                    Object val = ((JAXBElement)obj).getValue();
                    if (val instanceof ServiceNameType) {
                        return ((ServiceNameType)val).getEndpointName();
                    }
                } else if (obj instanceof ServiceNameType) {
                    return ((ServiceNameType)obj).getEndpointName();
                }
            }
        }
        return null;
    }
    
    public static QName getPortQName(EndpointReferenceType ref, Bus bus) {
        QName serviceName = getServiceName(ref, bus); 
        return new QName(serviceName.getNamespaceURI(), getPortName(ref));
    }
    
    public static void setPortName(EndpointReferenceType ref, String portName) {
        MetadataType metadata = ref.getMetadata();
        if (metadata != null) {
            for (Object obj : metadata.getAny()) {
                if (obj instanceof Element) {
                    Element node = (Element)obj;
                    if (node.getNodeName().contains("ServiceName")
                        && (node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAW)
                        || node.getNamespaceURI().equals(NS_WSAW_2005))) {
                        node.setAttribute(JAXWSAConstants.WSAW_ENDPOINT_NAME, portName);
                    }
                } else if (obj instanceof JAXBElement) {
                    Object val = ((JAXBElement)obj).getValue();
                    if (val instanceof ServiceNameType) {
                        ((ServiceNameType)val).setEndpointName(portName);
                    }
                } else if (obj instanceof ServiceNameType) {
                    ((ServiceNameType)obj).setEndpointName(portName);
                }
            }
        }
    }
    
    public static void setInterfaceName(EndpointReferenceType ref, QName portTypeName) {
        if (null != portTypeName) {
            AttributedQNameType interfaceNameType =
                WSA_WSDL_OBJECT_FACTORY.createAttributedQNameType();
            
            interfaceNameType.setValue(portTypeName);
            interfaceNameType.getOtherAttributes().put(XML_SCHEMA_NAMESPACE_NS, 
                                                       XML_SCHEMA_NAMESPACE);
            interfaceNameType.getOtherAttributes().put(XSI_TYPE,
                                                       XML_SCHEMA_NAMESPACE_PREFIX + ":"
                                                       + portTypeName.getClass().getSimpleName());
            
            JAXBElement<AttributedQNameType> jaxbElement = 
                WSA_WSDL_OBJECT_FACTORY.createInterfaceName(interfaceNameType);

            MetadataType mt = ref.getMetadata();
            if (null == mt) {
                mt = WSA_OBJECT_FACTORY.createMetadataType();
                ref.setMetadata(mt);
            }
            mt.getAny().add(jaxbElement);
        }
    }
  
    public static QName getInterfaceName(EndpointReferenceType ref, Bus bus) {
        MetadataType metadata = ref.getMetadata();
        if (metadata == null) {
            return null;
        }
        for (Object obj : metadata.getAny()) {
            if (obj instanceof Element) {
                Node node = (Element)obj;
                if (node.getNamespaceURI().equals(JAXWSAConstants.NS_WSAW)
                    && node.getNodeName().contains("InterfaceName")) {
                    
                    String content = node.getTextContent();
                    String namespaceURI = node.getFirstChild().getNamespaceURI();
                    //String service = content;
                    if (content.contains(":")) {
                        namespaceURI = getNameSpaceUri(node, content, namespaceURI);
                        if (StringUtils.isEmpty(namespaceURI)) {
                            namespaceURI = findNamespaceHack(ref, bus);                                
                        }
                        content = getService(content);
                    } else {
                        Node nodeAttr = node.getAttributes().getNamedItem("xmlns");
                        namespaceURI = nodeAttr.getNodeValue();
                    }

                    return new QName(namespaceURI, content);
                }
            } else if (obj instanceof JAXBElement) {
                Object val = ((JAXBElement)obj).getValue();
                if (val instanceof AttributedQNameType) {
                    return ((AttributedQNameType)val).getValue();
                }
            } else if (obj instanceof AttributedQNameType) {
                return ((AttributedQNameType)obj).getValue();
            }
        }

        return null;
    }
    
    private static String findNamespaceHack(EndpointReferenceType ref, Bus bus) {
        //probably a broken version of Xalan, we'll have to 
        //try a hack to figure out the namespace as xalan
        //dropped the namespace declaration so there isn't 
        //a way to map the namespace prefix to the real namespace.
        //This is fixed in xalan 2.7.1, but older versions may 
        //be used
        if (bus == null) {
            return "";
        }
        String wsdlLocation = getWSDLLocation(ref);
        if (StringUtils.isEmpty(wsdlLocation)) {
            return "";
        }
        WSDLManager manager = bus.getExtension(WSDLManager.class);
        if (manager != null) {
            try {
                Definition def = manager.getDefinition(wsdlLocation);
                return def.getTargetNamespace();
            } catch (WSDLException e) {
                //ignore
            }
        }
        return "";
    }

    public static void setWSDLLocation(EndpointReferenceType ref, String... wsdlLocation) {
        
        MetadataType metadata = ref.getMetadata();
        if (null == metadata) {
            metadata = WSA_OBJECT_FACTORY.createMetadataType();
            ref.setMetadata(metadata);
        }

        //wsdlLocation attribute is a list of anyURI.
        StringBuffer strBuf = new StringBuffer();
        for (String str : wsdlLocation) {
            strBuf.append(str);
            strBuf.append(" ");
        }

        metadata.getOtherAttributes().put(WSDL_LOCATION, strBuf.toString().trim());
    }
    
    public static String getWSDLLocation(EndpointReferenceType ref) {
        String wsdlLocation = null;
        MetadataType metadata = ref.getMetadata();

        if (metadata != null) {
            wsdlLocation = metadata.getOtherAttributes().get(WSDL_LOCATION);
        }

        if (null == wsdlLocation) {
            return null;
        }

        //TODO The wsdlLocation inserted should be a valid URI 
        //before doing a split. So temporarily return the string
        //return wsdlLocation.split(" ");
        return wsdlLocation;
    }

    /**
     * Sets the metadata on the provided endpoint reference.
     * @param ref the endpoint reference.
     * @param metadata the list of metadata source.
     */
    public static void setMetadata(EndpointReferenceType ref, List<Source> metadata)
        throws EndpointUtilsException {
        
        if (null != ref) {
            MetadataType mt = ref.getMetadata();
            if (null == mt) {
                mt = new MetadataType();
                ref.setMetadata(mt);
            }
            List<Object> anyList = mt.getAny();
            try {
                for (Source source : metadata) {
                    Node node = null;
                    boolean doTransform = true;
                    if (source instanceof StreamSource) {
                        StreamSource ss = (StreamSource)source;
                        if (null == ss.getInputStream()
                            && null == ss.getReader()) {
                            setWSDLLocation(ref, ss.getSystemId());
                            doTransform = false;
                        }
                    } else if (source instanceof DOMSource) {
                        node = ((DOMSource)source).getNode();
                        doTransform = false;
                    } 
                    
                    if (doTransform) {
                        DOMResult domResult = new DOMResult();
                        domResult.setSystemId(source.getSystemId());
                        
                        getTransformer().transform(source, domResult);
    
                        node = domResult.getNode();
                    }
                    
                    if (null != node) {
                        if (node instanceof Document) {
                            ((Document)node).setDocumentURI(source.getSystemId());
                            node =  node.getFirstChild();
                        }
                        
                        while (node.getNodeType() != Node.ELEMENT_NODE) {
                            node = node.getNextSibling();
                        }
                        
                        anyList.add(node);
                    }
                }
            } catch (TransformerException te) {
                throw new EndpointUtilsException(new Message("COULD_NOT_POPULATE_EPR", LOG),
                                                 te);
            }
        }
    }
   
    /**
     * Gets the WSDL definition for the provided endpoint reference.
     * @param manager - the WSDL manager 
     * @param ref - the endpoint reference
     * @return Definition the wsdl definition
     * @throws WSDLException
     */
    public static Definition getWSDLDefinition(WSDLManager manager, EndpointReferenceType ref)
        throws WSDLException {

        if (null == manager) {
            return null;
        }

        MetadataType metadata = ref.getMetadata();
        String location = getWSDLLocation(ref);

        if (null != location) {
            //Pick up the first url to obtain the wsdl defintion
            return manager.getDefinition(location);
        }

        for (Object obj : metadata.getAny()) {
            if (obj instanceof Element) {
                Element el = (Element)obj;
                if (StringUtils.isEqualUri(el.getNamespaceURI(), WSDLConstants.NS_WSDL11)
                    && "definitions".equals(el.getLocalName())) {
                    return manager.getDefinition(el);
                }
            }
        }

        return null;
    }
    
    
    private static Schema createSchema(ServiceInfo serviceInfo) {
        Schema schema = serviceInfo.getProperty(Schema.class.getName(), Schema.class);
        if (schema == null) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Map<String, String> schemaSourcesMap = new LinkedHashMap<String, String>();
            Map<String, Source> schemaSourcesMap2 = new LinkedHashMap<String, Source>();

            try {
                for (SchemaInfo si : serviceInfo.getSchemas()) {
                    Element el = si.getElement();
                    String baseURI = el.getBaseURI();
                    if (baseURI == null) {
                        baseURI = si.getSystemId();
                    }
                    DOMSource ds = new DOMSource(el, baseURI);   
                    schemaSourcesMap2.put(si.getSystemId() + ":" + si.getNamespaceURI(), ds);
                    
                    String s = StaxUtils.toString(el);
                    schemaSourcesMap.put(si.getSystemId() + ":" + si.getNamespaceURI(), s);
                }

                
                for (XmlSchema sch : serviceInfo.getXmlSchemaCollection().getXmlSchemas()) {
                    if (sch.getSourceURI() != null
                        && !schemaSourcesMap.containsKey(sch.getSourceURI() + ":" 
                                                         + sch.getTargetNamespace())) { 
                        
                        InputStream ins = null;
                        try {
                            URL url = new URL(sch.getSourceURI());
                            ins = url.openStream();
                        } catch (Exception e) {
                            //ignore, we'll just use what we have.  (though
                            //bugs in XmlSchema could make this less useful)
                        }
                        
                        if (ins == null) {
                            LoadingByteArrayOutputStream out = new LoadingByteArrayOutputStream();
                            sch.write(out);
                            ins = out.createInputStream();
                        }
                        Document doc = XMLUtils.parse(ins);
                        try {
                            ins.close();
                        } catch (IOException ex) {
                            //ignore
                        }
                        String s = StaxUtils.toString(doc);
                        schemaSourcesMap.put(sch.getSourceURI() + ":" 
                                             + sch.getTargetNamespace(), s);
                        
                        Source source = new StreamSource(new StringReader(s), sch.getSourceURI());
                        schemaSourcesMap2.put(sch.getSourceURI() + ":" 
                                              + sch.getTargetNamespace(), source);
                    }
                } 


                factory.setResourceResolver(new SchemaLSResourceResolver(schemaSourcesMap));
                schema = factory.newSchema(schemaSourcesMap2.values()
                                           .toArray(new Source[schemaSourcesMap2.size()]));
                
                
            } catch (Exception ex) {
                // Something not right with the schema from the wsdl.
                LOG.log(Level.WARNING, "SAXException for newSchema() on ", ex);
                for (SchemaInfo schemaInfo : serviceInfo.getSchemas()) {
                    String s = XMLUtils.toString(schemaInfo.getElement(), 4);
                    LOG.log(Level.WARNING, "Schema for: " + schemaInfo.getNamespaceURI() + "\n" + s);
                }
            }
            serviceInfo.setProperty(Schema.class.getName(), schema);
        }
        return schema;
    }
    
    public static Schema getSchema(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return null;
        }
        Schema schema = serviceInfo.getProperty(Schema.class.getName(), Schema.class);
        if (schema == null && !serviceInfo.hasProperty(Schema.class.getName())) {
            synchronized (serviceInfo) {
                return createSchema(serviceInfo);
            }
        }
        return schema;
    }
    

    /**
     * Gets the WSDL port for the provided endpoint reference.
     * @param manager - the WSDL manager 
     * @param ref - the endpoint reference
     * @return Port the wsdl port
     * @throws WSDLException
     */
    public static Port getPort(WSDLManager manager, EndpointReferenceType ref) throws WSDLException {

        Definition def = getWSDLDefinition(manager, ref);
        if (def == null) {
            throw new WSDLException(WSDLException.OTHER_ERROR, "unable to find definition for reference");
        }

        MetadataType metadata = ref.getMetadata();
        for (Object obj : metadata.getAny()) {
            
            if (obj instanceof JAXBElement) {
                Object jaxbVal = ((JAXBElement)obj).getValue();

                if (jaxbVal instanceof ServiceNameType) {
                    Port port = null;
                    ServiceNameType snt = (ServiceNameType)jaxbVal;
                    LOG.log(Level.FINEST, "found service name " + snt.getValue().getLocalPart());
                    Service service = def.getService(snt.getValue());
                    if (service == null) {
                        LOG.log(Level.WARNING, "can't find the service name ["
                                + snt.getValue()
                                + "], using the default service name in wsdl");
                        service = (Service)def.getServices().values().iterator().next();
                        if (service == null) {
                            return null;
                        }
                    }
                    String endpoint = snt.getEndpointName();
                    if ("".equals(endpoint) && service.getPorts().size() == 1) {
                        port = (Port)service.getPorts().values().iterator().next();
                    } else {
                        port = service.getPort(endpoint);
                    }
                    // FIXME this needs to be looked at service.getPort(endpoint)
                    //should not return null when endpoint is valid
                    if (port == null) {
                        LOG.log(Level.WARNING, "can't find the port name ["
                                + endpoint
                                + "], using the default port name in wsdl");
                        port = (Port)service.getPorts().values().iterator().next();
                    }
                    return port;
                }
            }
        }

        if (def.getServices().size() == 1) {
            Service service = (Service)def.getServices().values().iterator().next();
            if (service.getPorts().size() == 1) { 
                return (Port)service.getPorts().values().iterator().next();
            }
        }
        
        QName serviceName = getServiceName(ref, null);
        if (null != serviceName) {
            if (StringUtils.isEmpty(serviceName.getNamespaceURI())) {
                serviceName = new QName(def.getTargetNamespace(), serviceName.getLocalPart());
            }
            Service service = def.getService(serviceName);
            if (service == null) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Cannot find service for " + serviceName);
            }
            if (service.getPorts().size() == 1) {
                return (Port)service.getPorts().values().iterator().next();
            }
            String str = getPortName(ref);
            LOG.log(Level.FINE, "getting port " + str + " from service " + service.getQName());
            Port port = service.getPort(str);
            if (port == null) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "unable to find port " + str);
            }
            return port;
        }
        // TODO : throw exception here
        return null;
    }

    /**
     * Get the address from the provided endpoint reference.
     * @param ref - the endpoint reference
     * @return String the address of the endpoint
     */
    public static String getAddress(EndpointReferenceType ref) {
        AttributedURIType a = ref.getAddress();
        
        if (null != a) {
            return a.getValue();
        }
        // should wsdl be parsed for an address now?
        return null;
    }

    /**
     * Set the address of the provided endpoint reference.
     * @param ref - the endpoint reference
     * @param address - the address
     */
    public static void setAddress(EndpointReferenceType ref, String address) {
        AttributedURIType a = WSA_OBJECT_FACTORY.createAttributedURIType();
        a.setValue(address);
        ref.setAddress(a);
    }
    /**
     * Create an endpoint reference for the provided wsdl, service and portname.
     * @param wsdlUrl - url of the wsdl that describes the service.
     * @param serviceName - the <code>QName</code> of the service.
     * @param portName - the name of the port.
     * @return EndpointReferenceType - the endpoint reference
     */
    public static EndpointReferenceType getEndpointReference(URL wsdlUrl, 
                                                             QName serviceName,
                                                             String portName) {
        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        reference.setMetadata(WSA_OBJECT_FACTORY.createMetadataType());
        setServiceAndPortName(reference, serviceName, portName);
        //TODO To Ensure it is a valid URI syntax.
        setWSDLLocation(reference, wsdlUrl.toString());

        return reference;
    }
    
    
    /**
     * Create a duplicate endpoint reference sharing all atributes
     * @param ref the reference to duplicate
     * @return EndpointReferenceType - the duplicate endpoint reference
     */
    public static EndpointReferenceType duplicate(EndpointReferenceType ref) {

        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        reference.setMetadata(ref.getMetadata());
        reference.getAny().addAll(ref.getAny());
        reference.setAddress(ref.getAddress());
        return reference;
    }
    
    /**
     * Create an endpoint reference for the provided address.
     * @param address - address URI
     * @return EndpointReferenceType - the endpoint reference
     */
    public static EndpointReferenceType getEndpointReference(String address) {

        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        setAddress(reference, address);
        return reference;
    }
    
    public static EndpointReferenceType getEndpointReference(AttributedURIType address) {

        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        reference.setAddress(address);
        return reference;
    }    
    
    /**
     * Create an anonymous endpoint reference.
     * @return EndpointReferenceType - the endpoint reference
     */
    public static EndpointReferenceType getAnonymousEndpointReference() {
        
        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        setAddress(reference, ANONYMOUS_ADDRESS);
        return reference;
    }
    
    /**
     * Resolve logical endpoint reference via the Bus EndpointResolverRegistry.
     * 
     * @param logical the abstract EPR to resolve
     * @return the resolved concrete EPR if appropriate, null otherwise
     */
    public static EndpointReferenceType resolve(EndpointReferenceType logical, Bus bus) {
        EndpointReferenceType physical = null;
        if (bus != null) {
            EndpointResolverRegistry registry =
                bus.getExtension(EndpointResolverRegistry.class);
            if (registry != null) {
                physical = registry.resolve(logical);
            }
        }
        return physical != null ? physical : logical;
    }

    
    /**
     * Renew logical endpoint reference via the Bus EndpointResolverRegistry.
     * 
     * @param logical the original abstract EPR (if still available)
     * @param physical the concrete EPR to renew
     * @return the renewed concrete EPR if appropriate, null otherwise
     */
    public static EndpointReferenceType renew(EndpointReferenceType logical,
                                              EndpointReferenceType physical,
                                              Bus bus) {
        EndpointReferenceType renewed = null;
        if (bus != null) {
            EndpointResolverRegistry registry =
                bus.getExtension(EndpointResolverRegistry.class);
            if (registry != null) {
                renewed = registry.renew(logical, physical);
            }
        }
        return renewed != null ? renewed : physical;
    }

    /**
     * Mint logical endpoint reference via the Bus EndpointResolverRegistry.
     * 
     * @param serviceName the given serviceName
     * @return the newly minted EPR if appropriate, null otherwise
     */
    public static EndpointReferenceType mint(QName serviceName, Bus bus) {
        EndpointReferenceType logical = null;
        if (bus != null) {
            EndpointResolverRegistry registry =
                bus.getExtension(EndpointResolverRegistry.class);
            if (registry != null) {
                logical = registry.mint(serviceName);
            }
        }
        return logical;
    }
    
    /**
     * Mint logical endpoint reference via the Bus EndpointResolverRegistry.
     * 
     * @param physical the concrete template EPR 
     * @return the newly minted EPR if appropriate, null otherwise
     */
    public static EndpointReferenceType mint(EndpointReferenceType physical, Bus bus) {
        EndpointReferenceType logical = null;
        if (bus != null) {
            EndpointResolverRegistry registry =
                bus.getExtension(EndpointResolverRegistry.class);
            if (registry != null) {
                logical = registry.mint(physical);
            }
        }
        return logical != null ? logical : physical;
    }
                                             
    private static String getNameSpaceUri(Node node, String content, String namespaceURI) {
        if (namespaceURI == null) {
            namespaceURI =  node.lookupNamespaceURI(content.substring(0, 
                                                                  content.indexOf(":")));
        }
        return namespaceURI;
    }

    private static String getService(String content) {
        return content.substring(content.indexOf(":") + 1, content.length());
    }

    /**
     * Obtain a multiplexed endpoint reference for the deployed service that contains the provided id
     * @param serviceQName identified the target service
     * @param portName identifies a particular port of the service, may be null
     * @param id that must be embedded in the returned reference
     * @param bus the current bus
     * @return a new reference or null if the target destination does not support destination mutiplexing  
     */
    public static EndpointReferenceType getEndpointReferenceWithId(QName serviceQName, 
                                                                   String portName, 
                                                                   String id, 
                                                                   Bus bus) {
        EndpointReferenceType epr = null;        
        MultiplexDestination destination = getMatchingMultiplexDestination(serviceQName, portName, bus);
        if (null != destination) {
            epr = destination.getAddressWithId(id);
        }
        return epr;
    }
    
    /**
     * Obtain the id String from the endpoint reference of the current dispatch. 
     * @param messageContext the current message context 
     * @return the id embedded in the current endpoint reference or null if not found
     */
    public static String getEndpointReferenceId(Map messageContext) {
        String id = null;
        Destination destination = (Destination) messageContext.get(Destination.class.getName());
        if (destination instanceof MultiplexDestination) {
            id = ((MultiplexDestination) destination).getId(messageContext);
        }
        return id;
    }
    
    public static Source convertToXML(EndpointReferenceType epr) {
        try {
            JAXBContext jaxbContext = 
                JAXBContext.newInstance(new Class[] {WSA_WSDL_OBJECT_FACTORY.getClass(), 
                                                     WSA_OBJECT_FACTORY.getClass()});
            javax.xml.bind.Marshaller jm = jaxbContext.createMarshaller();
            jm.setProperty(Marshaller.JAXB_FRAGMENT, true);
            QName qname = new QName("http://www.w3.org/2005/08/addressing", "EndpointReference");
            JAXBElement<EndpointReferenceType> 
            jaxEle = new JAXBElement<EndpointReferenceType>(qname, EndpointReferenceType.class, epr);
            
            
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
            jm.marshal(jaxEle, writer); 
            return new DOMSource(writer.getDocument());
        } catch (JAXBException e) {
            //ignore
        } catch (ParserConfigurationException e) {
            //ignore
        }
        return null;
    }
    
    
    private static MultiplexDestination getMatchingMultiplexDestination(QName serviceQName, String portName,
                                                                        Bus bus) {
        MultiplexDestination destination = null;
        ServerRegistry serverRegistry = (ServerRegistry)bus.getExtension(ServerRegistry.class);
        if (null != serverRegistry) {
            List<Server> servers = serverRegistry.getServers();
            for (Server s : servers) {
                QName targetServiceQName = s.getEndpoint().getEndpointInfo().getService().getName();
                if (serviceQName.equals(targetServiceQName) && portNameMatches(s, portName)) {
                    Destination dest = s.getDestination();
                    if (dest instanceof MultiplexDestination) {
                        destination = (MultiplexDestination)dest;
                        break;
                    }
                }
            }
        } else {
            LOG.log(Level.WARNING,
                    "Failed to locate service matching " + serviceQName 
                    + ", because the bus ServerRegistry extension provider is null");
        }
        return destination;
    }

    private static boolean portNameMatches(Server s, String portName) {
        boolean ret = false;
        if (null == portName 
            || portName.equals(s.getEndpoint().getEndpointInfo().getName().getLocalPart())) {
            return true;
        }
        return ret;
    }
    
   
    
}


class LSInputImpl implements LSInput {

    protected String fPublicId;

    protected String fSystemId;

    protected String fBaseSystemId;

    protected InputStream fByteStream;

    protected Reader fCharStream;

    protected String fData;

    protected String fEncoding;

    protected boolean fCertifiedText;

    public LSInputImpl() {
    }

    public LSInputImpl(String publicId, String systemId, InputStream byteStream) {
        fPublicId = publicId;
        fSystemId = systemId;
        fByteStream = byteStream;
    }

    public InputStream getByteStream() {
        return fByteStream;
    }

    public void setByteStream(InputStream byteStream) {
        fByteStream = byteStream;
    }

    public Reader getCharacterStream() {
        return fCharStream;
    }

    public void setCharacterStream(Reader characterStream) {
        fCharStream = characterStream;
    }

    public String getStringData() {
        return fData;
    }

    public void setStringData(String stringData) {
        fData = stringData;
    }

    public String getEncoding() {
        return fEncoding;
    }

    public void setEncoding(String encoding) {
        fEncoding = encoding;
    }

    public String getPublicId() {
        return fPublicId;
    }

    public void setPublicId(String publicId) {
        fPublicId = publicId;
    }

    public String getSystemId() {
        return fSystemId;
    }

    public void setSystemId(String systemId) {
        fSystemId = systemId;
    }

    public String getBaseURI() {
        return fBaseSystemId;
    }

    public void setBaseURI(String baseURI) {
        fBaseSystemId = baseURI;
    }

    public boolean getCertifiedText() {
        return fCertifiedText;
    }

    public void setCertifiedText(boolean certifiedText) {
        fCertifiedText = certifiedText;
    }

}
