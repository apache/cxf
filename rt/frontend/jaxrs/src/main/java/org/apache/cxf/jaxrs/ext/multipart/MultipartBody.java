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

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public class MultipartBody {

    public static final String INBOUND_MESSAGE_ATTACHMENTS = "org.apache.cxf.jaxrs.attachments.inbound";
    public static final String OUTBOUND_MESSAGE_ATTACHMENTS = "org.apache.cxf.jaxrs.attachments.outbound";

    private static final MediaType MULTIPART_RELATED_TYPE = JAXRSUtils.toMediaType("multipart/related");
    private List<Attachment> atts;
    private MediaType mt;

    public MultipartBody(Attachment att) {
        this(Collections.singletonList(att));
    }

    public MultipartBody(List<Attachment> atts) {
        this(atts, false);
    }

    public MultipartBody(List<Attachment> atts, boolean outbound) {
        this(atts, MULTIPART_RELATED_TYPE, outbound);
    }

    public MultipartBody(boolean outbound) {
        this(new ArrayList<>(), MULTIPART_RELATED_TYPE, outbound);
    }

    public MultipartBody(List<Attachment> atts, MediaType mt, boolean outbound) {
        this.atts = atts;
        this.mt = mt == null ? MULTIPART_RELATED_TYPE : mt;
    }

    public MediaType getType() {
        return mt;
    }

    public List<Attachment> getAllAttachments() {
        return atts;
    }

    public List<Attachment> getChildAttachments() {
        return atts.subList(1, atts.size());
    }

    public Attachment getRootAttachment() {
        return !atts.isEmpty() ? atts.get(0) : null;
    }

    public Attachment getAttachment(String contentId) {
        for (Attachment a : atts) {
            if (contentId.equalsIgnoreCase(a.getContentId())) {
                return a;
            }
            ContentDisposition cd = a.getContentDisposition();
            if (cd != null && contentId.equals(cd.getParameter("name"))) {
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
