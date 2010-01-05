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

package org.apache.cxf.staxutils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.Location;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;

public final class StaxUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(StaxUtils.class);
    
    private static final BlockingQueue<XMLInputFactory> NS_AWARE_INPUT_FACTORY_POOL;
    private static final BlockingQueue<XMLOutputFactory> OUTPUT_FACTORY_POOL;
    
    private static final String XML_NS = "http://www.w3.org/2000/xmlns/";
    private static final String DEF_PREFIXES[] = new String[] {
        "ns1".intern(), "ns2".intern(), "ns3".intern(),
        "ns4".intern(), "ns5".intern(), "ns6".intern(),
        "ns7".intern(), "ns8".intern(), "ns9".intern()
    };
    
    static {
        int i = 20;
    
        try {
            String s = System.getProperty("org.apache.cxf.staxutils.pool-size",
                                          "-1");
            i = Integer.parseInt(s);
        } catch (Throwable t) {
            //ignore 
            i = 20;
        }
        if (i <= 0) {
            i = 20;
        }
        NS_AWARE_INPUT_FACTORY_POOL = new LinkedBlockingQueue<XMLInputFactory>(i);
        OUTPUT_FACTORY_POOL = new LinkedBlockingQueue<XMLOutputFactory>(i);
    }
    
    private StaxUtils() {
    }

    /**
     * CXF works with multiple STaX parsers. When we can't find any other way to work 
     * against the different parsers, this can be used to condition code. Note: if you've got
     * Woodstox in the class path without being the default provider, this will return
     * the wrong answer.
     * @return true if Woodstox is in the classpath. 
     */
    public static boolean isWoodstox() {
        try {
            ClassLoaderUtils.loadClass("org.codehaus.stax2.XMLStreamReader2", StaxUtils.class);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }
    
    /**
     * Return a cached, namespace-aware, factory.
     * @return
     */
    private static XMLInputFactory getXMLInputFactory() {
        XMLInputFactory f = NS_AWARE_INPUT_FACTORY_POOL.poll();
        if (f == null) {
            f = XMLInputFactory.newInstance();
            f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        }
        return f;
    }
    
    private static void returnXMLInputFactory(XMLInputFactory factory) {
        NS_AWARE_INPUT_FACTORY_POOL.offer(factory);
    }
    
    private static XMLOutputFactory getXMLOutputFactory() {
        XMLOutputFactory f = OUTPUT_FACTORY_POOL.poll();
        if (f == null) {
            f = XMLOutputFactory.newInstance();
        }
        return f;
    }
    
    private static void returnXMLOutputFactory(XMLOutputFactory factory) {
        OUTPUT_FACTORY_POOL.offer(factory);
    }
    
    /**
     * Return a new factory so that the caller can set sticky parameters.
     * @param nsAware
     * @return
     */
    public static XMLInputFactory createXMLInputFactory(boolean nsAware) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, nsAware);
        return factory;
    }

    

    public static XMLStreamWriter createXMLStreamWriter(Writer out) {
        XMLOutputFactory factory = getXMLOutputFactory();
        try {
            return factory.createXMLStreamWriter(out);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Cant' create XMLStreamWriter", e);
        } finally {
            returnXMLOutputFactory(factory);
        }
    } 
    
    public static XMLStreamWriter createXMLStreamWriter(OutputStream out) {
        return createXMLStreamWriter(out, null);
    }

    public static XMLStreamWriter createXMLStreamWriter(OutputStream out, String encoding) {
        if (encoding == null) {
            encoding = "UTF-8";
        }
        XMLOutputFactory factory = getXMLOutputFactory();
        try {
            return factory.createXMLStreamWriter(out, encoding);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Cant' create XMLStreamWriter", e);
        } finally {
            returnXMLOutputFactory(factory);
        }
    }
    
    public static XMLStreamWriter createXMLStreamWriter(Result r) {
        XMLOutputFactory factory = getXMLOutputFactory();
        try {
            return factory.createXMLStreamWriter(r);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Cant' create XMLStreamWriter", e);
        } finally {
            returnXMLOutputFactory(factory);
        }
    }

    public static XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter) {
        XMLInputFactory factory = getXMLInputFactory();
        try {
            return factory.createFilteredReader(reader, filter);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Cant' create XMLStreamReader", e);
        } finally {
            returnXMLInputFactory(factory);
        }
    }

    
    public static void nextEvent(XMLStreamReader dr) {
        try {
            dr.next();
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        }
    }

    public static boolean toNextText(DepthXMLStreamReader reader) {
        if (reader.getEventType() == XMLStreamReader.CHARACTERS) {
            return true;
        }

        try {
            int depth = reader.getDepth();
            int event = reader.getEventType();
            while (reader.getDepth() >= depth && reader.hasNext()) {
                if (event == XMLStreamReader.CHARACTERS && reader.getDepth() == depth + 1) {
                    return true;
                }
                event = reader.next();
            }
            return false;
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        }
    }
    public static boolean toNextTag(XMLStreamReader reader) {
        try {
            // advance to first tag.
            int x = reader.getEventType();
            while (x != XMLStreamReader.START_ELEMENT
                && x != XMLStreamReader.END_ELEMENT
                && reader.hasNext()) {
                x = reader.next();
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        }
        return true;
    }

    public static boolean toNextTag(DepthXMLStreamReader reader, QName endTag) {
        try {
            int depth = reader.getDepth();
            int event = reader.getEventType();
            while (reader.getDepth() >= depth && reader.hasNext()) {
                if (event == XMLStreamReader.START_ELEMENT && reader.getName().equals(endTag) 
                    && reader.getDepth() == depth + 1) {
                    return true;
                }
                event = reader.next();
            }
            return false;
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        }
    }    
    
    public static void writeStartElement(XMLStreamWriter writer, String prefix, String name, String namespace)
        throws XMLStreamException {
        if (prefix == null) {
            prefix = "";
        }

        if (namespace.length() > 0) {
            writer.writeStartElement(prefix, name, namespace);
            if (prefix.length() > 0) {
                writer.writeNamespace(prefix, namespace);
                writer.setPrefix(prefix, namespace);
            } else {
                writer.writeDefaultNamespace(namespace);
                writer.setDefaultNamespace(namespace);
            }
        } else {
            writer.writeStartElement(name);
            writer.writeDefaultNamespace("");
            writer.setDefaultNamespace("");
        }
    }

    /**
     * Returns true if currently at the start of an element, otherwise move
     * forwards to the next element start and return true, otherwise false is
     * returned if the end of the stream is reached.
     */
    public static boolean skipToStartOfElement(XMLStreamReader in) throws XMLStreamException {
        for (int code = in.getEventType(); code != XMLStreamReader.END_DOCUMENT; code = in.next()) {
            if (code == XMLStreamReader.START_ELEMENT) {
                return true;
            }
        }
        return false;
    }

    public static boolean toNextElement(DepthXMLStreamReader dr) {
        if (dr.getEventType() == XMLStreamReader.START_ELEMENT) {
            return true;
        }
        if (dr.getEventType() == XMLStreamReader.END_ELEMENT) {
            return false;
        }
        try {
            int depth = dr.getDepth();

            for (int event = dr.getEventType(); dr.getDepth() >= depth && dr.hasNext(); event = dr.next()) {
                if (event == XMLStreamReader.START_ELEMENT && dr.getDepth() == depth + 1) {
                    return true;
                } else if (event == XMLStreamReader.END_ELEMENT) {
                    depth--;
                }
            }

            return false;
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        }
    }

    public static boolean skipToStartOfElement(DepthXMLStreamReader in) throws XMLStreamException {
        for (int code = in.getEventType(); code != XMLStreamReader.END_DOCUMENT; code = in.next()) {
            if (code == XMLStreamReader.START_ELEMENT) {
                return true;
            }
        }
        return false;
    }
    public static void copy(Source source, XMLStreamWriter writer) throws XMLStreamException {
        if (source instanceof SAXSource) {
            InputSource src = ((SAXSource)source).getInputSource();
            if (src.getSystemId() == null && src.getPublicId() == null
                && ((SAXSource)source).getXMLReader() != null) {
                
                //OK - reader is OK.  We'll use that out
                StreamWriterContentHandler ch = new StreamWriterContentHandler(writer);
                XMLReader reader = ((SAXSource)source).getXMLReader();
                reader.setContentHandler(ch);
                try {
                    try {
                        reader.setFeature("http://xml.org/sax/features/namespaces", true);
                    } catch (Throwable t) {
                        //ignore
                    }
                    reader.parse(((SAXSource)source).getInputSource());
                    return;
                } catch (Exception e) {
                    throw new XMLStreamException(e);
                }
            }
       
        }
        
        XMLStreamReader reader = createXMLStreamReader(source);
        copy(reader, writer);
        reader.close();
    }

    public static Document copy(Document doc) 
        throws XMLStreamException, ParserConfigurationException {
        
        XMLStreamReader reader = createXMLStreamReader(doc);
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        copy(reader, writer);
        Document d = writer.getDocument();
        try {
            d.setDocumentURI(doc.getDocumentURI());
        } catch (Exception ex) {
            //ignore - probably not DOM level 3
        }
        return d;
    }
    public static void copy(Document doc, XMLStreamWriter writer) throws XMLStreamException {
        XMLStreamReader reader = createXMLStreamReader(doc);
        copy(reader, writer);
    }
    public static void copy(Element node, XMLStreamWriter writer) throws XMLStreamException {
        XMLStreamReader reader = createXMLStreamReader(node);
        copy(reader, writer);
    }
    
    /**
     * Copies the reader to the writer. The start and end document methods must
     * be handled on the writer manually. TODO: if the namespace on the reader
     * has been declared previously to where we are in the stream, this probably
     * won't work.
     * 
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    public static void copy(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        copy(reader, writer, false);
    }
    public static void copy(XMLStreamReader reader, XMLStreamWriter writer,
                            boolean fragment) throws XMLStreamException {
        // number of elements read in
        int read = 0;
        int event = reader.getEventType();

        while (reader.hasNext()) {
            switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                read++;
                writeStartElement(reader, writer);
                break;
            case XMLStreamConstants.END_ELEMENT:
                writer.writeEndElement();
                read--;
                if (read <= 0 && !fragment) {
                    return;
                }
                break;
            case XMLStreamConstants.CHARACTERS:
                writer.writeCharacters(reader.getText());
                break;
            case XMLStreamConstants.COMMENT:
                writer.writeComment(reader.getText());
                break;
            case XMLStreamConstants.CDATA:
                writer.writeCData(reader.getText());
                break;
            case XMLStreamConstants.START_DOCUMENT:
            case XMLStreamConstants.END_DOCUMENT:
            case XMLStreamConstants.ATTRIBUTE:
            case XMLStreamConstants.NAMESPACE:
                break;
            default:
                break;
            }
            event = reader.next();
        }
    }

    private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer)
        throws XMLStreamException {
        String local = reader.getLocalName();
        String uri = reader.getNamespaceURI();
        String prefix = reader.getPrefix();
        if (prefix == null) {
            prefix = "";
        }

        
//        System.out.println("STAXUTILS:writeStartElement : node name : " + local +  " namespace URI" + uri);
        boolean writeElementNS = false;
        
     // Write out the element name
        if (uri != null) {
            if (prefix.length() == 0 && StringUtils.isEmpty(uri)) {
                writer.writeStartElement(local);
            } else {
                writer.writeStartElement(prefix, local, uri);
            }
        } else {
            writer.writeStartElement(local);
        }

        
        if (uri != null) {
            writeElementNS = true;
            Iterator<String> it = CastUtils.cast(writer.getNamespaceContext().getPrefixes(uri));
            while (it != null && it.hasNext()) {
                String s = it.next();
                if (s == null) {
                    s = "";
                }
                if (s.equals(prefix)) {
                    writeElementNS = false;
                }
            }
        }

        // Write out the namespaces
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String nsURI = reader.getNamespaceURI(i);
            String nsPrefix = reader.getNamespacePrefix(i);
            if (nsPrefix == null) {
                nsPrefix = "";
            }

            if (nsPrefix.length() == 0) {
                writer.writeDefaultNamespace(nsURI);
                writer.setDefaultNamespace(nsURI);
            } else {
                writer.writeNamespace(nsPrefix, nsURI);
                writer.setPrefix(nsPrefix, nsURI);
            }

            if (nsURI.equals(uri) && nsPrefix.equals(prefix)) {
                writeElementNS = false;
            }
        }

        // Check if the namespace still needs to be written.
        // We need this check because namespace writing works
        // different on Woodstox and the RI.
        if (writeElementNS) {
            if (prefix == null || prefix.length() == 0) {
                writer.writeDefaultNamespace(uri);
                writer.setDefaultNamespace(uri);
            } else {
                writer.writeNamespace(prefix, uri);
                writer.setPrefix(prefix, uri);
            }
        }        
        
        // Write out attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String ns = reader.getAttributeNamespace(i);
            String nsPrefix = reader.getAttributePrefix(i);
            if (ns == null || ns.length() == 0) {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else if (nsPrefix == null || nsPrefix.length() == 0) {
                writer.writeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                                      reader.getAttributeValue(i));
            } else {
                Iterator<String> it = CastUtils.cast(writer.getNamespaceContext().getPrefixes(ns));
                boolean writeNs = true;
                while (it != null && it.hasNext()) {
                    String s = it.next();
                    if (s == null) {
                        s = "";
                    }
                    if (s.equals(nsPrefix)) {
                        writeNs = false;
                    }
                }
                if (writeNs) {
                    writer.writeNamespace(nsPrefix, ns);
                    writer.setPrefix(nsPrefix, ns);
                }
                writer.writeAttribute(reader.getAttributePrefix(i), reader.getAttributeNamespace(i), reader
                    .getAttributeLocalName(i), reader.getAttributeValue(i));
            }

        }
    }

    public static void writeDocument(Document d, XMLStreamWriter writer, boolean repairing)
        throws XMLStreamException {
        writeDocument(d, writer, true, repairing);
    }

    public static void writeDocument(Document d, XMLStreamWriter writer, boolean writeProlog,
                                     boolean repairing) throws XMLStreamException {
        if (writeProlog) {
            writer.writeStartDocument();
        }
        
        Node node = d.getFirstChild();
        while (node != null) {
            if (writeProlog || node.getNodeType() == Node.ELEMENT_NODE) {
                writeNode(node, writer, repairing);
            }
            node = node.getNextSibling();
        }
        
        if (writeProlog) {
            writer.writeEndDocument();
        }
    }

    /**
     * Writes an Element to an XMLStreamWriter. The writer must already have
     * started the document (via writeStartDocument()). Also, this probably
     * won't work with just a fragment of a document. The Element should be the
     * root element of the document.
     * 
     * @param e
     * @param writer
     * @throws XMLStreamException
     */
    public static void writeElement(Element e, XMLStreamWriter writer, boolean repairing) 
        throws XMLStreamException {
        writeElement(e, writer, repairing, true);
    }

    /**
     * Writes an Element to an XMLStreamWriter. The writer must already have
     * started the document (via writeStartDocument()). Also, this probably
     * won't work with just a fragment of a document. The Element should be the
     * root element of the document.
     * 
     * @param e
     * @param writer
     * @param endElement true if the element should be ended
     * @throws XMLStreamException
     */
    public static void writeElement(Element e,
                                    XMLStreamWriter writer,
                                    boolean repairing,
                                    boolean endElement)
        throws XMLStreamException {
        String prefix = e.getPrefix();
        String ns = e.getNamespaceURI();
        String localName = e.getLocalName();

       
//        System.out.println("local name : " + localName + " URI: " + ns + " Prefix :" + prefix);
        if (prefix == null) {
            prefix = "";
        }
        if (localName == null) {
            localName = e.getNodeName();

            if (localName == null) {
                throw new IllegalStateException("Element's local name cannot be null!");
            }
        }

        String decUri = writer.getNamespaceContext().getNamespaceURI(prefix);
        boolean declareNamespace = decUri == null || !decUri.equals(ns);

        if (ns == null || ns.length() == 0) {
            writer.writeStartElement(localName);
            if (StringUtils.isEmpty(decUri)) {
                declareNamespace = false;
            }
        } else {
//            System.out.println("Calling writeStartElement for local name : " 
//            + localName + " URI: " + ns + " Prefix :" + prefix);
            writer.writeStartElement(prefix, localName, ns);
        }

        NamedNodeMap attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);

            String name = attr.getLocalName();
            String attrPrefix = attr.getPrefix();
            if (attrPrefix == null) {
                attrPrefix = "";
            }
            if (name == null) {
                name = attr.getNodeName();
            }
     
            if ("xmlns".equals(attrPrefix)) {
//                System.out.println("WriteNamespace is called for prefix : " 
//                + name + " namespace :" + attr.getNodeValue());
                writer.writeNamespace(name, attr.getNodeValue());
                writer.setPrefix(name, attr.getNodeValue());
                if (name.equals(prefix) && attr.getNodeValue().equals(ns)) {
                    declareNamespace = false;
                }
            } else {
                if ("xmlns".equals(name) && "".equals(attrPrefix)) {
                    writer.writeDefaultNamespace(attr.getNodeValue());
                    writer.setDefaultNamespace(attr.getNodeValue());
                    if (attr.getNodeValue().equals(ns)) {
                        declareNamespace = false;
                    } else if (StringUtils.isEmpty(attr.getNodeValue())
                        && StringUtils.isEmpty(ns)) {
                        declareNamespace = false;
                    }
                } else {
                    String attns = attr.getNamespaceURI();
                    String value = attr.getNodeValue();
                    if (attns == null || attns.length() == 0) {
                        writer.writeAttribute(name, value);
                    } else if (attrPrefix == null || attrPrefix.length() == 0) {
                        writer.writeAttribute(attns, name, value);
                    } else {
                        writer.writeAttribute(attrPrefix, attns, name, value);
                    }                    
                }
            }
        }

        if (declareNamespace && repairing) {
            if (ns == null) {
                writer.writeNamespace(prefix, "");
                writer.setPrefix(prefix, "");
            } else {
                writer.writeNamespace(prefix, ns);
                writer.setPrefix(prefix, ns);
            }
        }

        Node nd = e.getFirstChild();
        while (nd != null) {
            writeNode(nd, writer, repairing);
            nd = nd.getNextSibling();
        }       

        if (endElement) {
            writer.writeEndElement();
        }
    }

    public static void writeNode(Node n, XMLStreamWriter writer, boolean repairing) 
        throws XMLStreamException {
        
        switch (n.getNodeType()) {
        case Node.ELEMENT_NODE:
            writeElement((Element)n, writer, repairing);
            break;
        case Node.TEXT_NODE:
            writer.writeCharacters(((Text)n).getNodeValue());
            break;
        case Node.COMMENT_NODE:
            writer.writeComment(((Comment)n).getData());
            break;
        case Node.CDATA_SECTION_NODE:
            writer.writeCData(((CDATASection)n).getData());
            break;
        case Node.ENTITY_REFERENCE_NODE:
            writer.writeEntityRef(((EntityReference)n).getNodeValue());
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            ProcessingInstruction pi = (ProcessingInstruction)n;
            writer.writeProcessingInstruction(pi.getTarget(), pi.getData());
            break;
        case Node.DOCUMENT_NODE:
            writeDocument((Document)n, writer, repairing);
            break;
        case Node.DOCUMENT_FRAGMENT_NODE: {
            DocumentFragment frag = (DocumentFragment)n;
            Node child = frag.getFirstChild();
            while (child != null) {
                writeNode(child, writer, repairing);
                child = child.getNextSibling();
            }
            break;
        }
        default:
            throw new IllegalStateException("Found type: " + n.getClass().getName());
        }        
    }

    public static Document read(XMLStreamReader reader) throws XMLStreamException {
        return read(reader, false);
    }
    public static Document read(XMLStreamReader reader, boolean recordLoc) throws XMLStreamException {
        Document doc = DOMUtils.createDocument();
        if (reader.getLocation().getSystemId() != null) {
            try {
                doc.setDocumentURI(new String(reader.getLocation().getSystemId()));
            } catch (Exception e) {
                //ignore - probably not DOM level 3
            }
        }
        readDocElements(doc, doc, reader, true, recordLoc);
        return doc;
    }
    
    public static Document read(DocumentBuilder builder, XMLStreamReader reader, boolean repairing) 
        throws XMLStreamException {
        Document doc = builder.newDocument();
        if (reader.getLocation().getSystemId() != null) {
            try {
                doc.setDocumentURI(new String(reader.getLocation().getSystemId()));
            } catch (Exception e) {
                //ignore - probably not DOM level 3
            }
        }
        readDocElements(doc, reader, repairing);
        return doc;
    }

    /**
     * @param parent
     * @return
     */
    private static Document getDocument(Node parent) {
        return (parent instanceof Document) ? (Document)parent : parent.getOwnerDocument();
    }

    /**
     * @param parent
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private static Element startElement(Document doc, 
                                        Node parent, 
                                        XMLStreamReader reader,
                                        boolean repairing,
                                        boolean recordLocation)
        throws XMLStreamException {

        Element e = doc.createElementNS(reader.getNamespaceURI(), reader.getLocalName());
        if (reader.getPrefix() != null) {
            e.setPrefix(reader.getPrefix());
        }       
        e = (Element)parent.appendChild(e);
        recordLocation = addLocation(doc, e, reader, recordLocation);

        for (int ns = 0; ns < reader.getNamespaceCount(); ns++) {
            String uri = reader.getNamespaceURI(ns);
            String prefix = reader.getNamespacePrefix(ns);

            declare(e, uri, prefix);
        }

        for (int att = 0; att < reader.getAttributeCount(); att++) {
            String name = reader.getAttributeLocalName(att);
            String prefix = reader.getAttributePrefix(att);
            if (prefix != null && prefix.length() > 0) {
                name = prefix + ":" + name;
            }

            Attr attr = doc.createAttributeNS(reader.getAttributeNamespace(att), name);
            attr.setValue(reader.getAttributeValue(att));
            e.setAttributeNode(attr);
        }

        if (repairing && !isDeclared(e, reader.getNamespaceURI(), reader.getPrefix())) {
            declare(e, reader.getNamespaceURI(), reader.getPrefix());
        }

        reader.next();

        readDocElements(doc, e, reader, repairing, recordLocation);

        return e;
    }

    private static boolean isDeclared(Element e, String namespaceURI, String prefix) {
        Attr att;
        if (prefix != null && prefix.length() > 0) {
            att = e.getAttributeNodeNS(XML_NS, prefix);
        } else {
            att = e.getAttributeNode("xmlns");
        }

        if (att != null && att.getNodeValue().equals(namespaceURI)) {
            return true;
        }

        if (e.getParentNode() instanceof Element) {
            return isDeclared((Element)e.getParentNode(), namespaceURI, prefix);
        }

        return false;
    }
    public static void readDocElements(Node parent, XMLStreamReader reader, boolean repairing) 
        throws XMLStreamException {
        Document doc = getDocument(parent);
        readDocElements(doc, parent, reader, repairing, false);
    }

    /**
     * @param parent
     * @param reader
     * @throws XMLStreamException
     */
    public static void readDocElements(Document doc, Node parent,
                                       XMLStreamReader reader, boolean repairing, boolean recordLoc)
        throws XMLStreamException {

        int event = reader.getEventType();
        while (reader.hasNext()) {
            switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                startElement(doc, parent, reader, repairing, recordLoc);
                
                if (parent instanceof Document) {
                    return;
                }
                break;
            case XMLStreamConstants.END_ELEMENT:
                return;
            case XMLStreamConstants.NAMESPACE:
                break;
            case XMLStreamConstants.ATTRIBUTE:
                break;
            case XMLStreamConstants.CHARACTERS:
                if (parent != null) {
                    recordLoc = addLocation(doc, 
                                            parent.appendChild(doc.createTextNode(reader.getText())),
                                            reader, recordLoc);
                }
                break;
            case XMLStreamConstants.COMMENT:
                if (parent != null) {
                    parent.appendChild(doc.createComment(reader.getText()));
                }
                break;
            case XMLStreamConstants.CDATA:
                recordLoc = addLocation(doc, 
                                        parent.appendChild(doc.createCDATASection(reader.getText())),
                                        reader, recordLoc);
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                parent.appendChild(doc.createProcessingInstruction(reader.getPITarget(), reader.getPIData()));
                break;
            default:
                break;
            }

            if (reader.hasNext()) {
                event = reader.next();
            }
        }
    }
    private static boolean addLocation(Document doc, Node node, 
                                    XMLStreamReader reader,
                                    boolean recordLoc) {
        if (recordLoc) {
            Location loc = reader.getLocation();
            if (loc != null && (loc.getColumnNumber() != 0 || loc.getLineNumber() != 0)) {
                try {
                    final int charOffset = loc.getCharacterOffset();
                    final int colNum = loc.getColumnNumber();
                    final int linNum = loc.getLineNumber();
                    final String pubId = loc.getPublicId() == null ? doc.getDocumentURI() : loc.getPublicId();
                    final String sysId = loc.getSystemId() == null ? doc.getDocumentURI() : loc.getSystemId();
                    Location loc2 = new Location() {
                        public int getCharacterOffset() {
                            return charOffset;
                        }
                        public int getColumnNumber() {
                            return colNum;
                        }
                        public int getLineNumber() {
                            return linNum;
                        }
                        public String getPublicId() {
                            return pubId;
                        }
                        public String getSystemId() {
                            return sysId;
                        }
                    };
                    node.setUserData("location", loc2, new UserDataHandler() {
                        public void handle(short operation, String key, Object data, Node src, Node dst) {
                            if (operation == NODE_CLONED) {
                                dst.setUserData(key, data, this);
                            }
                        }
                    });
                } catch (Exception ex) {
                    //possibly not DOM level 3, won't be able to record this then
                    return false;
                }
            }
        }
        return recordLoc;
    }

    private static void declare(Element node, String uri, String prefix) {
        String qualname;
        if (prefix != null && prefix.length() > 0) {
            qualname = "xmlns:" + prefix;
        } else {
            qualname = "xmlns";
        }
        Attr attr = node.getOwnerDocument().createAttributeNS(XML_NS, qualname);
        attr.setValue(uri);
        node.setAttributeNodeNS(attr);
    }
    public static XMLStreamReader createXMLStreamReader(InputSource src) {
        String sysId = src.getSystemId() == null ? null : new String(src.getSystemId());
        String pubId = src.getPublicId() == null ? null : new String(src.getPublicId());
        if (src.getByteStream() != null) {
            if (src.getEncoding() == null) {
                StreamSource ss = new StreamSource(src.getByteStream(), sysId);
                ss.setPublicId(pubId);
                return createXMLStreamReader(ss);
            }
            return createXMLStreamReader(src.getByteStream(), src.getEncoding());
        } else if (src.getCharacterStream() != null) {
            StreamSource ss = new StreamSource(src.getCharacterStream(), sysId);
            ss.setPublicId(pubId);
            return createXMLStreamReader(ss);
        } else {
            try {
                URL url = new URL(sysId);
                StreamSource ss = new StreamSource(url.openStream(), sysId);
                ss.setPublicId(pubId);
                return createXMLStreamReader(ss);
            } catch (Exception ex) {
                //ignore - not a valid URL
            }
        }
        throw new IllegalArgumentException("InputSource must have a ByteStream or CharacterStream");
    }
    /**
     * @param in
     * @param encoding
     * @param ctx
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(InputStream in, String encoding) {
        if (encoding == null) {
            encoding = "UTF-8";
        }

        XMLInputFactory factory = getXMLInputFactory();
        try {
            return factory.createXMLStreamReader(in, encoding);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        } finally {
            returnXMLInputFactory(factory);
        }
    }

    /**
     * @param in
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(InputStream in) {
        XMLInputFactory factory = getXMLInputFactory();
        try {
            return factory.createXMLStreamReader(in);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        } finally {
            returnXMLInputFactory(factory);
        }
    }
    public static XMLStreamReader createXMLStreamReader(String systemId, InputStream in) {
        XMLInputFactory factory = getXMLInputFactory();
        try {
            return factory.createXMLStreamReader(systemId, in);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        } finally {
            returnXMLInputFactory(factory);
        }
    }
    
    public static XMLStreamReader createXMLStreamReader(Element el) {
        return new W3CDOMStreamReader(el);
    }
    public static XMLStreamReader createXMLStreamReader(Document doc) {
        return new W3CDOMStreamReader(doc.getDocumentElement());
    }
    public static XMLStreamReader createXMLStreamReader(Element el, String sysId) {
        return new W3CDOMStreamReader(el, sysId);
    }
    public static XMLStreamReader createXMLStreamReader(Document doc, String sysId) {
        return new W3CDOMStreamReader(doc.getDocumentElement(), sysId);
    }
    
    public static XMLStreamReader createXMLStreamReader(Source source) {
        try {
            if (source instanceof DOMSource) {
                DOMSource ds = (DOMSource)source;
                Node nd = ds.getNode();
                Element el = null;
                if (nd instanceof Document) {
                    el = ((Document)nd).getDocumentElement();
                } else if (nd instanceof Element) {
                    el = (Element)nd;
                }
                
                if (null != el) {
                    return new W3CDOMStreamReader(el, source.getSystemId());
                }
            } else if ("javax.xml.transform.stax.StAXSource".equals(source.getClass().getName())) {
                try {
                    return (XMLStreamReader)source.getClass()
                        .getMethod("getXMLStreamReader").invoke(source);
                } catch (Exception ex) {
                    //ignore
                }
            }
            
            XMLInputFactory factory = getXMLInputFactory();
            try {
                XMLStreamReader reader = factory.createXMLStreamReader(source);
                if (reader == null && source instanceof StreamSource) {
                    //createXMLStreamReader from Source is optional, we'll try and map it
                    StreamSource ss = (StreamSource)source;
                    if (ss.getInputStream() != null) {
                        reader = factory.createXMLStreamReader(ss.getSystemId(),
                                                               ss.getInputStream());
                    } else {
                        reader = factory.createXMLStreamReader(ss.getSystemId(),
                                                               ss.getReader());
                    }
                }
                return reader;
            } finally {
                returnXMLInputFactory(factory);
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        }
    }

    /**
     * @param reader
     * @return
     */
    public static XMLStreamReader createXMLStreamReader(Reader reader) {
        XMLInputFactory factory = getXMLInputFactory();
        try {
            return factory.createXMLStreamReader(reader);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        } finally {
            returnXMLInputFactory(factory);
        }
    }

    /**
     * Reads a QName from the element text. Reader must be positioned at the
     * start tag.
     */
    public static QName readQName(XMLStreamReader reader) throws XMLStreamException {
        String value = reader.getElementText();
        if (value == null) {
            return null;
        }
        
        int index = value.indexOf(":");

        if (index == -1) {
            return new QName(value);
        }

        String prefix = value.substring(0, index);
        String localName = value.substring(index + 1);
        String ns = reader.getNamespaceURI(prefix);

        if ((prefix != null && ns == null) || localName == null) {
            throw new RuntimeException("Invalid QName in mapping: " + value);
        }

        if (ns == null) {
            return new QName(localName);
        }
        
        return new QName(ns, localName, prefix);
    }
    
    /**
     * Create a unique namespace uri/prefix combination.
     * 
     * @param nsUri
     * @return The namespace with the specified URI. If one doesn't exist, one
     *         is created.
     * @throws XMLStreamException
     */
    public static String getUniquePrefix(XMLStreamWriter writer, String namespaceURI, boolean declare)
        throws XMLStreamException {
        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = getUniquePrefix(writer);

            if (declare) {
                writer.setPrefix(prefix, namespaceURI);
                writer.writeNamespace(prefix, namespaceURI);
            }
        }
        return prefix;
    }
    public static String getUniquePrefix(XMLStreamWriter writer, String namespaceURI)
        throws XMLStreamException {
        return getUniquePrefix(writer, namespaceURI, false);
    }
    public static String getUniquePrefix(XMLStreamWriter writer) {
        NamespaceContext nc = writer.getNamespaceContext();
        if (nc == null) {
            return DEF_PREFIXES[0];
        }
        for (String t : DEF_PREFIXES) {
            String uri = nc.getNamespaceURI(t);
            if (StringUtils.isEmpty(uri)) {
                return t;
            }
        }

        int n = 10;
        while (true) {
            String nsPrefix = "ns" + n;
            String uri = nc.getNamespaceURI(nsPrefix);
            if (StringUtils.isEmpty(uri)) {
                return nsPrefix;
            }
            n++;
        }
    }
    

    public static void printXmlFragment(XMLStreamReader reader) {
        try {
            LOG.info(XMLUtils.toString(StaxUtils.read(reader), 4));
        } catch (XMLStreamException e) {
            LOG.severe(e.getMessage());
        }
    }
    
    
    private static void writeStartElementEvent(XMLEvent event, XMLStreamWriter writer) 
        throws XMLStreamException {
        StartElement start = event.asStartElement();
        QName name = start.getName();
        String nsURI = name.getNamespaceURI();
        String localName = name.getLocalPart();
        String prefix = name.getPrefix();
        
        if (prefix != null) {
            writer.writeStartElement(prefix, localName, nsURI);
        } else if (nsURI != null) {
            writer.writeStartElement(localName, nsURI);
        } else {
            writer.writeStartElement(localName);
        }
        Iterator it = start.getNamespaces();
        while (it != null && it.hasNext()) {
            writeEvent((XMLEvent)it.next(), writer);
        }
        
        it = start.getAttributes();
        while (it != null && it.hasNext()) {
            writeAttributeEvent((Attribute)it.next(), writer);            
        }
    }
    private static void writeAttributeEvent(XMLEvent event, XMLStreamWriter writer) 
        throws XMLStreamException {
        
        Attribute attr = (Attribute)event;
        QName name = attr.getName();
        String nsURI = name.getNamespaceURI();
        String localName = name.getLocalPart();
        String prefix = name.getPrefix();
        String value = attr.getValue();

        if (prefix != null) {
            writer.writeAttribute(prefix, nsURI, localName, value);
        } else if (nsURI != null) {
            writer.writeAttribute(nsURI, localName, value);
        } else {
            writer.writeAttribute(localName, value);
        }
    }

    public static void writeEvent(XMLEvent event, XMLStreamWriter writer)
        throws XMLStreamException {

        switch (event.getEventType()) {
        case XMLEvent.START_ELEMENT:
            writeStartElementEvent(event, writer);
            break;
        case XMLEvent.END_ELEMENT:
            writer.writeEndElement();
            break;
        case XMLEvent.ATTRIBUTE: 
            writeAttributeEvent(event, writer);
            break;
        case XMLEvent.ENTITY_REFERENCE:
            writer.writeEntityRef(((javax.xml.stream.events.EntityReference)event).getName());
            break;
        case XMLEvent.DTD:
            writer.writeDTD(((DTD)event).getDocumentTypeDeclaration());
            break;
        case XMLEvent.PROCESSING_INSTRUCTION:
            if (((javax.xml.stream.events.ProcessingInstruction)event).getData() != null) {
                writer.writeProcessingInstruction(
                    ((javax.xml.stream.events.ProcessingInstruction)event).getTarget(), 
                    ((javax.xml.stream.events.ProcessingInstruction)event).getData());
            } else {
                writer.writeProcessingInstruction(
                    ((javax.xml.stream.events.ProcessingInstruction)event).getTarget());
            }
            break;
        case XMLEvent.NAMESPACE:
            if (((Namespace)event).isDefaultNamespaceDeclaration()) {
                writer.writeDefaultNamespace(((Namespace)event).getNamespaceURI());
                writer.setDefaultNamespace(((Namespace)event).getNamespaceURI());
            } else {
                writer.writeNamespace(((Namespace)event).getPrefix(),
                                      ((Namespace)event).getNamespaceURI());
                writer.setPrefix(((Namespace)event).getPrefix(),
                                 ((Namespace)event).getNamespaceURI()); 
            }
            break;
        case XMLEvent.COMMENT:
            writer.writeComment(((javax.xml.stream.events.Comment)event).getText());
            break;
        case XMLEvent.CHARACTERS:
        case XMLEvent.SPACE:
            writer.writeCharacters(event.asCharacters().getData());
            break;
        case XMLEvent.CDATA:
            writer.writeCData(event.asCharacters().getData());
            break;
        case XMLEvent.START_DOCUMENT:
            if (((StartDocument)event).encodingSet()) {
                writer.writeStartDocument(((StartDocument)event).getCharacterEncodingScheme(),
                                          ((StartDocument)event).getVersion());

            } else {
                writer.writeStartDocument(((StartDocument)event).getVersion());
            }
            break;
        case XMLEvent.END_DOCUMENT:
            writer.writeEndDocument();
            break;
        default:
            //shouldn't get here
        }
    }

    public static String toString(Document doc) throws XMLStreamException {
        StringWriter sw = new StringWriter(1024);
        XMLStreamWriter writer = createXMLStreamWriter(sw);
        copy(doc, writer);
        writer.flush();
        return sw.toString();
    }
    public static String toString(Element el) throws XMLStreamException {
        StringWriter sw = new StringWriter(1024);
        XMLStreamWriter writer = createXMLStreamWriter(sw);
        copy(el, writer);
        writer.flush();
        return sw.toString();
    }

}
