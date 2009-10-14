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

package org.apache.cxf.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;

public final class XMLUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(XMLUtils.class);
    
    private static final Map<ClassLoader, DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORIES
        = Collections.synchronizedMap(new WeakHashMap<ClassLoader, DocumentBuilderFactory>());
    
    private static final Map<ClassLoader, TransformerFactory> TRANSFORMER_FACTORIES
        = Collections.synchronizedMap(new WeakHashMap<ClassLoader, TransformerFactory>());

    private XMLUtils() {
    }

    private static TransformerFactory getTransformerFactory() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = XMLUtils.class.getClassLoader();
        }
        if (loader == null) {
            return TransformerFactory.newInstance();
        }
        TransformerFactory factory = TRANSFORMER_FACTORIES.get(loader);
        if (factory == null) {
            factory = TransformerFactory.newInstance();
            TRANSFORMER_FACTORIES.put(loader, factory);
        }
        return factory;
    }
    private static DocumentBuilderFactory getDocumentBuilderFactory() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = XMLUtils.class.getClassLoader();
        }
        if (loader == null) {
            return DocumentBuilderFactory.newInstance();
        }
        DocumentBuilderFactory factory = DOCUMENT_BUILDER_FACTORIES.get(loader);
        if (factory == null) {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DOCUMENT_BUILDER_FACTORIES.put(loader, factory);
        }
        return factory;
    }
    public static Transformer newTransformer() throws TransformerConfigurationException {
        return getTransformerFactory().newTransformer();
    }

    public static DocumentBuilder getParser() throws ParserConfigurationException {
        return getDocumentBuilderFactory().newDocumentBuilder();
    }

    public static Document parse(InputSource is) throws ParserConfigurationException, SAXException,
        IOException {
        return getParser().parse(is.getSystemId());
    }

    public static Document parse(File is) throws ParserConfigurationException, SAXException,
        IOException {
        return getParser().parse(is);
    }

    public static Document parse(InputStream in) throws ParserConfigurationException, SAXException,
        IOException {
        if (in == null && LOG.isLoggable(Level.FINE)) {
            LOG.fine("XMLUtils trying to parse a null inputstream");
        }
        return getParser().parse(in);
    }

    public static Document parse(String in) throws ParserConfigurationException, SAXException, IOException {
        return parse(in.getBytes());
    }

    public static Document parse(byte[] in) throws ParserConfigurationException, SAXException, IOException {
        if (in == null && LOG.isLoggable(Level.FINE)) {
            LOG.fine("XMLUtils trying to parse a null bytes");
        }
        return getParser().parse(new ByteArrayInputStream(in));
    }

    public static Document newDocument() throws ParserConfigurationException {
        return getParser().newDocument();
    }

    public static void writeTo(Node node, OutputStream os) {
        writeTo(new DOMSource(node), os);
    }
    public static void writeTo(Node node, OutputStream os, int indent) {
        writeTo(new DOMSource(node), os, indent);
    }
    public static void writeTo(Source src, OutputStream os) {
        writeTo(src, os, -1);
    }
    public static void writeTo(Source src, OutputStream os, int indent) {
        String enc = null;
        if (src instanceof DOMSource
            && ((DOMSource)src).getNode() instanceof Document) {
            enc = ((Document)((DOMSource)src).getNode()).getXmlEncoding();
        }
        writeTo(src, os, indent, enc, "no");
    }
    public static void writeTo(Source src,
                               OutputStream os,
                               int indent,
                               String charset,
                               String omitXmlDecl) {
        Transformer it;
        try {
            if (StringUtils.isEmpty(charset)) {
                charset = "utf-8"; 
            }

            it = newTransformer();
            it.setOutputProperty(OutputKeys.METHOD, "xml");
            if (indent > -1) {
                it.setOutputProperty(OutputKeys.INDENT, "yes");
                it.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                                     Integer.toString(indent));
            }
            it.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDecl);
            it.setOutputProperty(OutputKeys.ENCODING, charset);
            it.transform(src, new StreamResult(os));
        } catch (TransformerException e) {
            throw new RuntimeException("Failed to configure TRaX", e);
        }

    }
    public static String toString(Source source) throws TransformerException, IOException {
        return toString(source, null);
    }

    public static String toString(Source source, Properties props) throws TransformerException, IOException {
        StringWriter bos = new StringWriter();
        StreamResult sr = new StreamResult(bos);
        Transformer trans = newTransformer();
        if (props == null) {
            props = new Properties();
            props.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }
        trans.setOutputProperties(props);
        trans.transform(source, sr);
        bos.close();
        return bos.toString();
    }

    public static String toString(Node node, int indent) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTo(node, out, indent);
        return out.toString();
    }
    public static String toString(Node node) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTo(node, out);
        return out.toString();
    }

    public static void printDOM(Node node) {
        printDOM("", node);
    }

    public static void printDOM(String words, Node node) {
        System.out.println(words);
        System.out.println(toString(node));
    }

    public static Attr getAttribute(Element el, String attrName) {
        return el.getAttributeNode(attrName);
    }

    public static void replaceAttribute(Element element, String attr, String value) {
        if (element.hasAttribute(attr)) {
            element.removeAttribute(attr);
        }
        element.setAttribute(attr, value);
    }

    public static boolean hasAttribute(Element element, String value) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            if (value.equals(node.getNodeValue())) {
                return true;
            }
        }
        return false;
    }

    public static void printAttributes(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            System.err.println("## prefix=" + node.getPrefix() + " localname:" + node.getLocalName()
                               + " value=" + node.getNodeValue());
        }
    }

    public static QName getNamespace(Map<String, String> namespaces, String str, String defaultNamespace) {
        String prefix = null;
        String localName = null;

        StringTokenizer tokenizer = new StringTokenizer(str, ":");
        if (tokenizer.countTokens() == 2) {
            prefix = tokenizer.nextToken();
            localName = tokenizer.nextToken();
        } else if (tokenizer.countTokens() == 1) {
            localName = tokenizer.nextToken();
        }

        String namespceURI = defaultNamespace;
        if (prefix != null) {
            namespceURI = (String)namespaces.get(prefix);
        }
        return new QName(namespceURI, localName);
    }

    public static void generateXMLFile(Element element, Writer writer) {
        try {
            Transformer it = newTransformer();

            it.setOutputProperty(OutputKeys.METHOD, "xml");
            it.setOutputProperty(OutputKeys.INDENT, "yes");
            it.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            it.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            it.transform(new DOMSource(element), new StreamResult(writer));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Element createElementNS(Node node, QName name) {
        return createElementNS(node.getOwnerDocument(), name.getNamespaceURI(), name.getLocalPart());
    }

    public static Element createElementNS(Document root, QName name) {
        return createElementNS(root, name.getNamespaceURI(), name.getLocalPart());
    }

    public static Element createElementNS(Document root, String namespaceURI, String qualifiedName) {
        return root.createElementNS(namespaceURI, qualifiedName);
    }

    public static Text createTextNode(Document root, String data) {
        return root.createTextNode(data);
    }

    public static Text createTextNode(Node node, String data) {
        return createTextNode(node.getOwnerDocument(), data);
    }

    public static void removeContents(Node parent) {     
        Node node = parent.getFirstChild();
        while (node != null) {
            parent.removeChild(node);
            node = node.getNextSibling();
        }
    }

    public static String writeQName(Definition def, QName qname) {
        return def.getPrefix(qname.getNamespaceURI()) + ":" + qname.getLocalPart();
    }

    public static InputStream getInputStream(Document doc) throws Exception {
        DOMImplementationLS impl = null;
        DOMImplementation docImpl = doc.getImplementation();
        // Try to get the DOMImplementation from doc first before
        // defaulting to the sun implementation.
        if (docImpl != null && docImpl.hasFeature("LS", "3.0")) {
            impl = (DOMImplementationLS)docImpl.getFeature("LS", "3.0");
        } else {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
            if (impl == null) {
                System.setProperty(DOMImplementationRegistry.PROPERTY,
                                   "com.sun.org.apache.xerces.internal.dom.DOMImplementationSourceImpl");
                registry = DOMImplementationRegistry.newInstance();
                impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
            }
        }
        LSOutput output = impl.createLSOutput();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        output.setByteStream(byteArrayOutputStream);
        LSSerializer writer = impl.createLSSerializer();
        writer.write(doc, output);
        byte[] buf = byteArrayOutputStream.toByteArray();
        return new ByteArrayInputStream(buf);
    }

    public static Element fetchElementByNameAttribute(Element parent, String targetName, String nameValue) {
        
        List<Element> elemList = DOMUtils.findAllElementsByTagName(parent, targetName);
        for (Element elem : elemList) {
            if (elem.getAttribute("name").equals(nameValue)) {
                return elem;
            }
        }
        return null;
    }

    public static QName getQName(String value, Node node) {
        if (value == null) {
            return null;
        }

        int index = value.indexOf(":");

        if (index == -1) {
            return new QName(value);
        }

        String prefix = value.substring(0, index);
        String localName = value.substring(index + 1);
        String ns = node.lookupNamespaceURI(prefix);

        if (ns == null || localName == null) {
            throw new RuntimeException("Invalid QName in mapping: " + value);
        }

        return new QName(ns, localName, prefix);
    }

    public static Node  fromSource(Source src) throws Exception {

        Transformer trans = TransformerFactory.newInstance().newTransformer();
        DOMResult res = new DOMResult();
        trans.transform(src, res);
        return res.getNode();
    }
}
