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
package org.apache.cxf.jaxrs.ext.multipart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;

public class MultipartBody {
    
    public static final String INBOUND_MESSAGE_ATTACHMENTS = "org.apache.cxf.jaxrs.attachments.inbound";
    public static final String OUTBOUND_MESSAGE_ATTACHMENTS = "org.apache.cxf.jaxrs.attachments.outbound";
    
    private static final MediaType MULTIPART_RELATED_TYPE = MediaType.valueOf("multipart/related"); 
    private boolean outbound;
    private List<Attachment> atts;
    private MediaType mt; 
    
    public MultipartBody(List<Attachment> atts, MediaType mt, boolean outbound) {
        this.atts = atts;
        this.outbound = outbound;
        this.mt = mt == null ? MULTIPART_RELATED_TYPE : mt;
    }
    
    public MultipartBody(List<Attachment> atts, boolean outbound) {
        this(atts, MULTIPART_RELATED_TYPE, outbound);
    }
    
    public MultipartBody(Attachment att) {
        atts = new ArrayList<Attachment>();
        atts.add(att);
        outbound = true;
        this.mt = MULTIPART_RELATED_TYPE;
    }
    
    public MultipartBody(List<Attachment> atts) {
        this(atts, MULTIPART_RELATED_TYPE, false);
    }
    
    public MultipartBody(boolean outbound) {
        this(new ArrayList<Attachment>(), MULTIPART_RELATED_TYPE, outbound);
    }
    
    public MediaType getType() {
        return mt;
    }
    
    public List<Attachment> getAllAttachments() {
        
        return outbound ? atts : Collections.unmodifiableList(atts);
    }
    
    public List<Attachment> getChildAttachments() {
        List<Attachment> childAtts = new ArrayList<Attachment>();
        for (int i = 1; i < atts.size(); i++) {
            childAtts.add(atts.get(i));
        }
        return childAtts;
    }
    
    public Attachment getRootAttachment() {
        return atts.size() > 0 ? atts.get(0) : null;
    }
    
    public Attachment getAttachment(String contentId) {
        for (Attachment a : atts) {
            if (contentId.equalsIgnoreCase(a.getContentId())) {
                return a;
            }
        }
        return null;
    }
    
    public <T> T getAttachmentObject(String contentId, Class<T> cls) {
        Attachment att = getAttachment(contentId);
        if (att != null) {
            return att.getObject(cls);
        }
        return null;
    }
}
