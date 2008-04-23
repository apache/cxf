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

package org.apache.cxf.ws.policy.attachment.external;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.policy.PolicyException;

/**
 * 
 */
public class Wsdl11XPointerDomainExpression implements DomainExpression {
    
    private static final String NAMESPACE = "http://schemas.xmlsoap.org/wsdl/";
    private static final String SERVICE_ELEM_NAME = "service";
    private static final String PORT_ELEM_NAME = "port";
    private static final String PORTTYPE_ELEM_NAME = "portType";
    private static final String BINDING_ELEM_NAME = "binding";
    private static final String OPERATION_ELEM_NAME = "operation";
    private static final String NAME_ATTR_NAME = "name";
    
    
    private String baseURI;
    private NodeList nodes;
    
    Wsdl11XPointerDomainExpression(String u) {
        baseURI = u;
    }
        
    public boolean appliesTo(BindingFaultInfo bfi) {
        throw new UnsupportedOperationException();
    }

    public boolean appliesTo(BindingMessageInfo bmi) {
        throw new UnsupportedOperationException();
    }

    public boolean appliesTo(BindingOperationInfo boi) {
        if (baseURI.equals(boi.getBinding().getDescription().getBaseURI())) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                if (Node.ELEMENT_NODE != n.getNodeType()) {
                    continue;
                }
                Element e = (Element)n;
                if (matchesBindingOperation(e, boi)) {
                    Element p = (Element)e.getParentNode();
                    return matchesBinding(p, boi.getBinding());
                } else if (matchesOperation(e, boi.getOperationInfo())) {         
                    return true;
                }
            }
        }
        return false;
    }



    public boolean appliesTo(EndpointInfo ei) {
        if (baseURI.equals(ei.getDescription().getBaseURI())) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                if (Node.ELEMENT_NODE != n.getNodeType()) {
                    continue;
                }
                Element e = (Element)n;
                if (matchesPort(e, ei)) {
                    Element p = (Element)e.getParentNode();
                    return matchesService(p, ei.getService());
                } else if (matchesPortType(e, ei.getInterface())) {         
                    return true;
                } else if (matchesBinding(e, ei.getBinding())) {
                    return true;
                }
            }
        }
        return false;       
    }



    public boolean appliesTo(ServiceInfo si) {
        if (baseURI.equals(si.getDescription().getBaseURI())) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                if (Node.ELEMENT_NODE != n.getNodeType()) {
                    continue;
                }
                Element e = (Element)n;
                if (matchesService(e, si)) {
                    return true;
                }
            }
        }        
        return false;
    }



    void evaluate(String uri) {
        int pos = uri.indexOf('#');
        
        String documentURI = uri.substring(0, pos);
        String path = uri.substring(pos + 1);
        
        InputSource is = new ExtendedURIResolver().resolve(documentURI, baseURI);
        if (null == is) {
            System.out.println("Failed to resolve: " + documentURI + " w.r.t baseURI: " + baseURI);
            return;
        }
        Document doc = null;
        try {
            doc = DOMUtils.readXml(is.getByteStream());
        } catch (Exception ex) {
            throw new PolicyException(ex);
        }
        
        XPathUtils xu = new XPathUtils();
        nodes = (NodeList)xu.getValue(path, doc, XPathConstants.NODESET);     
    }
    
    boolean matchesService(Element e, ServiceInfo si) {
        return NAMESPACE.equals(e.getNamespaceURI()) && SERVICE_ELEM_NAME.equals(e.getLocalName())
            && si.getName().getLocalPart().equals(e.getAttribute(NAME_ATTR_NAME));
    }
    
    boolean matchesPortType(Element e, InterfaceInfo ii) {
        return NAMESPACE.equals(e.getNamespaceURI()) && PORTTYPE_ELEM_NAME.equals(e.getLocalName())
            && ii.getName().getLocalPart().equals(e.getAttribute(NAME_ATTR_NAME));
    }
    
    boolean matchesPort(Element e, EndpointInfo ei) {
        return NAMESPACE.equals(e.getNamespaceURI()) && PORT_ELEM_NAME.equals(e.getLocalName())
            && ei.getName().getLocalPart().equals(e.getAttribute(NAME_ATTR_NAME));
    }
    
    boolean matchesBinding(Element e, BindingInfo ei) {
        return NAMESPACE.equals(e.getNamespaceURI()) && BINDING_ELEM_NAME.equals(e.getLocalName())
            && ei.getName().getLocalPart().equals(e.getAttribute(NAME_ATTR_NAME));
    }
    
    boolean matchesBindingOperation(Element e, BindingOperationInfo boi) {
        return NAMESPACE.equals(e.getNamespaceURI()) && OPERATION_ELEM_NAME.equals(e.getLocalName())
            && boi.getName().getLocalPart().equals(e.getAttribute(NAME_ATTR_NAME));
    }
    
    boolean matchesOperation(Element e, OperationInfo boi) {
        return NAMESPACE.equals(e.getNamespaceURI()) && OPERATION_ELEM_NAME.equals(e.getLocalName())
            && boi.getName().getLocalPart().equals(e.getAttribute(NAME_ATTR_NAME));
    }
    
    
    
}
