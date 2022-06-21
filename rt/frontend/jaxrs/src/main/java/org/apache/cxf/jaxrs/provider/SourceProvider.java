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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.DepthExceededStaxException;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;

@Provider
@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml", "text/html" })
public class SourceProvider<T> extends AbstractConfigurableProvider implements
    MessageBodyReader<T>, MessageBodyWriter<T> {

    private static final String PREFERRED_FORMAT = "source-preferred-format";
    private static final Logger LOG = LogUtils.getL7dLogger(SourceProvider.class);
    @Context
    private MessageContext context;


    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return Source.class.isAssignableFrom(type)
            || Node.class.isAssignableFrom(type);
    }

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return Source.class.isAssignableFrom(type)
               || XMLSource.class.isAssignableFrom(type)
               || Document.class.isAssignableFrom(type);
    }

    public T readFrom(Class<T> source, Type genericType, Annotation[] annotations, MediaType m,
        MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {

        Class<?> theSource = source;
        if (theSource == Source.class) {
            String s = getPreferredSource();
            if ("sax".equalsIgnoreCase(s) || "cxf.stax".equalsIgnoreCase(s)) {
                theSource = SAXSource.class;
            }
        }
        try {
            if (DOMSource.class.isAssignableFrom(theSource) || Document.class.isAssignableFrom(theSource)) {

                boolean docRequired = Document.class.isAssignableFrom(theSource);
                XMLStreamReader reader = getReader(is);
                try {
                    Document doc = StaxUtils.read(reader);
                    return source.cast(docRequired ? doc : new DOMSource(doc));
                } catch (DepthExceededStaxException e) {
                    throw ExceptionUtils.toWebApplicationException(null, JAXRSUtils.toResponse(413));
                } catch (XMLStreamException e) {
                    if (e.getMessage() != null && e.getMessage().startsWith("Maximum Number")) {
                        throw ExceptionUtils.toWebApplicationException(null, JAXRSUtils.toResponse(413));
                    }
                    throw ExceptionUtils.toBadRequestException(e, null);
                } catch (Exception e) {
                    IOException ioex = new IOException("Problem creating a Source object");
                    ioex.setStackTrace(e.getStackTrace());
                    throw ioex;
                } finally {
                    try {
                        reader.close();
                    } catch (XMLStreamException e) {
                        //ignore
                    }
                }
            } else if (SAXSource.class.isAssignableFrom(theSource)
                      || StaxSource.class.isAssignableFrom(theSource)) {
                return source.cast(new StaxSource(getReader(is)));
            } else if (StreamSource.class.isAssignableFrom(theSource)
                       || Source.class.isAssignableFrom(theSource)) {
                return source.cast(new StreamSource(getRealStream(is)));
            } else if (XMLSource.class.isAssignableFrom(theSource)) {
                return source.cast(new XMLSource(getRealStream(is)));
            }
        } catch (ClassCastException e) {
            String msg = "Unsupported class: " + source.getName();
            LOG.warning(msg);
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }

        throw new IOException("Unrecognized source");
    }

    protected XMLStreamReader getReader(InputStream is) {
        XMLStreamReader reader = getReaderFromMessage();
        if (reader == null) {
            reader = StaxUtils.createXMLStreamReader(is);
        }
        return configureReaderRestrictions(reader);
    }

    protected XMLStreamReader configureReaderRestrictions(XMLStreamReader reader) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message != null) {
            try {
                return StaxUtils.configureReader(reader, message);
            } catch (XMLStreamException ex) {
                throw ExceptionUtils.toInternalServerErrorException(ex, null);
            }
        }
        return reader;
    }

    protected InputStream getRealStream(InputStream is) throws IOException {
        XMLStreamReader reader = getReaderFromMessage();
        return reader == null ? is : getStreamFromReader(reader);
    }

    private InputStream getStreamFromReader(XMLStreamReader input)
        throws IOException {

        try (CachedOutputStream out = new CachedOutputStream()) {
            StaxUtils.copy(input, out);
            return out.getInputStream();
        } catch (XMLStreamException ex) {
            throw new IOException("XMLStreamException:" + ex.getMessage());
        }
    }

    protected XMLStreamReader getReaderFromMessage() {
        MessageContext mc = getContext();
        if (mc != null) {
            return mc.getContent(XMLStreamReader.class);
        }
        return null;
    }

    public void writeTo(T source, Class<?> clazz, Type genericType, Annotation[] annotations,
        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {

        String encoding = HttpUtils.getSetEncoding(mt, headers, StandardCharsets.UTF_8.name());

        final XMLStreamReader reader;
        if (source instanceof Source) {
            reader = StaxUtils.createXMLStreamReader((Source)source);
        } else if (source instanceof Document) {
            reader = StaxUtils.createXMLStreamReader((Document)source);
        } else {
            reader = StaxUtils.createXMLStreamReader(new DOMSource((Node)source));
        }
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, encoding);
        try {
            StaxUtils.copy(reader, writer);
        } catch (XMLStreamException e) {
            throw ExceptionUtils.toInternalServerErrorException(e, null);
        } finally {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                //ignore
            }
            try {
                writer.flush();
                writer.close();
            } catch (XMLStreamException e) {
                //ignore
            }
        }
    }

    public long getSize(T source, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mt) {
        return -1;
    }

    protected String getPreferredSource() {
        MessageContext mc = getContext();
        String source = null;
        if (mc != null) {
            source = (String)mc.getContextualProperty(PREFERRED_FORMAT);
        }
        return source != null ? source : "sax";

    }

    protected MessageContext getContext() {
        return context;
    }
}
