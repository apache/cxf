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

import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.neethi.All;
import org.apache.neethi.Constants;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyOperator;
import org.apache.neethi.PolicyReference;


/**
 * PolicyBuilderImpl is an implementation of the PolicyBuilder interface,
 * provides methods to create Policy and PolicyReferenceObjects
 * from DOM elements, but also from an input stream etc.
 */
public class PolicyBuilderImpl implements PolicyBuilder, BusExtension {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(PolicyBuilderImpl.class);
 
    private AssertionBuilderRegistry assertionBuilderRegistry;
    private Bus bus;
   
    public Class<?> getRegistrationType() {
        return PolicyBuilder.class;
    }
 
    public void setBus(Bus theBus) {
        bus = theBus;
    }
    
    public Bus getBus() {
        return bus;
    }
    
    public void setAssertionBuilderRegistry(AssertionBuilderRegistry abr) {
        assertionBuilderRegistry = abr;        
    }
    
    public AssertionBuilderRegistry getAssertionBuilderRegistry() {
        return assertionBuilderRegistry;        
    }


    /**
     * Creates a PolicyReference object from an InputStream.
     * 
     * @param inputStream the input stream
     * @return the PolicyReference constructed from the input stream
     */
    public PolicyReference getPolicyReference(InputStream is)
        throws IOException, ParserConfigurationException, SAXException {
        Element element = DOMUtils.readXml(is).getDocumentElement();
        return getPolicyReference(element);
    }
    
    /**
     * Creates a PolicyReference object from a DOM element.
     * 
     * @param element the element
     * @return the PolicyReference object constructed from the element
     */
    public PolicyReference getPolicyReference(Element element) {
        if (!Constants.ELEM_POLICY_REF.equals(element.getLocalName())) {
            throw new PolicyException(new Message("NOT_A_POLICYREF_ELEMENT_EXC", BUNDLE));
        }
        synchronized (element) {
            PolicyReference reference = new PolicyReference();
            reference.setURI(element.getAttribute("URI"));
            return reference;
        }
    }
    
    /**
     * Creates a Policy object from an InputStream.
     * 
     * @param inputStream the input stream
     * @return the Policy object constructed from the input stream
     */
    public Policy getPolicy(InputStream is) 
        throws IOException, ParserConfigurationException, SAXException {
        Element element = DOMUtils.readXml(is).getDocumentElement();
        return getPolicy(element);
    }
    
    /**
     * Creates a Policy object from a DOM element.
     * 
     * @param element the element
     * @retun the Policy object constructed from the element
     */
    public Policy getPolicy(Element element) {
        return getPolicyOperator(element);
    }
    
    private Policy getPolicyOperator(Element element) {
        return (Policy) processOperationElement(element, new Policy());
    }

    private ExactlyOne getExactlyOneOperator(Element element) {
        return (ExactlyOne) processOperationElement(element, new ExactlyOne());
    }

    private All getAllOperator(Element element) {
        return (All) processOperationElement(element, new All());
    }

    private PolicyOperator processOperationElement(Element operationElement, PolicyOperator operator) {
        synchronized (operationElement) {
            if (Constants.TYPE_POLICY == operator.getType()) {
                Policy policyOperator = (Policy)operator;
    
                QName key;
    
                NamedNodeMap nnm = operationElement.getAttributes();
                for (int i = 0; i < nnm.getLength(); i++) {
                    Node n = nnm.item(i);
                    if (Node.ATTRIBUTE_NODE == n.getNodeType()) {
                        String namespace = n.getNamespaceURI();    
                        if (namespace == null) {
                            key = new QName(n.getLocalName());
    
                        } else if (n.getPrefix() == null) {
                            key = new QName(namespace, n.getLocalName());
    
                        } else {
                            key = new QName(namespace, n.getLocalName(), n.getPrefix());
                        }
                        policyOperator.addAttribute(key, n.getNodeValue());
                    }
                }            
            }
    
            String policyNsURI = 
                bus == null ? PolicyConstants.NAMESPACE_WS_POLICY
                            : bus.getExtension(PolicyConstants.class).getNamespace();
            
            Element childElement;
            for (Node n = operationElement.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (Node.ELEMENT_NODE != n.getNodeType()) {
                    continue;
                }
                childElement = (Element)n;
                String namespaceURI = childElement.getNamespaceURI();
                String localName = childElement.getLocalName();
    
                if (policyNsURI.equals(namespaceURI)) {
    
                    if (Constants.ELEM_POLICY.equals(localName)) {
                        operator.addPolicyComponent(getPolicyOperator(childElement));
    
                    } else if (Constants.ELEM_EXACTLYONE.equals(localName)) {
                        operator.addPolicyComponent(getExactlyOneOperator(childElement));
    
                    } else if (Constants.ELEM_ALL.equals(localName)) {
                        operator.addPolicyComponent(getAllOperator(childElement));
    
                    } else if (Constants.ELEM_POLICY_REF.equals(localName)) {
                        operator.addPolicyComponent(getPolicyReference(childElement));
                    }
    
                } else if (null != assertionBuilderRegistry) {
                    PolicyAssertion a = assertionBuilderRegistry.build(childElement);
                    if (null != a) {
                        operator.addPolicyComponent(a);
                    }
                }
            }
            return operator;
        }
    }
    
}
