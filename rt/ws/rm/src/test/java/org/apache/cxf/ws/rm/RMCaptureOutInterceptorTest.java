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

package org.apache.cxf.ws.rm;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class RMCaptureOutInterceptorTest {
    @Test
    public void testSaveAttachmentsWithNullIds() {
        Attachment original = new AttachmentImpl(null);
        Attachment identified = new AttachmentImpl("attachment-1");
        List<Attachment> originals = new ArrayList<>(List.of(original, identified));
        RMCaptureOutInterceptor.AttachmentSaveInterceptor interceptor =
            new RMCaptureOutInterceptor.AttachmentSaveInterceptor(originals);

        Attachment first = new AttachmentImpl(null);
        Attachment second = new AttachmentImpl(null);
        Message message = new MessageImpl();
        message.setAttachments(List.of(original, new AttachmentImpl("attachment-1"), first, second));
        interceptor.handleMessage(message);

        Object saved = message.get(RMMessageConstants.SAVED_ATTACHMENTS);
        assertEquals(List.of(original, identified, first, second), saved);
        assertEquals(2, originals.size());
        interceptor.handleMessage(message);
        assertSame(saved, message.get(RMMessageConstants.SAVED_ATTACHMENTS));
        assertEquals(List.of(original, identified, first, second), saved);
    }
}
