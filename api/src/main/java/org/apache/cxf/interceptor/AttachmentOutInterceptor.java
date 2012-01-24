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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.cxf.attachment.AttachmentSerializer;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class AttachmentOutInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String WRITE_ATTACHMENTS = "write.attachments";
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AttachmentOutInterceptor.class);

    private AttachmentOutEndingInterceptor ending = new AttachmentOutEndingInterceptor();
    
    public AttachmentOutInterceptor() {
        super(Phase.PRE_STREAM);
    }

    public void handleMessage(Message message) {
        
        // Make it possible to step into this process in spite of Eclipse
        // by declaring the Object.
        Object prop = message.getContextualProperty(org.apache.cxf.message.Message.MTOM_ENABLED); 
        boolean mtomEnabled = MessageUtils.isTrue(prop);
        boolean writeAtts = MessageUtils.isTrue(message.getContextualProperty(WRITE_ATTACHMENTS))
            || (message.getAttachments() != null && !message.getAttachments().isEmpty());
        
        if (!mtomEnabled && !writeAtts) {
            return;
        }
        if (message.getContent(OutputStream.class) == null) {
            return;
        }

        AttachmentSerializer serializer = 
            new AttachmentSerializer(message, getMultipartType(), getRootHeaders());
        serializer.setXop(mtomEnabled);
        
        try {
            serializer.writeProlog();
        } catch (IOException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("WRITE_ATTACHMENTS", BUNDLE), e);
        }        
        message.setContent(AttachmentSerializer.class, serializer);
        
        // Add a final interceptor to write attachements
        message.getInterceptorChain().add(ending);   
    }
   
    protected String getMultipartType() {
        return "multipart/related";
    }
    
    protected Map<String, List<String>> getRootHeaders() {
        return Collections.emptyMap();
    }
    
    public class AttachmentOutEndingInterceptor extends AbstractPhaseInterceptor<Message> {
        public AttachmentOutEndingInterceptor() {
            super(Phase.PRE_STREAM_ENDING);
        }

        public void handleMessage(Message message) {
            AttachmentSerializer ser = message.getContent(AttachmentSerializer.class);
            if (ser != null) {
                try {
                    ser.writeAttachments();
                } catch (IOException e) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("WRITE_ATTACHMENTS", BUNDLE), e);
                }
            }
        }

    }
    
    
}
