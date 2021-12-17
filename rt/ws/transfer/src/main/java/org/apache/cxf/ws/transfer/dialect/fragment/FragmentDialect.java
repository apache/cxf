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

package org.apache.cxf.ws.transfer.dialect.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.annotation.Resource;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.Dialect;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.InvalidExpression;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.UnsupportedLanguage;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.UnsupportedMode;
import org.apache.cxf.ws.transfer.dialect.fragment.language.FragmentDialectLanguage;
import org.apache.cxf.ws.transfer.dialect.fragment.language.FragmentDialectLanguageQName;
import org.apache.cxf.ws.transfer.dialect.fragment.language.FragmentDialectLanguageXPath10;
import org.apache.cxf.ws.transfer.shared.faults.InvalidRepresentation;
import org.apache.cxf.ws.transfer.shared.faults.UnknownDialect;

/**
 * Implementation of the WS-Fragment dialect.
 */
public class FragmentDialect implements Dialect {

    @Resource
    WebServiceContext context;

    private final Map<String, FragmentDialectLanguage> languages = new HashMap<>();

    private final Pattern badXPathPattern =
        Pattern.compile("//@?" + FragmentDialectLanguageQName.getQNamePatternString() + '$');

    private final Pattern goodXPathPattern =
        Pattern.compile("/@?" + FragmentDialectLanguageQName.getQNamePatternString() + '$');

    public FragmentDialect() {
        languages.put(FragmentDialectConstants.QNAME_LANGUAGE_IRI, new FragmentDialectLanguageQName());
        languages.put(FragmentDialectConstants.XPATH10_LANGUAGE_IRI, new FragmentDialectLanguageXPath10());
    }

    @Override
    public JAXBElement<ValueType> processGet(Get body, Representation representation) {
        for (Object o : body.getAny()) {
            if (o instanceof JAXBElement<?> && ((JAXBElement<?>)o).getDeclaredType() == ExpressionType.class) {
                ExpressionType expression = (ExpressionType) ((JAXBElement<?>)o).getValue();
                String languageIRI = expression.getLanguage();
                languageIRI = languageIRI == null ? FragmentDialectConstants.XPATH10_LANGUAGE_IRI : languageIRI;
                if (languages.containsKey(languageIRI)) {
                    FragmentDialectLanguage language = languages.get(languageIRI);
                    return generateGetResponse(language.getResourceFragment(representation, expression));
                }
                throw new UnsupportedLanguage();
            }
        }
        throw new SoapFault("wsf:Expression is not present.", getSoapVersion().getSender());
    }

    @Override
    public Representation processPut(Put body, Representation representation) {
        for (Object o : body.getAny()) {
            if (o instanceof Fragment) {
                Fragment fragment = (Fragment) o;
                ExpressionType expression = fragment.getExpression();
                ValueType value = fragment.getValue();

                if (expression == null) {
                    throw new SoapFault("wsf:Expression is not present.", getSoapVersion().getSender());
                }
                if (value == null) {
                    value = new ValueType();
                }
                String languageIRI = expression.getLanguage();
                languageIRI = languageIRI == null ? FragmentDialectConstants.XPATH10_LANGUAGE_IRI : languageIRI;
                if (languages.containsKey(languageIRI)) {
                    FragmentDialectLanguage language = languages.get(languageIRI);
                    Object resourceFragment = language.getResourceFragment(representation, expression);
                    String mode = expression.getMode();
                    mode = mode == null ? FragmentDialectConstants.FRAGMENT_MODE_REPLACE : mode;
                    if (resourceFragment == null && FragmentDialectConstants.FRAGMENT_MODE_REPLACE.equals(mode)
                            && FragmentDialectConstants.XPATH10_LANGUAGE_IRI.equals(languageIRI)) {
                        resourceFragment = language.getResourceFragment(representation, getParentXPath(expression));
                        mode = FragmentDialectConstants.FRAGMENT_MODE_ADD;
                    }
                    return modifyRepresentation(resourceFragment, mode, value);
                }
                throw new UnsupportedLanguage();
            }
        }
        throw new SoapFault("wsf:Fragment is not present.", getSoapVersion().getSender());
    }

    @Override
    public boolean processDelete(Delete body, Representation representation) {
        throw new UnknownDialect();
    }

    @Override
    public Representation processCreate(Create body) {
        throw new UnknownDialect();
    }

    /**
     * Register FragmentDialectLanguage object for IRI.
     * @param iri
     * @param language
     */
    public void registerLanguage(String iri, FragmentDialectLanguage language) {
        if (languages.containsKey(iri)) {
            throw new IllegalArgumentException(String.format("IRI \"%s\" is already registered", iri));
        }
        languages.put(iri, language);
    }

    /**
     * Unregister FragmentDialectLanguage object for IRI.
     * @param iri
     */
    public void unregisterLanguage(String iri) {
        if (!languages.containsKey(iri)) {
            throw new IllegalArgumentException(String.format("IRI \"%s\" is not registered", iri));
        }
        languages.remove(iri);
    }

    /**
     * Generates Value element, which is returned as response to Get request.
     * @param value Result of the XPath evaluation.
     * @return
     */
    private JAXBElement<ValueType> generateGetResponse(Object value) {
        if (value instanceof Node) {
            return generateGetResponseNode((Node) value);
        } else if (value instanceof NodeList) {
            return generateGetResponseNodeList((NodeList) value);
        } else if (value instanceof String) {
            return generateGetResponseString((String) value);
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(new ValueType());
    }

    /**
     * Generates Value element from NodeList.
     * @param nodeList
     * @return
     */
    private JAXBElement<ValueType> generateGetResponseNodeList(NodeList nodeList) {
        ValueType resultValue = new ValueType();
        for (int i = 0; i < nodeList.getLength(); i++) {
            resultValue.getContent().add(nodeList.item(i));
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }

    /**
     * Generates Value element from Node.
     * @param node
     * @return
     */
    private JAXBElement<ValueType> generateGetResponseNode(Node node) {
        Document doc = DOMUtils.getEmptyDocument();
        ValueType resultValue = new ValueType();
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            Element attrNodeEl = doc.createElementNS(
                    FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                    FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME);
            attrNodeEl.setAttribute(
                    FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME_ATTR,
                    node.getNodeName()
            );
            attrNodeEl.setTextContent(node.getNodeValue());
            resultValue.getContent().add(attrNodeEl);
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            Element textNodeEl = doc.createElementNS(
                    FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                    FragmentDialectConstants.FRAGMENT_TEXT_NODE_NAME);
            textNodeEl.setNodeValue(node.getNodeValue());
            resultValue.getContent().add(textNodeEl);
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            resultValue.getContent().add(node);
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }

    /**
     * Generates Value element from String.
     * @param value
     * @return
     */
    private JAXBElement<ValueType> generateGetResponseString(String value) {
        ValueType resultValue = new ValueType();
        resultValue.getContent().add(value);
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }

    /**
     * Returns expression containing XPath, which refers to parent element.
     * @param expression
     * @return
     */
    private ExpressionType getParentXPath(ExpressionType expression) {
        String expr;
        if (expression.getContent().size() == 1) {
            expr = (String) expression.getContent().get(0);
        } else {
            throw new InvalidExpression();
        }
        if (badXPathPattern.matcher(expr).find()) {
            throw new InvalidExpression();
        }
        if (goodXPathPattern.matcher(expr).find()) {
            expression.getContent().clear();
            expr = expr.replaceFirst(goodXPathPattern.pattern(), "");
            if (expr.isEmpty()) {
                expr = "/";
            }
            expression.getContent().add(expr);
            return expression;
        }
        throw new InvalidExpression();
    }

    /**
     * Process Put requests.
     * @param resourceFragment Result of the XPath evaluation. It can be Node or NodeList.
     * @param mode Mode defined in the Mode attribute.
     * @param value Value defined in the Value element.
     * @return Representation element, which is returned as response.
     */
    private Representation modifyRepresentation(
            Object resourceFragment,
            String mode,
            ValueType value) {
        if (resourceFragment instanceof Node) {
            List<Node> nodeList = new ArrayList<>();
            nodeList.add((Node) resourceFragment);
            return modifyRepresentationMode(nodeList, mode, value);
        } else if (resourceFragment instanceof NodeList) {
            NodeList rfNodeList = (NodeList) resourceFragment;
            List<Node> nodeList = new ArrayList<>();
            for (int i = 0; i < rfNodeList.getLength(); i++) {
                nodeList.add(rfNodeList.item(i));
            }
            return modifyRepresentationMode(nodeList, mode, value);
        } else {
            throw new InvalidExpression();
        }
    }

    /**
     * Process Put requests.
     * @param mode Mode defined in the Mode attribute.
     * @param value Value defined in the Value element.
     * @return Representation element, which is returned as response.
     */
    private Representation modifyRepresentationMode(
            List<Node> nodeList,
            String mode,
            ValueType value) {
        switch (mode) {
        case FragmentDialectConstants.FRAGMENT_MODE_REPLACE:
            return modifyRepresentationModeReplace(nodeList, value);
        case FragmentDialectConstants.FRAGMENT_MODE_ADD:
            return modifyRepresentationModeAdd(nodeList, value);
        case FragmentDialectConstants.FRAGMENT_MODE_INSERT_BEFORE:
            return modifyRepresentationModeInsertBefore(nodeList, value);
        case FragmentDialectConstants.FRAGMENT_MODE_INSERT_AFTER:
            return modifyRepresentationModeInsertAfter(nodeList, value);
        case FragmentDialectConstants.FRAGMENT_MODE_REMOVE:
            return modifyRepresentationModeRemove(nodeList, value);
        default:
            throw new UnsupportedMode();
        }
    }

    /**
     * Process Put requests for Replace mode.
     * @param value Value defined in the Value element.
     * @return Representation element, which is returned as response.
     */
    private Representation modifyRepresentationModeReplace(
            List<Node> nodeList,
            ValueType value) {

        if (nodeList.isEmpty()) {
            throw new InvalidExpression();
        }
        Node firstNode = nodeList.get(0);
        Document ownerDocument = firstNode.getOwnerDocument();
        // if firstNode.getOwnerDocument == null the firstNode is ownerDocument
        ownerDocument = ownerDocument == null ? (Document) firstNode : ownerDocument;
        Node nextSibling = null;
        Node parent = null;

        for (Node node : nodeList) {
            nextSibling = node.getNextSibling();
            parent = removeNode(node);
        }

        addNode(ownerDocument, parent, nextSibling, value);

        Representation representation = new Representation();
        representation.setAny(ownerDocument.getDocumentElement());
        return representation;
    }

    /**
     * Process Put requests for Add mode.
     * @param value Value defined in the Value element.
     * @return Representation element, which is returned as response.
     */
    private Representation modifyRepresentationModeAdd(
            List<Node> nodeList,
            ValueType value) {
        if (nodeList.isEmpty()) {
            throw new InvalidExpression();
        }
        Node firstNode = nodeList.get(0);
        Document ownerDocument = firstNode.getOwnerDocument();
        // if firstNode.getOwnerDocument == null the firstNode is ownerDocument
        ownerDocument = ownerDocument == null ? (Document) firstNode : ownerDocument;

        for (Node node : nodeList) {
            addNode(ownerDocument, node, null, value);
        }
        Representation representation = new Representation();
        representation.setAny(ownerDocument.getDocumentElement());
        return representation;
    }

    /**
     * Process Put requests for InsertBefore mode.
     * @param value Value defined in the Value element.
     * @return Representation element, which is returned as response.
     */
    private Representation modifyRepresentationModeInsertBefore(
            List<Node> nodeList,
            ValueType value) {
        if (nodeList.isEmpty()) {
            throw new InvalidExpression();
        }
        Node firstNode = nodeList.get(0);
        Document ownerDocument = firstNode.getOwnerDocument();
        // if firstNode.getOwnerDocument == null the firstNode is ownerDocument
        ownerDocument = ownerDocument == null ? (Document) firstNode : ownerDocument;

        Node parent = firstNode.getParentNode();
        if (parent == null && firstNode.getNodeType() != Node.DOCUMENT_NODE) {
            throw new InvalidExpression();
        }
        if (parent == null) {
            parent = firstNode;
            if (((Document) parent).getDocumentElement() != null) {
                throw new InvalidExpression();
            }
        }

        for (Node node : nodeList) {
            if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                throw new InvalidRepresentation();
            }
            insertBefore(ownerDocument, parent, node, value);
        }

        Representation representation = new Representation();
        representation.setAny(ownerDocument.getDocumentElement());
        return representation;
    }

    /**
     * Process Put requests for InsertAfter mode.
     * @param value Value defined in the Value element.
     * @return Representation element, which is returned as response.
     */
    private Representation modifyRepresentationModeInsertAfter(
            List<Node> nodeList,
            ValueType value) {
        if (nodeList.isEmpty()) {
            throw new InvalidExpression();
        }
        Node firstNode = nodeList.get(0);
        Document ownerDocument = firstNode.getOwnerDocument();
        // if firstNode.getOwnerDocument == null the firstNode is ownerDocument
        ownerDocument = ownerDocument == null ? (Document) firstNode : ownerDocument;

        Node parent = firstNode.getParentNode();
        if (parent == null && firstNode.getNodeType() != Node.DOCUMENT_NODE) {
            throw new InvalidExpression();
        }
        if (parent == null) {
            parent = firstNode;
            if (((Document) parent).getDocumentElement() != null) {
                throw new InvalidExpression();
            }
        }

        for (Node node : nodeList) {
            if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                throw new InvalidRepresentation();
            }
            insertAfter(ownerDocument, parent, node, value);
        }

        Representation representation = new Representation();
        representation.setAny(ownerDocument.getDocumentElement());
        return representation;
    }

    /**
     * Process Put requests for Remove mode.
     * @param value Value defined in the Value element.
     * @return Representation element, which is returned as response.
     */
    private Representation modifyRepresentationModeRemove(
            List<Node> nodeList,
            ValueType value) {
        if (nodeList.isEmpty()) {
            throw new InvalidExpression();
        }
        Node firstNode = nodeList.get(0);
        Document ownerDocument = firstNode.getOwnerDocument();
        // if firstNode.getOwnerDocument == null the firstNode is ownerDocument
        ownerDocument = ownerDocument == null ? (Document) firstNode : ownerDocument;

        for (Node node : nodeList) {
            removeNode(node);
        }

        Representation representation = new Representation();
        representation.setAny(ownerDocument.getDocumentElement());
        return representation;
    }

    /**
     * Helper method. It removes Node and returns its parent.
     * @param resourceFragment Node to remove.
     * @return Parent of removed Node.
     */
    private Node removeNode(Node resourceFragment) {
        Node parent;
        if (resourceFragment.getNodeType() == Node.ATTRIBUTE_NODE) {
            parent = ((Attr)resourceFragment).getOwnerElement();
        } else {
            parent = resourceFragment.getParentNode();
        }
        if (parent == null) {
            // resourceFragment is Document Node
            parent = resourceFragment;
        }
        if (resourceFragment.getNodeType() == Node.ATTRIBUTE_NODE) {
            ((Element)parent).removeAttributeNode((Attr)resourceFragment);
        } else {
            if (parent != resourceFragment) {
                parent.removeChild(resourceFragment);
            } else {
                // Both parent and resourceFragment are Document
                Document doc = (Document) parent;
                if (doc.getDocumentElement() != null) {
                    doc.removeChild(doc.getDocumentElement());
                }
            }
        }

        return parent;
    }

    private void insertAfter(Document ownerDocument, Node parent, Node refChild, ValueType value) {
        for (Object o : value.getContent()) {
            if (o instanceof Node) {
                Node node = (Node) o;

                if (
                        FragmentDialectConstants.FRAGMENT_2011_03_IRI.equals(node.getNamespaceURI())
                                && FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME.equals(node.getLocalName())) {
                    throw new InvalidRepresentation();
                }

                Node importedNode = ownerDocument.importNode(node, true);
                if (parent.getNodeType() == Node.DOCUMENT_NODE) {
                    parent.appendChild(importedNode);
                } else {
                    Node nextSibling = refChild.getNextSibling();
                    if (nextSibling == null) {
                        parent.appendChild(importedNode);
                    } else {
                        parent.insertBefore(importedNode, nextSibling);
                    }
                }
            } else {
                throw new InvalidExpression();
            }
        }
    }

    private void insertBefore(Document ownerDocument, Node parent, Node refChild, ValueType value) {
        for (Object o : value.getContent()) {
            if (o instanceof Node) {
                Node node = (Node) o;

                if (
                        FragmentDialectConstants.FRAGMENT_2011_03_IRI.equals(node.getNamespaceURI())
                                && FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME.equals(node.getLocalName())) {
                    throw new InvalidRepresentation();
                }

                Node importedNode = ownerDocument.importNode(node, true);
                if (parent.getNodeType() == Node.DOCUMENT_NODE) {
                    parent.appendChild(importedNode);
                } else {
                    parent.insertBefore(importedNode, refChild);
                }
            } else {
                throw new InvalidExpression();
            }
        }
    }

    /**
     * Helper method. It adds new Node as the last child of parent.
     * @param ownerDocument Document, where the Node is added.
     * @param parent Parent, where the Node is added.
     * @param value Value defined in the Value element. It represents newly added Node.
     */
    private void addNode(Document ownerDocument, Node parent, Node nextSibling, ValueType value) {
        if (ownerDocument == parent && ownerDocument.getDocumentElement() != null) {
            throw new InvalidRepresentation();
        }
        for (Object o : value.getContent()) {
            if (o instanceof String) {
                parent.setTextContent(parent.getTextContent() + ((String) o));
            } else if (o instanceof Node) {
                Node node = (Node) o;
                if (
                        FragmentDialectConstants.FRAGMENT_2011_03_IRI.equals(node.getNamespaceURI())
                                && FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME.equals(node.getLocalName())) {
                    String attrName = ((Element)node).getAttributeNS(
                            FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                            FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME_ATTR);
                    String attrValue = node.getTextContent();
                    if (attrName == null) {
                        throw new SoapFault("wsf:AttributeNode@name is not present.", getSoapVersion().getSender());
                    }
                    if (((Element) parent).hasAttribute(attrName)) {
                        throw new InvalidRepresentation();
                    }
                    ((Element) parent).setAttribute(attrName, attrValue);
                } else {
                    // import the node to the ownerDocument
                    Node importedNode = ownerDocument.importNode((Node) o, true);
                    if (nextSibling == null) {
                        parent.appendChild(importedNode);
                    } else {
                        parent.insertBefore(importedNode, nextSibling);
                    }
                }
            } else {
                throw new InvalidExpression();
            }
        }
    }

    private SoapVersion getSoapVersion() {
        WrappedMessageContext wmc = (WrappedMessageContext) context.getMessageContext();
        SoapMessage message = (SoapMessage) wmc.getWrappedMessage();
        return message.getVersion();
    }
}
