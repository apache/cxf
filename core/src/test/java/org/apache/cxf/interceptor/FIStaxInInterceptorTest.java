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
package org.apache.cxf.interceptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.stream.XMLStreamReader;

import com.sun.xml.fastinfoset.stax.StAXDocumentSerializer;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class FIStaxInInterceptorTest {

    @Test
    public void testReadDocument() throws Exception {
        XMLStreamReader reader = parse(nested(10), null, null);
        int elements = 0;
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamReader.START_ELEMENT) {
                elements++;
            }
        }
        assertEquals(10, elements);
    }

    @Test
    public void testDepthLimit() throws Exception {
        // the default depth limit is 100
        readAll(parse(nested(99), null, null));
        assertThrows(RuntimeException.class, () -> readAll(parse(nested(100), null, null)));
    }

    @Test
    public void testDeepNestingRejected() throws Exception {
        XMLStreamReader reader = parse(nested(100_000), null, null);
        assertThrows(RuntimeException.class, () -> readAll(reader));
    }

    @Test
    public void testChildElementLimit() throws Exception {
        // the default limit on child elements is 50000
        readAll(parse(children(49_999), null, null));
        assertThrows(RuntimeException.class, () -> readAll(parse(children(50_000), null, null)));
    }

    @Test
    public void testLimitFromMessage() throws Exception {
        readAll(parse(nested(150), StaxUtils.MAX_ELEMENT_DEPTH, "200"));
        assertThrows(RuntimeException.class, () -> readAll(parse(nested(20), StaxUtils.MAX_ELEMENT_DEPTH, "10")));
    }

    private static XMLStreamReader parse(byte[] fi, String key, String value) {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);
        message.setExchange(exchange);
        message.setInterceptorChain(new PhaseInterceptorChain(new java.util.TreeSet<>()));
        message.put(Message.CONTENT_TYPE, "application/fastinfoset");
        if (key != null) {
            message.put(key, value);
        }
        message.setContent(InputStream.class, new ByteArrayInputStream(fi));
        new FIStaxInInterceptor().handleMessage(message);
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);
        assertNotNull(reader);
        return reader;
    }

    private static void readAll(XMLStreamReader reader) throws Exception {
        while (reader.hasNext()) {
            reader.next();
        }
    }

    private static byte[] nested(int depth) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StAXDocumentSerializer writer = new StAXDocumentSerializer(out);
        writer.writeStartDocument();
        for (int i = 0; i < depth; i++) {
            writer.writeStartElement("a");
        }
        for (int i = 0; i < depth; i++) {
            writer.writeEndElement();
        }
        writer.writeEndDocument();
        writer.close();
        return out.toByteArray();
    }

    private static byte[] children(int count) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StAXDocumentSerializer writer = new StAXDocumentSerializer(out);
        writer.writeStartDocument();
        writer.writeStartElement("root");
        for (int i = 0; i < count; i++) {
            writer.writeEmptyElement("a");
        }
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        return out.toByteArray();
    }
}
