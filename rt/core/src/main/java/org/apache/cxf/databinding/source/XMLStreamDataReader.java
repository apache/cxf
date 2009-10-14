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
package org.apache.cxf.databinding.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.FragmentStreamReader;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;



public class XMLStreamDataReader implements DataReader<XMLStreamReader> {
    private static final Logger LOG = LogUtils.getL7dLogger(XMLStreamDataReader.class);
    private Schema schema;
    private Message message;
    
    public Object read(MessagePartInfo part, XMLStreamReader input) {
        return read(null, input, part.getTypeClass());
    }

    public Object read(final QName name, XMLStreamReader input, Class type) {
        if (Source.class.equals(type) && message != null) {
            //generic Source, find the preferred type
            String s = (String)message.getContextualProperty(SourceDataBinding.PREFERRED_FORMAT);
            if (StringUtils.isEmpty(s)) {
                s = "dom";
            }
            if ("dom".equalsIgnoreCase(s)) {
                type = DOMSource.class;
            } else if ("stream".equalsIgnoreCase(s)) {
                type = StreamSource.class;
            } else if ("sax".equalsIgnoreCase(s) || "cxf.stax".equalsIgnoreCase(s)) {
                type = SAXSource.class;
            } else if ("stax".equals(s)) {
                try {
                    type = ClassLoaderUtils.loadClass("javax.xml.transform.stax.StAXSource", getClass());
                } catch (ClassNotFoundException e) {
                    type = SAXSource.class;
                }
            } else {
                type = DOMSource.class;
            }
        }
        try {
            Element dom = null;
            if (schema != null) {
                dom = validate(input);
                input = StaxUtils.createXMLStreamReader(dom);
            }
            if (type != null) {
                if (SAXSource.class.isAssignableFrom(type)
                    || StaxSource.class.isAssignableFrom(type)) {
                    return new StaxSource(resetForStreaming(input));
                } else if (StreamSource.class.isAssignableFrom(type)) {
                    return new StreamSource(getInputStream(input));
                } else if (XMLStreamReader.class.isAssignableFrom(type)) {
                    return resetForStreaming(input);
                } else if (DataSource.class.isAssignableFrom(type)) {
                    final InputStream ins = getInputStream(input);
                    return new DataSource() {
                        public String getContentType() {
                            return "text/xml";
                        }
                        public InputStream getInputStream() throws IOException {
                            return ins;
                        }
                        public String getName() {
                            return name.toString();
                        }
                        public OutputStream getOutputStream() throws IOException {
                            return null;
                        }
                    };
                } else if ("javax.xml.transform.stax.StAXSource".equals(type.getName())) {
                    input = resetForStreaming(input);
                    Object o = createStaxSource(input, type);
                    if (o != null) {
                        return o;
                    }
                }
            }
            return dom == null ? read(input) : new DOMSource(dom);
        } catch (IOException e) {
            throw new Fault("COULD_NOT_READ_XML_STREAM", LOG, e);
        } catch (XMLStreamException e) {
            throw new Fault("COULD_NOT_READ_XML_STREAM", LOG, e);
        } catch (SAXException e) {
            throw new Fault("COULD_NOT_READ_XML_STREAM", LOG, e);
        }
    }
    
    private Object createStaxSource(XMLStreamReader input, Class<?> type) {
        try {
            return type.getConstructor(XMLStreamReader.class).newInstance(input);
        } catch (Exception e) {
            //ignore
        }
        return null;
    }
    
    private XMLStreamReader resetForStreaming(XMLStreamReader input) throws XMLStreamException {
        //Need to mark the message as streaming this so input stream
        //is not closed and additional parts are not read and such
        if (message != null) {
            message.removeContent(XMLStreamReader.class);
            final InputStream ins = message.getContent(InputStream.class);
            message.removeContent(InputStream.class);
            
            input = new FragmentStreamReader(input) {
                boolean closed;
                public boolean hasNext() throws XMLStreamException {
                    boolean b = super.hasNext();
                    if (!b && !closed) {
                        closed = true;
                        try {
                            ins.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                    return b;
                }
            };
        }
        return input;
    }

    private Element validate(XMLStreamReader input) 
        throws XMLStreamException, SAXException, IOException {
        DOMSource ds = read(input);
        schema.newValidator().validate(ds);
        Node nd = ds.getNode();
        if (nd instanceof Document) {
            return ((Document)nd).getDocumentElement();
        }
        return (Element)ds.getNode();
    }

    private InputStream getInputStream(XMLStreamReader input) 
        throws XMLStreamException, IOException {
        
        CachedOutputStream out = new CachedOutputStream();
        try {
            XMLStreamWriter xsw = StaxUtils.createXMLStreamWriter(out);
            StaxUtils.copy(input, xsw);
            xsw.close();
            return out.getInputStream();
        } finally {
            out.close();
        }
    }
    public DOMSource read(XMLStreamReader reader) {
        // Use a DOMSource for now, we should really use a StaxSource/SAXSource though for 
        // performance reasons
        try {
            XMLStreamReader reader2 = reader;
            if (reader2 instanceof DepthXMLStreamReader) {
                reader2 = ((DepthXMLStreamReader)reader2).getReader();
            }
            if (reader2 instanceof W3CDOMStreamReader) {
                W3CDOMStreamReader domreader = (W3CDOMStreamReader)reader2;
                DOMSource o = new DOMSource(domreader.getCurrentElement());
                domreader.consumeFrame();
                return o;
            } else {
                Document document = StaxUtils.read(reader);
                return new DOMSource(document);
            }
        } catch (XMLStreamException e) {
            throw new Fault("COULD_NOT_READ_XML_STREAM", LOG, e);
        }
    }
    
    public void setSchema(Schema s) {
        schema = s;
    }

    public void setAttachments(Collection<Attachment> attachments) {
    }

    public void setProperty(String prop, Object value) {
        if (Message.class.getName().equals(prop)) {
            message = (Message)value;
        }
    }
}
