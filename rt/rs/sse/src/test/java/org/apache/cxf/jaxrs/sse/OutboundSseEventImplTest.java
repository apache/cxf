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

package org.apache.cxf.jaxrs.sse;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;

import org.junit.Assert;
import org.junit.Test;

public class OutboundSseEventImplTest extends Assert {

    /**
     * Ensure that the <code>SseImpl</code> returns the correct builder class, 
     * <code>OutboundSseEventImpl.BuilderImpl.class</code>.
     */
    @Test
    public void testSseImplReturnsExpectedOutboundSseEventBuilder() {
        SseImpl sse = new SseImpl();
        assertEquals(sse.newEventBuilder().getClass(), OutboundSseEventImpl.BuilderImpl.class);
    }

    /**
     * A user should not need to specify a media type when creating an outbound event. The default
     * should be <code>MediaType.SERVER_SENT_EVENTS_TYPE</code>.
     */
    @Test
    public void testDefaultMediaType() {
        SseImpl sse = new SseImpl();
        
        // test newEvent(data)
        OutboundSseEvent event = sse.newEvent("myData");
        assertNull(event.getName());
        assertEquals("myData", event.getData());
        assertEquals(MediaType.SERVER_SENT_EVENTS_TYPE, event.getMediaType());
        
        // test newEvent(name, data)
        event = sse.newEvent("myName", "myData2");
        assertEquals("myName", event.getName());
        assertEquals("myData2", event.getData());
        assertEquals(MediaType.SERVER_SENT_EVENTS_TYPE, event.getMediaType());
        
        // test newEventBuilder()...build()
        event = sse.newEventBuilder().comment("myComment").data("myData3").build();
        assertEquals("myComment", event.getComment());
        assertEquals("myData3", event.getData());
        assertEquals(MediaType.SERVER_SENT_EVENTS_TYPE, event.getMediaType());
    }

    /**
     * A user should not need to specify the type of data being sent in an outbound
     * event. In fact, the API specifies that it must be a <code>String</code>. Other
     * types may be supported, but the default should be <code>String</code>.
     */
    @Test
    public void testDefaultClass() {
        SseImpl sse = new SseImpl();
        
        // test newEvent(data)
        OutboundSseEvent event = sse.newEvent("myData");
        assertNull(event.getName());
        assertEquals("myData", event.getData());
        assertEquals(String.class, event.getType());
        
        // test newEvent(name, data)
        event = sse.newEvent("myName", "myData2");
        assertEquals("myName", event.getName());
        assertEquals("myData2", event.getData());
        assertEquals(String.class, event.getType());
        
        // test newEventBuilder()...build()
        event = sse.newEventBuilder().comment("myComment").data("myData3").build();
        assertEquals("myComment", event.getComment());
        assertEquals("myData3", event.getData());
        assertEquals(String.class, event.getType());
    }

    @Test
    public void testBuilderAPIs() {
        SseImpl sse = new SseImpl();
        OutboundSseEvent.Builder builder = sse.newEventBuilder();
        builder.comment("myComment");
        builder.data("dataNoType");
        builder.id("id");
        builder.mediaType(MediaType.APPLICATION_JSON_TYPE);
        builder.name("name");
        builder.reconnectDelay(5000);
        OutboundSseEvent event = builder.build();
        assertEquals("myComment", event.getComment());
        assertEquals("dataNoType", event.getData());
        assertEquals(String.class, event.getType());
        assertEquals("id", event.getId());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, event.getMediaType());
        assertEquals("name", event.getName());
        assertEquals(5000, event.getReconnectDelay());

        // now reuse the builder to build a new event
        builder.comment("myComment2");
        builder.data(TestData.class, new TestData("data1", "data2"));
        builder.id("id2");
        builder.mediaType(MediaType.TEXT_PLAIN_TYPE);
        builder.name("name2");
        builder.reconnectDelay(9000);
        event = builder.build();
        assertEquals("myComment2", event.getComment());
        assertEquals(new TestData("data1", "data2"), event.getData());
        assertEquals(TestData.class, event.getType());
        assertEquals("id2", event.getId());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, event.getMediaType());
        assertEquals("name2", event.getName());
        assertEquals(9000, event.getReconnectDelay());
    }

    class TestData {
        final List<String> data = new ArrayList<>();
        TestData(String...entries) {
            for (String entry : entries) {
                data.add(entry);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TestData && ((TestData)o).data.size() == data.size()) {
                for (int i = 0; i < data.size(); i++) {
                    if (((TestData)o).data.get(i) != data.get(i)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }
    }
}
