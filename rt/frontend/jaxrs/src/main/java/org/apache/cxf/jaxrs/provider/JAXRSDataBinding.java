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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.MessagePartInfo;

/**
 * CXF DataBinding implementation wrapping JAX-RS providers
 */
public class JAXRSDataBinding extends AbstractDataBinding {

    private static final Class<?>[] SUPPORTED_READER_FORMATS = new Class<?>[] {XMLStreamReader.class};
    private static final Class<?>[] SUPPORTED_WRITER_FORMATS = new Class<?>[] {XMLStreamWriter.class};

    private MessageBodyReader<?> xmlReader;
    private MessageBodyWriter<Object> xmlWriter;

    @SuppressWarnings("unchecked")
    public void setProvider(Object provider) {
        if (!(provider instanceof MessageBodyWriter)) {
            throw new IllegalArgumentException(
                "The provider must implement jakarta.ws.rs.ext.MessageBodyWriter");
        }
        xmlWriter = (MessageBodyWriter<Object>)provider;

        if (provider instanceof MessageBodyReader) {
            xmlReader = (MessageBodyReader<?>)provider;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(final Class<T> cls) {
        if (xmlReader == null) {
            throw new IllegalStateException(
                "jakarta.ws.rs.ext.MessageBodyReader reference is uninitialized");
        }
        return (DataReader<T>)new MessageBodyDataReader();
    }

    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(final Class<T> cls) {
        return (DataWriter<T>)new MessageBodyDataWriter();
    }

    public Class<?>[] getSupportedReaderFormats() {
        return SUPPORTED_READER_FORMATS;
    }

    public Class<?>[] getSupportedWriterFormats() {
        return SUPPORTED_WRITER_FORMATS;
    }

    public void initialize(Service service) {
        // Check how to deal with individual parts if needed, build a single JAXBContext, etc
    }

    @SuppressWarnings("unchecked")
    private MultivaluedMap<String, String> getHeaders(Message message) {
        return new MetadataMap<String, String>(
            (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS), true, true);
    }
    @SuppressWarnings("unchecked")
    private MultivaluedMap<String, Object> getWriteHeaders(Message message) {
        return new MetadataMap<String, Object>(
            (Map<String, List<Object>>)message.get(Message.PROTOCOL_HEADERS), true, true);
    }

    private final class MessageBodyDataWriter implements DataWriter<XMLStreamWriter> {

        public void write(Object obj, XMLStreamWriter output) {
            write(obj, null, output);
        }

        public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
            try {
                Message message = PhaseInterceptorChain.getCurrentMessage();
                Method method = MessageUtils.getTargetMethod(message).orElse(null);
                MultivaluedMap<String, Object> headers = getWriteHeaders(message);
                xmlWriter.writeTo(obj,
                                 method.getReturnType(),
                                 method.getGenericReturnType(),
                                 method.getAnnotations(),
                                 MediaType.APPLICATION_XML_TYPE,
                                 headers,
                                 null);
                message.put(Message.PROTOCOL_HEADERS, headers);
            } catch (Exception ex) {
                // ignore
            }
        }

        public void setAttachments(Collection<Attachment> attachments) {
            // complete
        }

        public void setProperty(String key, Object value) {
            // complete
        }

        public void setSchema(Schema s) {
            // complete
        }
    }

    private final class MessageBodyDataReader implements DataReader<XMLStreamReader> {

        public Object read(XMLStreamReader input) {
            throw new UnsupportedOperationException();
        }

        public Object read(MessagePartInfo part, XMLStreamReader input) {
            return doRead(part.getTypeClass(), input);
        }

        public Object read(QName elementQName, XMLStreamReader input, Class<?> type) {
            return doRead(type, input);
        }


        @SuppressWarnings("unchecked")
        private <T> T read(Class<T> cls) throws WebApplicationException, IOException {
            Message message = PhaseInterceptorChain.getCurrentMessage();
            Method method = MessageUtils.getTargetMethod(message).orElse(null);
            MessageBodyReader<T> reader = (MessageBodyReader<T>)xmlReader;

            return reader.readFrom(cls,
                                      method.getGenericParameterTypes()[0],
                                      method.getParameterTypes()[0].getAnnotations(),
                                      MediaType.APPLICATION_ATOM_XML_TYPE,
                                      getHeaders(message),
                                      null);
        }
        private Object doRead(Class<?> cls, XMLStreamReader input) {
            try {
                return read(cls);
            } catch (Exception ex) {
                return null;
            }
        }

        public void setAttachments(Collection<Attachment> attachments) {
            // complete
        }

        public void setProperty(String prop, Object value) {
            // complete
        }

        public void setSchema(Schema s) {
            // complete
        }

    };
}
