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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class EntityPartProviderTest {
    private final Providers providers = new Providers() {
        @Override
        public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type,
                Type genericType, Annotation[] annotations, MediaType mediaType) {
            return new BinaryDataProvider<>();
        }

        @Override
        public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type,
                Type genericType, Annotation[] annotations, MediaType mediaType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
            throw new UnsupportedOperationException();
        }
        
    };

    @SuppressWarnings("unchecked")
    @Test
    public void testChangingMaxAttachmentCount() throws Exception {
        final Exchange exchange = new ExchangeImpl();
        final EntityPartProvider p = new EntityPartProvider();

        StringBuilder sb = new StringBuilder(1000);
        sb.append("SomeHeader: foo\n")
            .append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Type: text/xml; charset=UTF-8\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n")
            .append('\n')
            .append("<envelope/>\n");

        // Add many attachments
        IntStream.range(0, 40).forEach(i -> {
            sb.append("------=_Part_34950_1098328613.1263781527359\n")
                .append("Content-Type: text/xml\n")
                .append("Content-Transfer-Encoding: binary\n")
                .append("Content-Id: <b86a5f2d-e7af-4e5e-b71a-9f6f2307cab0>\n")
                .append('\n')
                .append("<message>\n")
                .append("------=_Part_34950_1098328613.1263781527359--\n");
        });

        // Too many attachments we'll not allow it
        final Message msg = new MessageImpl();
        msg.setExchange(exchange);
        exchange.setInMessage(msg);
        p.setMessageContext(new MessageContextImpl(msg));
        p.setProviders(providers);

        msg.put(AttachmentDeserializer.ATTACHMENT_MAX_COUNT, "30");
        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");

        assertThrows("Failure expected on too many attachments", RuntimeException.class,
                () -> p.readFrom((Class<List<EntityPart>>) (Class<?>) List.class, EntityPart.class, new Annotation[]{},
                    MediaType.APPLICATION_OCTET_STREAM_TYPE,
                    new MetadataMap<String, String>(),
                    msg.getContent(InputStream.class)));

        // Now we'll allow it
        final Message msg2 = new MessageImpl();
        msg2.setExchange(exchange);
        exchange.setInMessage(msg2);
        p.setMessageContext(new MessageContextImpl(msg2));

        msg2.put(AttachmentDeserializer.ATTACHMENT_MAX_COUNT, "60");
        msg2.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg2.put(Message.CONTENT_TYPE, "multipart/related");

        List<EntityPart> body = p.readFrom((Class<List<EntityPart>>) (Class<?>) List.class, 
            EntityPart.class, new Annotation[]{},
            MediaType.APPLICATION_OCTET_STREAM_TYPE,
            new MetadataMap<String, String>(),
            msg2.getContent(InputStream.class));

        // Force it to load the attachments
        assertEquals(41, body.size());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testManyAttachmentHeaders() throws Exception {
        final Exchange exchange = new ExchangeImpl();
        final EntityPartProvider p = new EntityPartProvider();

        StringBuilder sb = new StringBuilder(10000);
        // Add many attachment headers
        sb.append("------=_Part_34950_1098328613.1263781527359\n");
        IntStream.range(0, 1000).forEach(i -> sb.append("Header-").append(i).append(": foo").append(i).append('\n'));
        sb.append("Content-Type: text/xml; charset=UTF-8\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n")
            .append('\n')
            .append("<envelope/>\n");

        final Message msg = new MessageImpl();
        msg.setExchange(exchange);
        exchange.setInMessage(msg);
        p.setMessageContext(new MessageContextImpl(msg));

        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");

        assertThrows("Failure expected on too many attachment headers", RuntimeException.class,
                () -> p.readFrom((Class<List<EntityPart>>) (Class<?>) List.class, 
                    EntityPart.class, new Annotation[]{},
                    MediaType.APPLICATION_OCTET_STREAM_TYPE,
                    new MetadataMap<String, String>(),
                    msg.getContent(InputStream.class)));
    }
}
