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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.customization;

import java.util.Iterator;
import java.util.logging.Logger;

import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;

public class JAXWSBindingParser {
    private static final Logger LOG = LogUtils.getL7dLogger(CustomizationParser.class);
    private ExtensionRegistry extReg;

    public JAXWSBindingParser(ExtensionRegistry ext) {
        extReg = ext;
    }

    public JAXWSBinding parse(Class parentType, Element element, String namespace) throws WSDLException {
        JAXWSBinding jaxwsBinding = (JAXWSBinding)extReg.createExtension(parentType,
                                                                         ToolConstants.JAXWS_BINDINGS);

        jaxwsBinding.setElementType(ToolConstants.JAXWS_BINDINGS);
        jaxwsBinding.setElement(element);
        jaxwsBinding.setDocumentBaseURI(namespace);

        parseElement(jaxwsBinding, element);

        return jaxwsBinding;
    }

    void parseElement(JAXWSBinding jaxwsBinding, Element element) {
        Node child = element.getFirstChild();
        if (child == null) {
            // global binding
            if (isAsyncElement(element)) {
                jaxwsBinding.setEnableAsyncMapping(getNodeValue(element));
            }
            if (isMIMEElement(element)) {
                jaxwsBinding.setEnableMime(getNodeValue(element));
            }
            if (isPackageElement(element)) {
                jaxwsBinding.setPackage(getPackageName(element));
            }

            if (isWrapperStyle(element)) {
                jaxwsBinding.setEnableWrapperStyle(getNodeValue(element));
            }
        } else {
            // other binding
            while (child != null) {
                if (isAsyncElement(child)) {
                    jaxwsBinding.setEnableAsyncMapping(getNodeValue(child));
                } else if (isMIMEElement(child)) {
                    jaxwsBinding.setEnableMime(getNodeValue(child));
                } else if (isWrapperStyle(child)) {
                    jaxwsBinding.setEnableWrapperStyle(getNodeValue(child));
                } else if (isPackageElement(child)) {
                    jaxwsBinding.setPackage(getPackageName(child));
                    Node docChild = DOMUtils.getChild(child, Element.ELEMENT_NODE);
                    if (docChild != null && this.isJAXWSClassDoc(docChild)) {
                        jaxwsBinding.setPackageJavaDoc(DOMUtils.getContent(docChild));
                    }
                } else if (isJAXWSMethodElement(child)) {
                    jaxwsBinding.setMethodName(getMethodName(child));
                    Node docChild = DOMUtils.getChild(child, Element.ELEMENT_NODE);

                    if (docChild != null && this.isJAXWSClassDoc(docChild)) {
                        jaxwsBinding.setMethodJavaDoc(DOMUtils.getContent(docChild));
                    }
                } else if (isJAXWSParameterElement(child)) {
                    Element childElement = (Element)child;
                    String partPath = "//" +  childElement.getAttribute("part");
                    Node node = queryXPathNode(element.getOwnerDocument().getDocumentElement(), partPath);
                    String messageName = "";
                    if (node != null) {
                        Node messageNode = node.getParentNode();
                        if (messageNode != null) {
                            Element messageEle = (Element)messageNode;
                            messageName =  messageEle.getAttribute("name");
                        }
                    }

                    String name = childElement.getAttribute("name");
                    String elementName = childElement.getAttribute("childElementName");
                    JAXWSParameter jpara = new JAXWSParameter(messageName, elementName, name);
                    jaxwsBinding.setJaxwsPara(jpara);
                } else if (isJAXWSClass(child)) {
                    Element childElement = (Element)child;
                    String clzName = childElement.getAttribute("name");
                    String javadoc = "";
                    Node docChild = DOMUtils.getChild(child, Element.ELEMENT_NODE);

                    if (docChild != null && this.isJAXWSClassDoc(docChild)) {
                        javadoc  = DOMUtils.getContent(docChild);
                    }

                    JAXWSClass jaxwsClass = new JAXWSClass(clzName, javadoc);
                    jaxwsBinding.setJaxwsClass(jaxwsClass);
                }
                child = child.getNextSibling();
            }
        }

    }

    private boolean isJAXWSMethodElement(Node node) {
        return ToolConstants.NS_JAXWS_BINDINGS.equals(node.getNamespaceURI())
               && "method".equals(node.getLocalName());
    }

    private String getMethodName(Node node) {
        Element ele = (Element)node;
        return ele.getAttribute("name");
    }


    private boolean isPackageElement(Node node) {
        if (ToolConstants.NS_JAXWS_BINDINGS.equals(node.getNamespaceURI())
            && "package".equals(node.getLocalName())) {
            return true;
        }
        return false;

    }

    private boolean isJAXWSParameterElement(Node node) {
        return (ToolConstants.NS_JAXWS_BINDINGS.equals(node.getNamespaceURI()))
               && "parameter".equals(node.getLocalName());

    }

    private boolean isJAXWSClass(Node node) {
        return (ToolConstants.NS_JAXWS_BINDINGS.equals(node.getNamespaceURI()))
               && "class".equals(node.getLocalName());
    }

    private boolean isJAXWSClassDoc(Node node) {
        return (ToolConstants.NS_JAXWS_BINDINGS.equals(node.getNamespaceURI()))
               && "javadoc".equals(node.getLocalName());
    }


    private String getPackageName(Node node) {
        Element ele = (Element)node;
        return ele.getAttribute("name");
    }

    private Boolean isAsyncElement(Node node) {
        return "enableAsyncMapping".equals(node.getLocalName())
               && ToolConstants.NS_JAXWS_BINDINGS.equals(node.getNamespaceURI());
    }

    private Boolean isWrapperStyle(Node node) {
        return "enableWrapperStyle".equals(node.getLocalName())
               && ToolConstants.NS_JAXWS_BINDINGS.equals(node.getNamespaceURI());
    }

    private Boolean getNodeValue(Node node) {
        return Boolean.valueOf(node.getTextContent());
    }

    private Boolean isMIMEElement(Node node) {
        return "enableMIMEContent".equals(node.getLocalName())
               && ToolConstants.NS_JAXWS_BINDINGS.equals(node.getNamespaceURI());
    }

    private Node queryXPathNode(Node target, String expression) {
        NodeList nlst;
        try {
            ContextImpl contextImpl = new ContextImpl(target);
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(contextImpl);
            nlst = (NodeList)xpath.evaluate(expression, target, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            Message msg = new Message("XPATH_ERROR", LOG, new Object[] {expression});
            throw new ToolException(msg, e);
        }

        if (nlst.getLength() != 1) {
            Message msg = new Message("ERROR_TARGETNODE_WITH_XPATH", LOG, new Object[] {expression});
            throw new ToolException(msg);
        }

        Node rnode = nlst.item(0);
        if (!(rnode instanceof Element)) {
            return null;
        }
        return (Element)rnode;
    }

    class ContextImpl implements NamespaceContext {
        private Node targetNode;

        public ContextImpl(Node node) {
            targetNode = node;
        }

        public String getNamespaceURI(String prefix) {
            return targetNode.getOwnerDocument().lookupNamespaceURI(prefix);
        }

        public String getPrefix(String nsURI) {
            throw new UnsupportedOperationException();
        }

        public Iterator getPrefixes(String namespaceURI) {
            throw new UnsupportedOperationException();
        }
    }
}
