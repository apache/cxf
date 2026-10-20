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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Saves the attachments before they are replaced by the secured ones of WS-Security, which happens in
 * the POST_PROTOCOL phase, or while the message is written with the streaming WS-Security. The
 * attachments added by the marshalling, like the MTOM ones, are saved in the POST_MARSHAL phase, but
 * not the secured ones, which have the IDs of the attachments they replace.
 */
public class AttachmentSaveInterceptor extends AbstractPhaseInterceptor<Message> {
    private final List<Attachment> attachments;
    private final String propertyName;

    /**
     * @param attachments original attachments to snapshot before security processing
     * @param propertyName message property in which to save the original and marshalled attachments
     */
    public AttachmentSaveInterceptor(Collection<Attachment> attachments, String propertyName) {
        super(Phase.POST_MARSHAL);
        this.propertyName = propertyName;
        this.attachments = attachments == null ? new ArrayList<>() : new ArrayList<>(attachments);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        if (message.getAttachments() == null) {
            return;
        }
        Set<String> ids = new HashSet<>();
        for (Attachment a : attachments) {
            ids.add(a.getId());
        }
        for (Attachment a : message.getAttachments()) {
            if (ids.add(a.getId())) {
                attachments.add(a);
            }
        }
        message.put(propertyName, attachments);
    }
}
