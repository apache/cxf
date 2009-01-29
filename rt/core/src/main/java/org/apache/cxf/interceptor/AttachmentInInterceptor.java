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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class AttachmentInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(AttachmentInInterceptor.class);

    private static final List<String> TYPES = Collections.singletonList("multipart/related");

    /**
     * contruct the soap message with attachments from mime input stream
     * 
     * @param messageParam
     */
    
    public AttachmentInInterceptor() {
        super(Phase.RECEIVE);
    }
    
    public void handleMessage(Message message) {
        if (isGET(message)) {
            LOG.fine("AttachmentInInterceptor skipped in HTTP GET method");
            return;
        }
        
        String contentType = (String) message.get(Message.CONTENT_TYPE);
        if (AttachmentUtil.isTypeSupported(contentType, getSupportedTypes())) {
            AttachmentDeserializer ad = new AttachmentDeserializer(message, getSupportedTypes());
            try {
                ad.initializeAttachments();
            } catch (IOException e) {
                throw new Fault(e);
            }
        }
    }

    public void handleFault(Message messageParam) {
    }

    protected List<String> getSupportedTypes() {
        return TYPES;
    }
}
