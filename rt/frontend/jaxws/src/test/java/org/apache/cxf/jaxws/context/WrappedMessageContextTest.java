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

package org.apache.cxf.jaxws.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.MessageContext.Scope;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class WrappedMessageContextTest {
    @Test
    public void testPutAndGetJaxwsAttachments() throws Exception {
        WrappedMessageContext context =
            new WrappedMessageContext(new HashMap<String, Object>(), null, Scope.APPLICATION);

        DataHandler dh1 = new DataHandler(new ByteArrayDataSource("Hello world!".getBytes(), "text/plain"));
        DataHandler dh2 = new DataHandler(new ByteArrayDataSource("Hola mundo!".getBytes(), "text/plain"));
        DataHandler dh3 = new DataHandler(new ByteArrayDataSource("Bonjour tout le monde!".getBytes(), "text/plain"));
        Map<String, DataHandler> jattachments = new HashMap<>();
        context.put(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS, jattachments);

        jattachments.put("attachment-1", dh1);

        Set<Attachment> cattachments = CastUtils.cast((Set<?>)context.get(Message.ATTACHMENTS));
        assertNotNull(cattachments);

        assertEquals(1, cattachments.size());

        jattachments.put("attachment-2", dh2);

        assertEquals(2, cattachments.size());

        AttachmentImpl ca = new AttachmentImpl("attachment-3", dh3);
        ca.setHeader("X-test", "true");
        cattachments.add(ca);

        assertEquals(3, jattachments.size());
        assertEquals(3, cattachments.size());
        for (Attachment a : cattachments) {
            if ("attachment-1".equals(a.getId())) {
                assertEquals("Hello world!", a.getDataHandler().getContent());
            } else if ("attachment-2".equals(a.getId())) {
                assertEquals("Hola mundo!", a.getDataHandler().getContent());
            } else if ("attachment-3".equals(a.getId())) {
                assertEquals("Bonjour tout le monde!", a.getDataHandler().getContent());
                assertEquals("true", a.getHeader("X-test"));
            } else {
                fail("unknown attachment");
            }
        }
    }
    
    
    @Test
    public void testContainsKey() throws Exception {
        WrappedMessageContext context =
            new WrappedMessageContext(new HashMap<String, Object>(), null, Scope.APPLICATION);

        Map<String, List<String>> headers = new HashMap<>();
        context.put(MessageContext.HTTP_REQUEST_HEADERS, headers);

        assertNotNull(context.get(MessageContext.HTTP_REQUEST_HEADERS));

        assertTrue(context.containsKey(MessageContext.HTTP_REQUEST_HEADERS));
    }
}