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

package org.apache.cxf.jaxws.handler;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.jaxws.javaee.PortComponentHandlerType;

public class AnnotationHandlerChainBuilder extends HandlerChainBuilder {

    private static final Logger LOG = LogUtils.getL7dLogger(AnnotationHandlerChainBuilder.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();
    private static JAXBContext context;
    
    private ClassLoader classLoader;
    
    public AnnotationHandlerChainBuilder() {
    }

    public AnnotationHandlerChainBuilder(Bus bus) {
        super(bus);
    }

    /**
     * @param clz
     * @param existingHandlers
     * @return
     */
    public List<Handler> buildHandlerChainFromClass(Class<?> clz, List<Handler> existingHandlers,
                                                    QName portQName, QName serviceQName, String bindingID) {
        LOG.fine("building handler chain");
        classLoader = clz.getClassLoader();
        HandlerChainAnnotation hcAnn = findHandlerChainAnnotation(clz, true);
        List<Handler> chain = null;
        if (hcAnn == null) {
            LOG.fine("no HandlerChain annotation on " + clz);
            chain = new ArrayList<Handler>();
        } else {
            hcAnn.validate();

            try {
                
                URL handlerFileURL  = resolveHandlerChainFile(clz, hcAnn.getFileName()); 
                if (handlerFileURL == null) {
                    throw new WebServiceException(new Message("HANDLER_CFG_FILE_NOT_FOUND_EXC", BUNDLE, hcAnn
                        .getFileName()).toString());
                }
                
                Document doc = XMLUtils.parse(handlerFileURL.openStream());
                Element el = doc.getDocumentElement();
                if (!"http://java.sun.com/xml/ns/javaee".equals(el.getNamespaceURI()) 
                    || !"handler-chains".equals(el.getLocalName())) {
                        
                    String xml = XMLUtils.toString(el);
                    throw new WebServiceException(
                        BundleUtils.getFormattedString(BUNDLE,
                                                       "NOT_VALID_ROOT_ELEMENT",
                                                       "http://java.sun.com/xml/ns/javaee"
                                                           .equals(el.getNamespaceURI()), 
                                                       "handler-chains".equals(el.getLocalName()),
                                                       xml, handlerFileURL));                    
                }
                chain = new ArrayList<Handler>();
                Node node = el.getFirstChild();
                while (node != null) {
                    if (node instanceof Element) {
                        el = (Element)node;
                        if (!el.getNamespaceURI().equals("http://java.sun.com/xml/ns/javaee") 
                            || !el.getLocalName().equals("handler-chain")) {
                                
                            String xml = XMLUtils.toString(el);
                            throw new WebServiceException(
                                BundleUtils.getFormattedString(BUNDLE,
                                                               "NOT_VALID_ELEMENT_IN_HANDLER",
                                                               xml));                    
                        }
                        processHandlerChainElement(el, chain,
                                                   portQName, serviceQName, bindingID);
                    }
                    node = node.getNextSibling();
                }
            } catch (WebServiceException e) {
                throw e;
            } catch (Exception e) {
                throw new WebServiceException(BUNDLE.getString("CHAIN_NOT_SPECIFIED_EXC"), e);
            }
        }
        assert chain != null;
        if (existingHandlers != null) {
            chain.addAll(existingHandlers);
        }
        return sortHandlers(chain);
    }

    private void processHandlerChainElement(Element el, List<Handler> chain,
                                            QName portQName, QName serviceQName, String bindingID) {
        Node node = el.getFirstChild();
        while (node != null) {
            Node cur = node;
            node = node.getNextSibling();            
            if (cur instanceof Element) {
                el = (Element)cur;
                if (!el.getNamespaceURI().equals("http://java.sun.com/xml/ns/javaee")) {
                    String xml = XMLUtils.toString(el);
                    throw new WebServiceException(
                        BundleUtils.getFormattedString(BUNDLE,
                                                       "NOT_VALID_ELEMENT_IN_HANDLER",
                                                       xml));                    
                }
                String name = el.getLocalName();
                if ("port-name-pattern".equals(name)) {
                    if (!patternMatches(el, portQName)) {
                        return;
                    }
                } else if ("service-name-pattern".equals(name)) {
                    if (!patternMatches(el, serviceQName)) {
                        return;
                    }
                } else if ("protocol-bindings".equals(name)) {
                    if (!protocolMatches(el, bindingID)) {
                        return;
                    }
                } else if ("handler".equals(name)) {
                    processHandlerElement(el, chain);
                }
            }
        }        
    }
    private boolean protocolMatches(Element el, String id) {
        if (id == null) {
            return true;
        }
        String name = el.getTextContent().trim();
        if ("##SOAP11_HTTP".equals(name)) {
            return "http://schemas.xmlsoap.org/wsdl/soap/http".contains(id)
                || "http://schemas.xmlsoap.org/soap/".contains(id);
        } else if ("##SOAP11_HTTP_MTOM".equals(name)) {
            return "http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true".contains(id)
                || "http://schemas.xmlsoap.org/soap/?mtom=true".contains(id);
        } else if ("##SOAP12_HTTP".equals(name)) {
            return "http://www.w3.org/2003/05/soap/bindings/HTTP/".contains(id)
                || "http://schemas.xmlsoap.org/wsdl/soap12/".contains(id);
        } else if ("##SOAP12_HTTP_MTOM".equals(name)) {
            return "http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true".contains(id)
                || "http://schemas.xmlsoap.org/wsdl/soap12/?mtom=true".contains(id);
        } else if ("##XML_HTTP".equals(name)) {
            name = "http://www.w3.org/2004/08/wsdl/http";
        }
        return name.contains(id);
    }
    private boolean patternMatches(Element el, QName comp) {
        if (comp == null) {
            return true;
        }
        String namePattern = el.getTextContent().trim();
        if ("*".equals(namePattern)) {
            return true;
        }
        if (!namePattern.contains(":")) {
            String xml = XMLUtils.toString(el);
            throw new WebServiceException(
                BundleUtils.getFormattedString(BUNDLE,
                                               "NOT_A_QNAME_PATTER",
                                               namePattern, xml));                    
        }
        String localPart = namePattern.substring(namePattern.indexOf(':') + 1,
                                                 namePattern.length());
        String pfx = namePattern.substring(0, namePattern.indexOf(':'));
        String ns = el.lookupNamespaceURI(pfx);
        if (ns == null) {
            ns = pfx;
        }
        if (!ns.equals(comp.getNamespaceURI())) {
            return false;
        }
        if (localPart.contains("*")) {
            //wildcard pattern matching
            return Pattern.matches(mapPattern(localPart), comp.getLocalPart());
        } else if (!localPart.equals(comp.getLocalPart())) {
            return false;
        }
        return true;
    }
    
    private String mapPattern(String s) {
        StringBuilder buf = new StringBuilder(s);
        for (int x = 0; x < buf.length(); x++) {
            switch (buf.charAt(x)) {
            case '*':
                buf.insert(x, '.');
                x++;
                break;
            case '.':
            case '\\':
            case '^':
            case '$':
            case '{':
            case '}':
            case '(':
            case ')':
                buf.insert(x, '\\');
                x++;
                break;
            default:
                //nothing to do
            }
        }
        return buf.toString();
    }
    
    private void processHandlerElement(Element el, List<Handler> chain) {
        try {
            JAXBContext ctx = getContextForPortComponentHandlerType();
            PortComponentHandlerType pt = ctx.createUnmarshaller()
                .unmarshal(el, PortComponentHandlerType.class).getValue();
            chain.addAll(buildHandlerChain(pt, classLoader));
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private static synchronized JAXBContext getContextForPortComponentHandlerType()
        throws JAXBException {
        if (context == null) {
            context = JAXBContext.newInstance(PortComponentHandlerType.class);
        }
        return context;
    }
    
    public List<Handler> buildHandlerChainFromClass(Class<?> clz, QName portQName, QName serviceQName,
                                                    String bindingID) {
        return buildHandlerChainFromClass(clz, null, portQName, serviceQName, bindingID);
    }
    
    protected URL resolveHandlerChainAnnotationFile(Class clazz, String name) {
        return clazz.getResource(name);
    }
    
    private HandlerChainAnnotation findHandlerChainAnnotation(Class<?> clz, boolean searchSEI) {        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Checking for HandlerChain annotation on " + clz.getName());
        }
        HandlerChainAnnotation hcAnn = null;
        HandlerChain ann = clz.getAnnotation(HandlerChain.class);
        if (ann == null) {
            if (searchSEI) {
                /* HandlerChain annotation can be specified on the SEI
                 * but the implementation bean might not implement the SEI.          
                 */
                WebService ws = clz.getAnnotation(WebService.class);
                if (ws != null && !StringUtils.isEmpty(ws.endpointInterface())) {
                    String seiClassName = ws.endpointInterface().trim();
                    Class seiClass = null;
                    try {
                        seiClass = clz.getClassLoader().loadClass(seiClassName);
                    } catch (ClassNotFoundException e) {
                        throw new WebServiceException(BUNDLE.getString("SEI_LOAD_FAILURE_EXC"), e);
                    }

                    // check SEI class and its interfaces for HandlerChain annotation
                    hcAnn = findHandlerChainAnnotation(seiClass, false);
                }
            }
            if (hcAnn == null) {
                // check interfaces for HandlerChain annotation
                for (Class<?> iface : clz.getInterfaces()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Checking for HandlerChain annotation on " + iface.getName());
                    }
                    ann = iface.getAnnotation(HandlerChain.class);
                    if (ann != null) {
                        hcAnn = new HandlerChainAnnotation(ann, iface);
                        break;
                    }
                }
            }
        } else {
            hcAnn = new HandlerChainAnnotation(ann, clz);
        }
        
        return hcAnn;
    }
    
    private static class HandlerChainAnnotation {
        private final Class<?> declaringClass;
        private final HandlerChain ann;

        HandlerChainAnnotation(HandlerChain hc, Class<?> clz) {
            ann = hc;
            declaringClass = clz;
        }

        public Class<?> getDeclaringClass() {
            return declaringClass;
        }

        public String getFileName() {
            return ann.file();
        }

        public void validate() {
            if (null == ann.file() || "".equals(ann.file())) {
                throw new WebServiceException(BUNDLE.getString("ANNOTATION_WITHOUT_URL_EXC"));
            }
        }

        public String toString() {
            return "[" + declaringClass + "," + ann + "]";
        }
    }
}
