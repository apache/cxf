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

package org.apache.cxf.transport.jms;

import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.jms.StreamMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JMSMessageUtilTest {

    @Test
    public void testGetEncoding() throws Exception {
        assertEquals("Get the wrong encoding", StandardCharsets.UTF_8.name(),
                JMSMessageUtils.getEncoding("text/xml; charset=utf-8"));
        assertEquals("Get the wrong encoding", StandardCharsets.UTF_8.name(),
                JMSMessageUtils.getEncoding("text/xml"));
        assertEquals("Get the wrong encoding", "GBK", JMSMessageUtils.getEncoding("text/xml; charset=GBK"));
        try {
            JMSMessageUtils.getEncoding("text/xml; charset=asci");
            fail("Expect the exception here");
        } catch (UnsupportedEncodingException ex) {
            // we should get the UnsupportedEncodingException here
        }
    }

    @Test
    public void testStreamMessageIsReportedAsUnsupportedByDefault() throws Exception {
        StreamMessage streamMessage = mockStreamMessage();

        Message inMessage = JMSMessageUtils.asCXFMessage(streamMessage,
                                                         JMSConstants.JMS_SERVER_REQUEST_HEADERS);

        assertEquals(Collections.singletonList("stream"),
                     getProtocolHeaders(inMessage).get(JMSConstants.JMS_MESSAGE_TYPE));
        assertNull(inMessage.getContent(Reader.class));
        InputStream in = inMessage.getContent(InputStream.class);
        assertNotNull(in);
        assertEquals(-1, in.read());
        // The body must not be read (and so not deserialized)
        verify(streamMessage, never()).readObject();
        verify(streamMessage, never()).readString();
        verify(streamMessage, never()).readBytes(any());
    }

    @Test
    public void testStreamMessageIsReadWhenAllowed() throws Exception {
        StreamMessage streamMessage = mockStreamMessage();
        when(streamMessage.readObject()).thenReturn("payload");

        Message inMessage = JMSMessageUtils.asCXFMessage(streamMessage,
                                                         JMSConstants.JMS_SERVER_REQUEST_HEADERS, true);

        assertEquals(Collections.singletonList(JMSConstants.TEXT_MESSAGE_TYPE),
                     getProtocolHeaders(inMessage).get(JMSConstants.JMS_MESSAGE_TYPE));
        assertNotNull(inMessage.getContent(Reader.class));
    }

    private static StreamMessage mockStreamMessage() throws Exception {
        StreamMessage streamMessage = mock(StreamMessage.class);
        when(streamMessage.getPropertyNames()).thenReturn(Collections.emptyEnumeration());
        return streamMessage;
    }

    private static Map<String, List<String>> getProtocolHeaders(Message inMessage) {
        return CastUtils.cast((Map<?, ?>)inMessage.get(Message.PROTOCOL_HEADERS));
    }
}
