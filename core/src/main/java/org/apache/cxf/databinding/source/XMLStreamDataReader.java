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
import java.util.List;
import java.util.logging.Logger;

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

import jakarta.activation.DataSource;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.FragmentStreamReader;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxStreamFilter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.staxutils.validation.WoodstoxValidationImpl;



public class XMLStreamDataReader implements DataReader<XMLStreamReader> {
    private static final Logger LOG = LogUtils.getL7dLogger(XMLStreamDataReader.class);
    private static final QName XOP = new QName("http://www.w3.org/2004/08/xop/include", "Include");

    private final Class<?> preferred;
    private Schema schema;
    private Message message;

    public XMLStreamDataReader() {
        preferred = null;
    }
    public XMLStreamDataReader(Class<?> cls) {
        preferred = cls;
    }

    public Object read(MessagePartInfo part, XMLStreamReader input) {
        return read(null, input, part.getTypeClass());
    }

    public Object read(final QName name, XMLStreamReader input, Class<?> type) {
        if (type == null) {
            type = preferred;
        }
        if (Source.class.equals(type) && message != null) {
            //generic Source, find the preferred type
            String s = (String)message.getContextualProperty(SourceDataBinding.PREFERRED_FORMAT);
            if (StringUtils.isEmpty(s)) {
                s = "sax";
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
                Object retVal = null;
                if (SAXSource.class.isAssignableFrom(type)
                    || StaxSource.class.isAssignableFrom(type)) {
                    retVal = new StaxSource(resetForStreaming(input));
                } else if (StreamSource.class.isAssignableFrom(type)) {
                    retVal = new StreamSource(getInputStream(input));
                } else if (XMLStreamReader.class.isAssignableFrom(type)) {
                    retVal = resetForStreaming(input);
                } else if (Element.class.isAssignableFrom(type)) {
                    retVal = dom == null ? read(input).getNode() : dom;
                } else if (Document.class.isAssignableFrom(type)) {
                    retVal = dom == null ? read(input).getNode() : dom;
                } else if (DataSource.class.isAssignableFrom(type)) {
                    final InputStream ins = getInputStream(input);
                    retVal = new DataSource() {
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
                        retVal = o;
                    }
                }
                if (retVal != null) {
                    return retVal;
                }
            }
            return dom == null ? read(input) : new DOMSource(dom);
        } catch (IOException e) {
            throw new Fault("COULD_NOT_READ_XML_STREAM", LOG, e);
        } catch (XMLStreamException e) {
            throw new Fault("COULD_NOT_READ_XML_STREAM_CAUSED_BY", LOG, e,
                            e.getMessage());
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
            if (message.getInterceptorChain() != null) {
                message.getInterceptorChain().remove(StaxInEndingInterceptor.INSTANCE);
                message.getInterceptorChain().add(new StaxInEndingInterceptor(Phase.POST_INVOKE));
            }

            message.removeContent(XMLStreamReader.class);
            final InputStream ins = message.getContent(InputStream.class);
            message.removeContent(InputStream.class);

            return new FragmentStreamReader(input, true) {
                boolean closed;
                public boolean hasNext() throws XMLStreamException {
                    boolean b = super.hasNext();
                    if (!b && !closed) {
                        close();
                    }
                    return b;
                }

                public void close() throws XMLStreamException {
                    closed = true;
                    try {
                        super.close();
                    }  catch (XMLStreamException e) {
                        //ignore
                    }
                    if (ins != null) {
                        try {
                            ins.close();
                        }  catch (IOException e) {
                            //ignore
                        }
                    }
                }
            };
        }
        return input;
    }

    private Element validate(XMLStreamReader input) throws XMLStreamException, IOException {
        DOMSource ds = read(input);
        final Element rootElement;
        if (ds.getNode() instanceof Document) {
            rootElement = ((Document)ds.getNode()).getDocumentElement();
        } else {
            rootElement = (Element)ds.getNode();
        }

        WoodstoxValidationImpl impl = new WoodstoxValidationImpl();
        XMLStreamWriter nullWriter = null;
        boolean notUseMsvSchemaValidator = 
            MessageUtils.getContextualBoolean(message, SourceDataBinding.NOT_USE_MSV_SCHEMA_VALIDATOR, false);
        if (impl.canValidate()
            && !notUseMsvSchemaValidator) {
            nullWriter = StaxUtils.createXMLStreamWriter(new NUllOutputStream());
            impl.setupValidation(nullWriter, message.getExchange().getEndpoint(),
                                 message.getExchange().getService().getServiceInfos().get(0));
        }
        //check if the impl can still validate after the setup, possible issue loading schemas or similar
        if (impl.canValidate() && !notUseMsvSchemaValidator) {
            //Can use the MSV libs and woodstox to handle the schema validation during
            //parsing and processing.   Much faster and single traversal
            //filter xop node
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(ds);
            XMLStreamReader filteredReader =
                StaxUtils.createFilteredReader(reader,
                                               new StaxStreamFilter(new QName[] {XOP}));

            StaxUtils.copy(filteredReader, nullWriter);
        } else {
            //MSV not available, use a slower method of cloning the data, replace the xop's, validate
            LOG.fine("NO_MSV_AVAILABLE");
            Element newElement = rootElement;
            if (DOMUtils.hasElementWithName(rootElement, "http://www.w3.org/2004/08/xop/include", "Include")) {
                newElement = (Element)rootElement.cloneNode(true);
                List<Element> elems = DOMUtils.findAllElementsByTagNameNS(newElement,
                                                                          "http://www.w3.org/2004/08/xop/include",
                                                                          "Include");
                for (Element include : elems) {
                    Node parentNode = include.getParentNode();
                    parentNode.removeChild(include);
                    String cid = DOMUtils.getAttribute(include, "href");
                    //set the fake base64Binary to validate instead of reading the attachment from message
                    parentNode.setTextContent(jakarta.xml.bind.DatatypeConverter.printBase64Binary(cid.getBytes()));
                }
            }
            try {
                schema.newValidator().validate(new DOMSource(newElement));
            } catch (SAXException e) {
                throw new XMLStreamException(e.getMessage(), e);
            }
        }
        return rootElement;
    }

    private InputStream getInputStream(XMLStreamReader input)
        throws XMLStreamException, IOException {

        try (CachedOutputStream out = new CachedOutputStream()) {
            StaxUtils.copy(input, out);
            return out.getInputStream();
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
            }
            Document document = StaxUtils.read(reader);
            if (reader.hasNext()) {
                //need to actually consume the END_ELEMENT
                reader.next();
            }
            return new DOMSource(document);
        } catch (XMLStreamException e) {
            throw new Fault("COULD_NOT_READ_XML_STREAM_CAUSED_BY", LOG, e,
                            e.getMessage());
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

    static class NUllOutputStream extends OutputStream {
        public void write(byte[] b, int off, int len) {
        }
        public void write(int b) {
        }

        public void write(byte[] b) throws IOException {
        }
    }
}
