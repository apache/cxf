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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;

public final class XMLUtils {
    
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
            writer = new PrettyPrintXMLStreamWriter(writer, indent);
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
            writer = new PrettyPrintXMLStreamWriter(writer, indent);
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

    public static void generateXMLFile(Element element, Writer writer) throws XMLStreamException {
        writeTo(new DOMSource(element), writer, 2, "UTF-8", false);
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
