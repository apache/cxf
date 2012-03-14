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
package org.apache.cxf.jaxrs.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.apache.cxf.staxutils.DepthExceededStaxException;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.codehaus.jettison.AbstractXMLInputFactory;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.badgerfish.BadgerFishXMLInputFactory;
import org.codehaus.jettison.badgerfish.BadgerFishXMLOutputFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.codehaus.jettison.mapped.TypeConverter;

public final class JSONUtils {

    private static final String XSI_PREFIX = "xsi";
    private static final String XSI_URI = WSDLConstants.NS_SCHEMA_XSI; 
    private static final Charset UTF8 = Charset.forName("utf-8");

    private JSONUtils() {
    }
    
    public static XMLStreamWriter createBadgerFishWriter(OutputStream os) throws XMLStreamException {
        XMLOutputFactory factory = new BadgerFishXMLOutputFactory();
        return factory.createXMLStreamWriter(os);
    }
    
    public static XMLStreamReader createBadgerFishReader(InputStream is) throws XMLStreamException {
        XMLInputFactory factory = new BadgerFishXMLInputFactory();
        return factory.createXMLStreamReader(is);
    }
        
    public static XMLStreamWriter createStreamWriter(OutputStream os, 
                                                     QName qname, 
                                                     boolean writeXsiType,
                                                     Configuration config,
                                                     boolean serializeAsArray,
                                                     List<String> arrayKeys,
                                                     boolean dropRootElement) throws Exception {
        
        MappedNamespaceConvention convention = new PrefixRespectingMappedNamespaceConvention(config);
        AbstractXMLStreamWriter xsw = new MappedXMLStreamWriter(
                                            convention, 
                                            new OutputStreamWriter(os, UTF8));
        if (serializeAsArray) {
            if (arrayKeys != null) {
                for (String key : arrayKeys) {
                    xsw.serializeAsArray(key);
                }
            } else {
                String key = getKey(convention, qname);
                xsw.serializeAsArray(key);
            }
        }
        XMLStreamWriter writer = !writeXsiType || dropRootElement 
            ? new IgnoreContentJettisonWriter(xsw, writeXsiType, 
                                              dropRootElement ? qname : null) : xsw;
        
        return writer;
    }    
    
    public static Configuration createConfiguration(ConcurrentHashMap<String, String> namespaceMap,
                                                    boolean writeXsiType,
                                                    boolean attributesAsElements,
                                                    TypeConverter converter) {
        if (writeXsiType) {
            namespaceMap.putIfAbsent(XSI_URI, XSI_PREFIX);
        }
        Configuration c = new Configuration(namespaceMap);
        c.setSupressAtAttributes(attributesAsElements);
        if (converter != null) {
            c.setTypeConverter(converter);
        }
        return c;
    }
    
    public static XMLStreamWriter createIgnoreMixedContentWriterIfNeeded(XMLStreamWriter writer, 
                                                                         boolean ignoreMixedContent) {
        return ignoreMixedContent ? new IgnoreMixedContentWriter(writer) : writer; 
    }
    
    public static XMLStreamWriter createIgnoreNsWriterIfNeeded(XMLStreamWriter writer, 
                                                               boolean ignoreNamespaces) {
        return ignoreNamespaces ? new IgnoreNsWriter(writer) : writer; 
    }
    
    private static String getKey(MappedNamespaceConvention convention, QName qname) throws Exception {
        return convention.createKey(qname.getPrefix(), 
                                    qname.getNamespaceURI(),
                                    qname.getLocalPart());
            
        
    }
    
    public static XMLStreamReader createStreamReader(InputStream is, boolean readXsiType,
        ConcurrentHashMap<String, String> namespaceMap) throws Exception {
        return createStreamReader(is, readXsiType, namespaceMap, null);
    }
    
    public static XMLStreamReader createStreamReader(InputStream is, boolean readXsiType,
        ConcurrentHashMap<String, String> namespaceMap, 
        DocumentDepthProperties depthProps) throws Exception {
        if (readXsiType) {
            namespaceMap.putIfAbsent(XSI_URI, XSI_PREFIX);
        }
        XMLInputFactory factory = depthProps != null 
            ? new JettisonMappedReaderFactory(namespaceMap, depthProps) 
            : new MappedXMLInputFactory(namespaceMap);
        return new JettisonReader(namespaceMap, factory.createXMLStreamReader(is));
    }
    
    private static class JettisonMappedReaderFactory extends AbstractXMLInputFactory {
        private static final int INPUT_BUF_SIZE = 4096;
        private MappedNamespaceConvention convention;
        private DocumentDepthProperties depthProps;
        public JettisonMappedReaderFactory(Map<?, ?> nstojns, DocumentDepthProperties depthProps) {
            convention = new MappedNamespaceConvention(new Configuration(nstojns));
            this.depthProps = depthProps;
        }
        @Override
        public XMLStreamReader createXMLStreamReader(JSONTokener tokener) throws XMLStreamException {
            try {
                JSONObject root = new JettisonJSONObject(tokener, depthProps);
                return new MappedXMLStreamReader(root, convention);
            } catch (JSONException e) {
                throw new XMLStreamException(e);
            }
        } 
        private String readAll(InputStream in, String encoding)
            throws IOException {
            
            final byte[] buffer = new byte[INPUT_BUF_SIZE];
            ByteArrayOutputStream bos = null;
            while (true) {
                int count = in.read(buffer);
                if (count < 0) { // EOF
                    break;
                }
                if (bos == null) {
                    int cap;
                    if (count < 64) {
                        cap = 64;
                    } else if (count == INPUT_BUF_SIZE) {
                        // Let's assume there's more coming, not just this chunk
                        cap = INPUT_BUF_SIZE * 4;
                    } else {
                        cap = count;
                    }
                    bos = new ByteArrayOutputStream(cap);
                }
                bos.write(buffer, 0, count);
            }
            return (bos == null) ? "" : bos.toString(encoding);
        }
        public XMLStreamReader createXMLStreamReader(InputStream is, String charset) 
            throws XMLStreamException {
            /* !!! This is not really correct: should (try to) auto-detect
             * encoding, since JSON only allows 3 Unicode-based variants.
             * For now it's ok to default to UTF-8 though.
             */
            if (charset == null) {
                charset = "UTF-8";
            }
            try {
                String doc = readAll(is, charset);
                return createXMLStreamReader(new JettisonJSONTokener(doc, depthProps));
            } catch (IOException e) {
                throw new XMLStreamException(e);
            }
        }
    }

    private static class JettisonJSONTokener extends JSONTokener {
        private DocumentDepthProperties depthProps;
        public JettisonJSONTokener(String s, DocumentDepthProperties depthProps) {
            super(s);
            this.depthProps = depthProps;
        }
        public Object nextValue() throws JSONException {
            char c = nextClean();
            switch (c) {
            case '"':
            case '\'':
                return nextString(c);
            case '{':
                back();
                return new JettisonJSONObject(this, depthProps);
            case '[':
                back();
                return new JSONArray(this);
            default:    
            }

            return finalize(c);
        }
        private Object finalize(char c) throws JSONException { 
            StringBuffer sb = new StringBuffer();
            char b = c;
            while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
                sb.append(c);
                c = next();
            }
            back();

            String s = sb.toString().trim();
            if (s.length() == 0) {
                throw new JSONException("Missing value.");
            }
            Object res = null;
            if (s.equalsIgnoreCase("true")) {
                res = Boolean.TRUE;
            } else if (s.equalsIgnoreCase("false")) {
                res = Boolean.FALSE;
            } else if (s.equalsIgnoreCase("null")) {
                res = JSONObject.NULL;
            }
            if (res != null) {
                return res;
            }
            if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
                if (b == '0') {
                    if (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                        try {
                            res = new Integer(Integer.parseInt(s.substring(2),
                                    16));
                        } catch (Exception e) {
                            /* Ignore the error */
                        }
                    } else {
                        try {
                            res = new Integer(Integer.parseInt(s, 8));
                        } catch (Exception e) {
                            /* Ignore the error */
                        }
                    }
                }
                if (res == null) {
                    try {
                        res = new Integer(s);
                    } catch (Exception e) {
                        try {
                            res = new Long(s);
                        } catch (Exception f) {
                            try {
                                res = new Double(s);
                            }  catch (Exception g) {
                                res = s;
                            }
                        }
                    }
                }
                if (res != null) {
                    return res;
                }
            }
            return s;
        }
    }
    
    private static class JettisonJSONObject extends JSONObject {
        private static final long serialVersionUID = 9016458891093343731L;
        private int threshold;
        
        public JettisonJSONObject(JSONTokener x, DocumentDepthProperties depthProps) 
            throws JSONException {
            this.threshold = depthProps.getElementCountThreshold() != -1 
                ? depthProps.getElementCountThreshold() : depthProps.getInnerElementCountThreshold();
            String key;
            char c;
            if (x.nextClean() != '{') {
                throw x.syntaxError("A JSONObject text must begin with '{'");
            }
            for (;;) {
                c = x.nextClean();
                switch (c) {
                case 0:
                    throw x.syntaxError("A JSONObject text must end with '}'");
                case '}':
                    return;
                default:
                    x.back();
                    key = x.nextValue().toString();
                }

                c = x.nextClean();
                if (c == '=') {
                    if (x.next() != '>') {
                        x.back();
                    }
                } else if (c != ':') {
                    throw x.syntaxError("Expected a ':' after a key");
                }
                put(key, x.nextValue()); //NOPMD
                switch (x.nextClean()) {
                case ';':
                case ',':
                    if (x.nextClean() == '}') {
                        return;
                    }
                    x.back();
                    break;
                case '}':
                    return;
                default:
                    throw new JSONException("Expected a ',' or '}'");
                }
            }
        }
        public JSONObject put(String key, Object value) throws JSONException {
            JSONObject obj = super.put(key, value);
            if (threshold != -1 && super.length() >= threshold) {
                throw new DepthExceededStaxException();
            }
            return obj;
            
        }
    }
    
    private static class JettisonReader extends DepthXMLStreamReader {
        private Map<String, String> namespaceMap;
        public JettisonReader(Map<String, String> nsMap,
                                      XMLStreamReader reader) {
            super(reader);
            this.namespaceMap = nsMap;
        }
        
        @Override
        public String getAttributePrefix(int n) {
            QName name = getAttributeName(n);
            if (name != null 
                && XSI_URI.equals(name.getNamespaceURI())) {
                return XSI_PREFIX;
            } else {
                return super.getAttributePrefix(n);
            }
        }
        
        @Override
        public NamespaceContext getNamespaceContext() {
            return new NamespaceContext() {

                public String getNamespaceURI(String prefix) {
                    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                        if (entry.getValue().equals(prefix)) {
                            return entry.getKey();
                        }
                    }
                    return null;
                }

                public String getPrefix(String ns) {
                    return namespaceMap.get(ns);
                }

                public Iterator getPrefixes(String ns) {
                    String prefix = getPrefix(ns);
                    return prefix == null ? null : Collections.singletonList(prefix).iterator();
                }
                
            };
        }
    }
    
    private static class IgnoreContentJettisonWriter extends DelegatingXMLStreamWriter {
        
        private boolean writeXsiType;
        private QName ignoredQName;
        private boolean rootDropped;
        private int index; 
                
        public IgnoreContentJettisonWriter(XMLStreamWriter writer, boolean writeXsiType, QName qname) {
            super(writer);
            this.writeXsiType = writeXsiType;
            ignoredQName = qname;
        }
        
        public void writeAttribute(String prefix, String uri,
                                   String local, String value) throws XMLStreamException {
            if (!writeXsiType && "xsi".equals(prefix)
                    && ("type".equals(local) || "nil".equals(local))) {
                return;
            }
            super.writeAttribute(prefix, uri, local, value);
            
        }
        
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
            index++;
            if (ignoredQName != null && ignoredQName.getLocalPart().equals(local) 
                && ignoredQName.getNamespaceURI().equals(uri)) {
                rootDropped = true;
                return;
            }
            super.writeStartElement(prefix, local, uri);
        }
        
        @Override
        public void writeStartElement(String local) throws XMLStreamException {
            this.writeStartElement("", local, "");
        }
        
        @Override
        public void writeEndElement() throws XMLStreamException {
            index--;
            if (rootDropped && index == 0) {
                return;
            }
            super.writeEndElement();
        }
    }
    
    private static class IgnoreMixedContentWriter extends DelegatingXMLStreamWriter {
        String lastText;
        boolean isMixed;
        List<Boolean> mixed = new LinkedList<Boolean>();
        
        public IgnoreMixedContentWriter(XMLStreamWriter writer) {
            super(writer);
        }

        public void writeCharacters(String text) throws XMLStreamException {
            if (StringUtils.isEmpty(text.trim())) {
                lastText = text; 
            } else if (lastText != null) {
                lastText += text;
            } else if (!isMixed) {
                super.writeCharacters(text);                                
            } else {
                lastText = text;
            }
        }
        
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
            if (lastText != null) {
                isMixed = true;
            }
            mixed.add(0, isMixed);
            lastText = null;
            isMixed = false;
            super.writeStartElement(prefix, local, uri);
        }
        public void writeStartElement(String uri, String local) throws XMLStreamException {
            if (lastText != null) {
                isMixed = true;
            }
            mixed.add(0, isMixed);
            lastText = null;
            isMixed = false;
            super.writeStartElement(uri, local);
        }
        public void writeStartElement(String local) throws XMLStreamException {
            if (lastText != null) {
                isMixed = true;
            }
            mixed.add(0, isMixed);
            lastText = null;
            isMixed = false;
            super.writeStartElement(local);
        }
        public void writeEndElement() throws XMLStreamException {
            if (lastText != null && (!isMixed || !StringUtils.isEmpty(lastText.trim()))) {
                super.writeCharacters(lastText.trim());                
            }
            super.writeEndElement();
            isMixed = mixed.get(0);
            mixed.remove(0);
        }

        
    }
    
    private static class IgnoreNsWriter extends DelegatingXMLStreamWriter {
        
        public IgnoreNsWriter(XMLStreamWriter writer) {
            super(writer);
        }

        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
            super.writeStartElement(local);
        }
        
        public void writeStartElement(String uri, String local) throws XMLStreamException {
            super.writeStartElement(local);
        }
        
        public void setPrefix(String pfx, String uri) throws XMLStreamException {
            // completed
        }
        
        public void setDefaultNamespace(String uri) throws XMLStreamException {
            // completed
        }
    }
}
