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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.message.Attachment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class WrappedAttachmentsTest {

    @Test
    public void testCreateAndModify() {
        Map<String, DataHandler> content = new HashMap<>();
        content.put("att-1", new DataHandler(new ByteArrayDataSource("Hello world!".getBytes(), "text/plain")));
        content.put("att-2", new DataHandler(new ByteArrayDataSource("Hola mundo!".getBytes(), "text/plain")));
        WrappedAttachments attachments = new WrappedAttachments(content);
        Attachment att3 = new AttachmentImpl("att-3",
            new DataHandler(new ByteArrayDataSource("Bonjour tout le monde!".getBytes(), "text/plain")));

        assertEquals(2, attachments.size());
        assertFalse(attachments.isEmpty());

        assertTrue(attachments.containsAll(attachments));
        List<String> testCollection = new ArrayList<>();
        testCollection.add("Some value");
        assertFalse(attachments
            .stream()
            .map(Attachment::toString)
            .collect(Collectors.toList())
            .containsAll(testCollection));

        attachments.add(att3);
        assertEquals(3, attachments.size());

        attachments.add(att3);
        assertEquals(3, attachments.size());

        attachments.remove(att3);

        assertEquals(2, attachments.size());

        Attachment attx = attachments.iterator().next();

        attachments.remove(attx);

        assertEquals(1, attachments.size());

        Attachment[] atts = attachments.toArray(new Attachment[0]);
        assertEquals(1, atts.length);
        assertEquals("att-1".equals(attx.getId()) ? "att-2" : "att-1", atts[0].getId());

        atts = attachments.toArray(new Attachment[attachments.size()]);
        assertEquals(1, atts.length);
        assertEquals("att-1".equals(attx.getId()) ? "att-2" : "att-1", atts[0].getId());

        Object[] o = attachments.toArray();
        assertEquals(1, o.length);
        Attachment a = (Attachment)o[0];
        assertEquals("att-1".equals(attx.getId()) ? "att-2" : "att-1", a.getId());


        attachments.clear();
        assertTrue(attachments.isEmpty());
        assertTrue(content.isEmpty());
    }
}