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
package org.apache.cxf.wsn.util;

import java.io.StringWriter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A collection of W3C DOM helper methods.
 * </p>
 *
 * @version $Revision: 564607 $
 */
public final class DOMUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DOMUtil.class);

    private static DocumentBuilderFactory dbf;
    private static Queue<DocumentBuilder> builders = new ConcurrentLinkedQueue<DocumentBuilder>();


    private DOMUtil() {
    }

    /**
     * <p>
     * Returns the text of the element.
     * </p>
     *
     * @param element the element.
     * @return the element text value.
     */
    public static String getElementText(Element element) {
        StringBuffer buffer = new StringBuffer();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                buffer.append(node.getNodeValue());
            }
        }
        return buffer.toString();
    }

    /**
     * <p>
     * Moves the content of the given element to the given element.
     * </p>
     *
     * @param from the source element.
     * @param to the destination element.
     */
    public static void moveContent(Element from, Element to) {
        // lets move the child nodes across
        NodeList childNodes = from.getChildNodes();
        while (childNodes.getLength() > 0) {
            Node node = childNodes.item(0);
            from.removeChild(node);
            to.appendChild(node);
        }
    }

    /**
     * <p>
     * Copy the attribues on one element to the other.
     * </p>
     *
     * @param from the source element.
     * @param to the destination element.
     */
    public static void copyAttributes(Element from, Element to) {
        // lets copy across all the remainingattributes
        NamedNodeMap attributes = from.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr node = (Attr) attributes.item(i);
            to.setAttributeNS(node.getNamespaceURI(), node.getName(), node.getValue());
        }
    }

    /**
     * <p>
     * A helper method useful for debugging and logging which will convert the given DOM node into XML text.
     * </p>
     *
     * @param node the node.
     * @return a raw XML string representing the node.
     */
    public static String asXML(Node node) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter buffer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(buffer));
        return buffer.toString();
    }

    /**
     * <p>
     * A helper method useful for debugging and logging which will convert the given DOM node into XML text.
     * </p>
     *
     * @param node the node.
     * @return a indented XML string representing the node.
     */
    public static String asIndentedXML(Node node) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter buffer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(buffer));
        return buffer.toString();
    }

    /**
     * <p>
     * Adds the child element with the given text.
     * </p>
     *
     * @param element the element where to add child.
     * @param name the child elenemt name.
     * @param textValue the child element text value.
     */
    public static void addChildElement(Element element, String name, Object textValue) {
        Document document = element.getOwnerDocument();
        Element child = document.createElement(name);
        element.appendChild(child);
        if (textValue != null) {
            String text = textValue.toString();
            child.appendChild(document.createTextNode(text));
        }
    }

    /**
     * <p>
     * Creates a QName instance from the given namespace context for the given qualifiedName.
     * </p>
     *
     * @param element the element to use as the namespace context.
     * @param qualifiedName the fully qualified name.
     * @return the QName which matches the qualifiedName.
     */
    public static QName createQName(Element element, String qualifiedName) {
        int index = qualifiedName.indexOf(':');
        if (index >= 0) {
            String prefix = qualifiedName.substring(0, index);
            String localName = qualifiedName.substring(index + 1);
            String uri = recursiveGetAttributeValue(element, "xmlns:" + prefix);
            return new QName(uri, localName, prefix);
        } else {
            String uri = recursiveGetAttributeValue(element, "xmlns");
            if (uri != null) {
                return new QName(uri, qualifiedName);
            }
            return new QName(qualifiedName);
        }
    }

    /**
     * <p>
     * Recursive method to find a given attribute value.
     * </p>
     *
     * @param element the element where to looking for attribute.
     * @param attributeName the attribute name to look for.
     * @return the value of the given attribute.
     */
    public static String recursiveGetAttributeValue(Element element, String attributeName) {
        String answer = null;
        try {
            answer = element.getAttribute(attributeName);
        } catch (Exception e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Caught exception looking up attribute: " + attributeName 
                             + " on element: " + element + ". Cause: " + e, e);
            }
        }
        if (answer == null || answer.length() == 0) {
            Node parentNode = element.getParentNode();
            if (parentNode instanceof Element) {
                return recursiveGetAttributeValue((Element) parentNode, attributeName);
            }
        }
        return answer;
    }

    /**
     * <p>
     * Gets the first child element.
     * </p>
     *
     * @param parent the parent node.
     * @return the first child element.
     */
    public static Element getFirstChildElement(Node parent) {
        NodeList childs = parent.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);
            if (child instanceof Element) {
                return (Element) child;
            }
        }
        return null;
    }

    /**
     * <p>
     * Gets the next sibling element.
     * </p>
     *
     * @param el the base element.
     * @return the next sibling element.
     */
    public static Element getNextSiblingElement(Element el) {
        for (Node n = el.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * <p>
     * Builds a QName from the element name.
     * </p>
     *
     * @param el the element.
     * @return the QName for the given element.
     */
    public static QName getQName(Element el) {
        if (el == null) {
            return null;
        } else if (el.getPrefix() != null) {
            return new QName(el.getNamespaceURI(), el.getLocalName(), el.getPrefix());
        } else {
            return new QName(el.getNamespaceURI(), el.getLocalName());
        }
    }

    public static DocumentBuilder getBuilder() throws ParserConfigurationException {
        DocumentBuilder builder = (DocumentBuilder) builders.poll();
        if (builder == null) {
            if (dbf == null) {
                dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
            }
            builder = dbf.newDocumentBuilder();
        }
        return builder;
    }

    public static void releaseBuilder(DocumentBuilder builder) {
        if (builder != null) {
            builders.add(builder);
        }
    }

    /**
     * <p>
     * Returns a new document, ready to populate.
     * </p>
     *
     * @return a ready to use Document.
     */
    public static Document newDocument() throws ParserConfigurationException {
        DocumentBuilder builder = getBuilder();
        Document doc = builder.newDocument();
        releaseBuilder(builder);
        return doc;
    }

}
