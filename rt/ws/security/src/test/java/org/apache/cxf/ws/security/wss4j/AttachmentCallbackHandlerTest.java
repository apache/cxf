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

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.security.auth.callback.Callback;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.message.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.dom.engine.WSSConfig;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        DataHandler dataHandler = EasyMock.mock(DataHandler.class);
        dataHandler.setCommandMap(anyObject(CommandMap.class));
        EasyMock.expectLastCall();
        EasyMock.expect(dataHandler.getInputStream()).andReturn(null);
        EasyMock.expect(dataHandler.getContentType()).andReturn(null);
        EasyMock.replay(dataHandler);

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
        EasyMock.verify(dataHandler);
    }

}
