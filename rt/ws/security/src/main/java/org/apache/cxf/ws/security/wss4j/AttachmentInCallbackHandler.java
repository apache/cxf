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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;

/**
 * A outbound CallbackHandler to be used to sign/encrypt SOAP Attachments.
 */
public class AttachmentInCallbackHandler implements CallbackHandler {
    
    private final SoapMessage soapMessage;
    
    public AttachmentInCallbackHandler(SoapMessage soapMessage) {
        this.soapMessage = soapMessage;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            Callback callback = callbacks[i];
            if (callback instanceof AttachmentRequestCallback) {
                AttachmentRequestCallback attachmentRequestCallback = (AttachmentRequestCallback) callback;

                List<org.apache.wss4j.common.ext.Attachment> attachmentList =
                    new ArrayList<org.apache.wss4j.common.ext.Attachment>();
                attachmentRequestCallback.setAttachments(attachmentList);

                org.apache.cxf.message.Attachment attachment = null;

                final Collection<org.apache.cxf.message.Attachment> attachments = 
                    soapMessage.getAttachments();
                // Calling LazyAttachmentCollection.size() here to force it to load the attachments
                if (attachments.size() > 0) {
                    for (Iterator<org.apache.cxf.message.Attachment> iterator = attachments.iterator(); 
                        iterator.hasNext();) {
                        attachment = iterator.next();
    
                        if (!attachmentRequestCallback.getAttachmentId().equals(attachment.getId())) {
                            continue;
                        }
    
                        org.apache.wss4j.common.ext.Attachment att =
                            new org.apache.wss4j.common.ext.Attachment();
                        att.setMimeType(attachment.getDataHandler().getContentType());
                        att.setId(attachment.getId());
                        att.setSourceStream(attachment.getDataHandler().getInputStream());
                        Iterator<String> headerIterator = attachment.getHeaderNames();
                        while (headerIterator.hasNext()) {
                            String next = headerIterator.next();
                            att.addHeader(next, attachment.getHeader(next));
                        }
                        attachmentList.add(att);
    
                        iterator.remove();
                    }
                }
            } else if (callback instanceof AttachmentResultCallback) {
                AttachmentResultCallback attachmentResultCallback = (AttachmentResultCallback) callback;

                final Collection<org.apache.cxf.message.Attachment> attachments = soapMessage.getAttachments();

                org.apache.cxf.attachment.AttachmentImpl securedAttachment =
                    new org.apache.cxf.attachment.AttachmentImpl(
                        attachmentResultCallback.getAttachmentId(),
                        new DataHandler(
                            new AttachmentDataSource(
                                attachmentResultCallback.getAttachment().getMimeType(),
                                attachmentResultCallback.getAttachment().getSourceStream())
                        )
                    );
                Map<String, String> headers = attachmentResultCallback.getAttachment().getHeaders();
                Iterator<Map.Entry<String, String>> iterator = headers.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> next = iterator.next();
                    securedAttachment.setHeader(next.getKey(), next.getValue());
                }
                attachments.add(securedAttachment);

            } else {
                throw new UnsupportedCallbackException(callback, "Unsupported callback");
            }
        }
    }


}
