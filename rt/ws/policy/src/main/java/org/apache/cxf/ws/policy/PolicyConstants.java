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

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.neethi.Constants;

/**
 * Encapsulation of version-specific WS-Policy constants.
 */
public final class PolicyConstants {
    
    public static final String WSU_NAMESPACE_URI = Constants.URI_WSU_NS;
    public static final String WSU_ID_ATTR_NAME = Constants.ATTR_ID;
    

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
        QName qn = DOMUtils.getElementQName(el);
        if (localName.equals(qn.getLocalPart()) && Constants.isInPolicyNS(qn)) {
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
            if (Constants.isOptionalAttribute(qn)) {
                return att;
            }
        }
        return null;
    }

    
    public static boolean isIgnorable(Element e) {
        Attr at = findIgnorableAttribute(e);
        if (at != null) {
            String v = at.getValue();
            return "true".equalsIgnoreCase(v) || "1".equals(v);
        }
        return false;
    }
    public static Attr findIgnorableAttribute(Element e) {
        NamedNodeMap atts = e.getAttributes();
        for (int x = 0; x < atts.getLength(); x++) {
            Attr att = (Attr)atts.item(x);
            QName qn = new QName(att.getNamespaceURI(), att.getLocalName());
            if (Constants.isIgnorableAttribute(qn)) {
                return att;
            }
        }
        return null;
    }
    
    
    public static Element findPolicyElement(Element parent) {
        Node nd = parent.getFirstChild();
        while (nd != null) {
            if (nd instanceof Element) {
                QName qn = DOMUtils.getElementQName((Element)nd);
                if (Constants.isPolicyElement(qn)) {
                    return (Element)nd;
                }
            }
            nd = nd.getNextSibling();
        }
        return null;
    }
}
