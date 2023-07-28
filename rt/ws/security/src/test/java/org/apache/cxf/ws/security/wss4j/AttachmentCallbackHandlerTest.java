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
package org.apache.cxf.ws.security.wss4j;


import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;

import jakarta.activation.CommandMap;
import jakarta.activation.DataHandler;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.message.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.dom.engine.WSSConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AttachmentCallbackHandlerTest {

    static {
        WSSConfig.init();
    }

    @Test
    public void testUnencodedAttachmentId() throws Exception {
        String attachmentId = "998c3362-5b5f-405a-817a-b20f8373c378-5@urn:be:cin:nip:async:generic";
        parseAttachment(attachmentId);
    }

    @Test
    public void testEncodedAttachmentId() throws Exception {
        String attachmentId = "998c3362-5b5f-405a-817a-b20f8373c378-5@urn%3Abe%3Acin%3Anip%3Aasync%3Ageneric";
        parseAttachment(attachmentId);
    }

    private void parseAttachment(String attachmentId) throws Exception {
        Attachment attachment = new AttachmentImpl(attachmentId);

        // Mock up a DataHandler for the Attachment
        DataHandler dataHandler = mock(DataHandler.class);
        doCallRealMethod().when(dataHandler).setCommandMap(any(CommandMap.class));
        when(dataHandler.getInputStream()).thenReturn(null);
        when(dataHandler.getContentType()).thenReturn(null);

        ((AttachmentImpl)attachment).setDataHandler(dataHandler);
        AttachmentCallbackHandler callbackHandler =
                new AttachmentCallbackHandler(Collections.singletonList(attachment));

        AttachmentRequestCallback attachmentRequestCallback = new AttachmentRequestCallback();
        attachmentRequestCallback.setAttachmentId(AttachmentUtils.getAttachmentId("cid:" + attachmentId));
        attachmentRequestCallback.setRemoveAttachments(false);

        callbackHandler.handle(new Callback[]{attachmentRequestCallback});

        List<org.apache.wss4j.common.ext.Attachment> attachments = attachmentRequestCallback.getAttachments();
        assertNotNull(attachments);
        assertEquals(1, attachments.size());
    }

}
