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

package org.apache.cxf.ws.policy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;

/**
 * Encapsulation of version-specific WS-Policy constants.
 */
public final class PolicyConstants {
    
    public static final String NAMESPACE_WS_POLICY
        = "http://www.w3.org/ns/ws-policy";
    
    public static final String NAMESPACE_W3_200607
        = "http://www.w3.org/2006/07/ws-policy";
    
    public static final String NAMESPACE_XMLSOAP_200409
        = "http://schemas.xmlsoap.org/ws/2004/09/policy";
    
    
    public static final String POLICY_ELEM_NAME = "Policy";
    public static final String POLICYREFERENCE_ELEM_NAME = "PolicyReference";
    public static final String POLICYATTACHMENT_ELEM_NAME = "PolicyAttachment";
    public static final String APPLIESTO_ELEM_NAME = "AppliesTo";

    public static final String WSU_NAMESPACE_URI = 
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static final String WSU_ID_ATTR_NAME = "Id";

    public static final String POLICY_OVERRIDE 
        = "org.apache.cxf.ws.policy.override";
        
    public static final String POLICY_IN_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.PolicyInInterceptor";
    public static final String POLICY_OUT_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.PolicyOutInterceptor";
    public static final String CLIENT_POLICY_IN_FAULT_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.ClientPolicyInFaultInterceptor";
    public static final String SERVER_POLICY_OUT_FAULT_INTERCEPTOR_ID
        = "org.apache.cxf.ws.policy.ServerPolicyOutFaultInterceptor";
    
    public static final String CLIENT_OUT_ASSERTIONS
        = "org.apache.cxf.ws.policy.client.out.assertions";
    public static final String CLIENT_IN_ASSERTIONS
        = "org.apache.cxf.ws.policy.client.in.assertions";
    public static final String CLIENT_INFAULT_ASSERTIONS
        = "org.apache.cxf.ws.policy.client.infault.assertions";
    
    public static final String SERVER_IN_ASSERTIONS
        = "org.apache.cxf.ws.policy.server.in.assertions";
    public static final String SERVER_OUT_ASSERTIONS
        = "org.apache.cxf.ws.policy.server.out.assertions";
    public static final String SERVER_OUTFAULT_ASSERTIONS
        = "org.apache.cxf.ws.policy.server.outfault.assertions";
    
    
    private static final String ALL_ELEM_NAME = "All";
    private static final String EXACTLYONE_ELEM_NAME = "ExactlyOne";
    private static final String OPTIONAL_ATTR_NAME = "Optional"; 
    private static final String POLICYURIS_ATTR_NAME = "PolicyURIs";
    
    
    
    private static final Set<String> SUPPORTED_NAMESPACES = new HashSet<String>();
    static {
        SUPPORTED_NAMESPACES.add(NAMESPACE_WS_POLICY);
        SUPPORTED_NAMESPACES.add(NAMESPACE_W3_200607);
        SUPPORTED_NAMESPACES.add(NAMESPACE_XMLSOAP_200409);
    }
    
    private PolicyConstants() {
        //utility class
    }
    
    public static List<Element> findAllPolicyElementsOfLocalName(Document doc, String localName) {
        return findAllPolicyElementsOfLocalName(doc.getDocumentElement(), localName);
    }
    public static List<Element> findAllPolicyElementsOfLocalName(Element el, String localName) {
        List<Element> ret = new LinkedList<Element>();
        findAllPolicyElementsOfLocalName(el, localName, ret);
        return ret;
    }
    public static void findAllPolicyElementsOfLocalName(Element el, String localName, List<Element> val) {
        if (localName.equals(el.getLocalName()) && SUPPORTED_NAMESPACES.contains(el.getNamespaceURI())) {
            val.add(el);
        }
        el = DOMUtils.getFirstElement(el);
        while (el != null) {
            findAllPolicyElementsOfLocalName(el, localName, val);
            el = DOMUtils.getNextElement(el);
        }
    }

    public static boolean isOptional(Element e) {
        Attr at = findOptionalAttribute(e);
        if (at != null) {
            String v = at.getValue();
            return "true".equalsIgnoreCase(v) || "1".equals(v);
        }
        return false;
    }
    public static Attr findOptionalAttribute(Element e) {
        NamedNodeMap atts = e.getAttributes();
        for (int x = 0; x < atts.getLength(); x++) {
            Attr att = (Attr)atts.item(x);
            QName qn = new QName(att.getNamespaceURI(), att.getLocalName());
            if (isOptionalAttribute(qn)) {
                return att;
            }
        }
        return null;
    }

    public static Element findPolicyElement(Element parent) {
        Node nd = parent.getFirstChild();
        while (nd != null) {
            if (POLICY_ELEM_NAME.equals(nd.getLocalName())
                && SUPPORTED_NAMESPACES.contains(nd.getNamespaceURI())) {
                return (Element)nd;
            }
            nd = nd.getNextSibling();
        }
        return null;
    }
    public static boolean isOptionalAttribute(QName qn) {
        return OPTIONAL_ATTR_NAME.equals(qn.getLocalPart())
            && SUPPORTED_NAMESPACES.contains(qn.getNamespaceURI());
    }
    public static boolean isPolicyElem(QName qn) {
        return POLICY_ELEM_NAME.equals(qn.getLocalPart())
            && SUPPORTED_NAMESPACES.contains(qn.getNamespaceURI());
    }
    public static boolean isPolicyRefElem(QName qn) {
        return POLICYREFERENCE_ELEM_NAME.equals(qn.getLocalPart())
            && SUPPORTED_NAMESPACES.contains(qn.getNamespaceURI());
    }
    public static boolean isAppliesToElem(QName qn) {
        return APPLIESTO_ELEM_NAME.equals(qn.getLocalPart())
            && SUPPORTED_NAMESPACES.contains(qn.getNamespaceURI());
    }
    public static boolean isPolicyURIsAttr(QName qn) {
        return POLICYURIS_ATTR_NAME.equals(qn.getLocalPart())
            && SUPPORTED_NAMESPACES.contains(qn.getNamespaceURI());
    }
    public static boolean isExactlyOne(QName qn) {
        return EXACTLYONE_ELEM_NAME.equals(qn.getLocalPart())
            && SUPPORTED_NAMESPACES.contains(qn.getNamespaceURI());
    }
    public static boolean isAll(QName qn) {
        return ALL_ELEM_NAME.equals(qn.getLocalPart())
            && SUPPORTED_NAMESPACES.contains(qn.getNamespaceURI());
    }
}
