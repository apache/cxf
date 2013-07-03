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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.xml.sax.InputSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;

public final class XMLUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(XMLUtils.class);
    
    private static final Map<ClassLoader, DocumentBuilder> DOCUMENT_BUILDERS
        = Collections.synchronizedMap(new WeakHashMap<ClassLoader, DocumentBuilder>());
    
    private static final Pattern XML_ESCAPE_CHARS = Pattern.compile("[\"'&<>]");
    private static final Map<String, String> XML_ENCODING_TABLE;
    static {
        XML_ENCODING_TABLE = new HashMap<String, String>();
        XML_ENCODING_TABLE.put("\"", "&quot;");
        XML_ENCODING_TABLE.put("'", "&apos;");
        XML_ENCODING_TABLE.put("<", "&lt;");
        XML_ENCODING_TABLE.put(">", "&gt;");
        XML_ENCODING_TABLE.put("&", "&amp;");
    }
    
    private XMLUtils() {
    }

    private static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = XMLUtils.class.getClassLoader();
        }
        if (loader == null) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        DocumentBuilder factory = DOCUMENT_BUILDERS.get(loader);
        if (factory == null) {
            DocumentBuilderFactory f2 = DocumentBuilderFactory.newInstance();
            f2.setNamespaceAware(true);
            factory = f2.newDocumentBuilder();
            DOCUMENT_BUILDERS.put(loader, factory);
        }
        return factory;
    }


    public static Document parse(InputSource is) throws XMLStreamException {
        return StaxUtils.read(is);
    }

    public static Document parse(File is) throws XMLStreamException, IOException {
        InputStream fin = new FileInputStream(is);
        try {
            return StaxUtils.read(fin);
        } finally {
            fin.close();
        }
    }

    public static Document parse(InputStream in) throws XMLStreamException {
        if (in == null && LOG.isLoggable(Level.FINE)) {
            LOG.fine("XMLUtils trying to parse a null inputstream");
        }
        return StaxUtils.read(in);
    }

    public static Document parse(String in) throws XMLStreamException {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new StringReader(in));
        try {
            return StaxUtils.read(reader);
        } finally {
            reader.close();
        }
    }

    public static Document parse(byte[] in) throws XMLStreamException {
        if (in == null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("XMLUtils trying to parse a null bytes");
            }
            return null;
        }
        return StaxUtils.read(new ByteArrayInputStream(in));
    }

    public static Document newDocument() throws ParserConfigurationException {
        return getDocumentBuilder().newDocument();
    }

    public static void writeTo(Node node, OutputStream os) throws XMLStreamException {
        writeTo(new DOMSource(node), os);
    }
    public static void writeTo(Node node, OutputStream os, int indent) throws XMLStreamException {
        writeTo(new DOMSource(node), os, indent);
    }
    public static void writeTo(Source src, OutputStream os) throws XMLStreamException {
        writeTo(src, os, -1);
    }
    public static void writeTo(Node node, Writer os) throws XMLStreamException {
        writeTo(new DOMSource(node), os);
    }
    public static void writeTo(Node node, Writer os, int indent) throws XMLStreamException {
        writeTo(new DOMSource(node), os, indent);
    }
    public static void writeTo(Source src, Writer os) throws XMLStreamException {
        writeTo(src, os, -1);
    }
    public static void writeTo(Source src, OutputStream os, int indent) throws XMLStreamException {
        String enc = null;
        if (src instanceof DOMSource
            && ((DOMSource)src).getNode() instanceof Document) {
            try {
                enc = ((Document)((DOMSource)src).getNode()).getXmlEncoding();
            } catch (Exception ex) {
                //ignore - not DOM level 3
            }
        }
        writeTo(src, os, indent, enc, false);
    }
    public static void writeTo(Source src, Writer os, int indent) throws XMLStreamException {
        String enc = null;
        if (src instanceof DOMSource
            && ((DOMSource)src).getNode() instanceof Document) {
            try {
                enc = ((Document)((DOMSource)src).getNode()).getXmlEncoding();
            } catch (Exception ex) {
                //ignore - not DOM level 3
            }
        }
        writeTo(src, os, indent, enc, false);
    }
    public static void writeTo(Source src,
                               OutputStream os,
                               int indent,
                               String charset,
                               boolean omitXmlDecl) throws XMLStreamException {
        
        if (StringUtils.isEmpty(charset)) {
            charset = "utf-8"; 
        }
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, charset);
        if (indent > 0) {
            writer = new PrettyPrintXMLStreamWriter(writer, 0, indent);
        }
        if (!omitXmlDecl) {
            writer.writeStartDocument(charset, "1.0");
        }
        StaxUtils.copy(src, writer);
        if (!omitXmlDecl) {
            writer.writeEndDocument();
        }
        writer.close();
    }
    public static void writeTo(Source src,
                               Writer os,
                               int indent,
                               String charset,
                               boolean omitXmlDecl) throws XMLStreamException {
        if (StringUtils.isEmpty(charset)) {
            charset = "utf-8"; 
        }
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os);
        if (indent > 0) {
            writer = new PrettyPrintXMLStreamWriter(writer, 0, indent);
        }
        if (!omitXmlDecl) {
            writer.writeStartDocument(charset, "1.0");
        }
        StaxUtils.copy(src, writer);
        if (!omitXmlDecl) {
            writer.writeEndDocument();
        }
        writer.close();
    }
    
    
    public static String toString(Source source) throws TransformerException, IOException {
        StringWriter out = new StringWriter();
        try {
            writeTo(source, out, 0, "utf-8", true);
        } catch (XMLStreamException ex) {
            throw new RuntimeException(ex);
        }
        return out.toString();
    }

    public static String toString(Node node, int indent) {
        StringWriter out = new StringWriter();
        try {
            writeTo(node, out, indent);
        } catch (XMLStreamException ex) {
            throw new RuntimeException(ex);
        }
        return out.toString();
    }
    public static String toString(Node node) {
        StringWriter out = new StringWriter();
        try {
            writeTo(node, out);
        } catch (XMLStreamException ex) {
            throw new RuntimeException(ex);
        }
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
            namespceURI = namespaces.get(prefix);
        }
        return new QName(namespceURI, localName);
    }

    public static void generateXMLFile(Element element, Writer writer) throws XMLStreamException {
        writeTo(new DOMSource(element), writer, 2, "UTF-8", false);
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

    public static InputStream getInputStream(Document doc) throws Exception {
        LoadingByteArrayOutputStream out = new LoadingByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out, "UTF-8");
        StaxUtils.writeDocument(doc, writer, true);
        writer.close();
        return out.createInputStream();
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

    public static Node fromSource(Source src) throws Exception {
        return StaxUtils.read(src);
    }
    
    public static QName convertStringToQName(String expandedQName) {
        return convertStringToQName(expandedQName, "");
    }
    
    public static QName convertStringToQName(String expandedQName, String prefix) {
        int ind1 = expandedQName.indexOf('{');
        if (ind1 != 0) {
            return new QName(expandedQName);
        }
        
        int ind2 = expandedQName.indexOf('}');
        if (ind2 <= ind1 + 1 || ind2 >= expandedQName.length() - 1) {
            return null;
        }
        String ns = expandedQName.substring(ind1 + 1, ind2);
        String localName = expandedQName.substring(ind2 + 1);
        return new QName(ns, localName, prefix);
    }
    
    public static Set<QName> convertStringsToQNames(List<String> expandedQNames) {
        Set<QName> dropElements = Collections.emptySet();
        if (expandedQNames != null) {
            dropElements = new LinkedHashSet<QName>(expandedQNames.size());
            for (String val : expandedQNames) {
                dropElements.add(XMLUtils.convertStringToQName(val));
            }
        }
        return dropElements;
    }
    
    public static String xmlEncode(String value) {
        Matcher m = XML_ESCAPE_CHARS.matcher(value);
        boolean match = m.find();
        if (match) {
            int i = 0;
            StringBuilder sb = new StringBuilder();
            do {
                String replacement = XML_ENCODING_TABLE.get(m.group());
                sb.append(value.substring(i, m.start()));
                sb.append(replacement);
                i = m.end();
            } while (m.find());
            sb.append(value.substring(i, value.length()));
            return sb.toString();
        } else {
            return value;
        }
    }
}
