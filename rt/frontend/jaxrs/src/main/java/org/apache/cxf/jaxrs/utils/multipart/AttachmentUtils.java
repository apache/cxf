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

package org.apache.cxf.jaxrs.utils.multipart;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public final class AttachmentUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSUtils.class);
    
    private AttachmentUtils() {
    }
    
    public static MultipartBody getMultipartBody(MessageContext mc) {
        return (MultipartBody)mc.get(MultipartBody.INBOUND_MESSAGE_ATTACHMENTS);
    }
    
    public static Map<String, Attachment> getChildAttachmentsMap(MessageContext mc) {
        return fromListToMap(getChildAttachments(mc));
    }
    
    public static List<Attachment> getChildAttachments(MessageContext mc) {
        return ((MultipartBody)mc.get(MultipartBody.INBOUND_MESSAGE_ATTACHMENTS)).getChildAttachments();
    }
    
    public static Map<String, Attachment> getAttachmentsMap(MessageContext mc) {
        return fromListToMap(getAttachments(mc));
    }
    
    public static List<Attachment> getAttachments(MessageContext mc) {
        return ((MultipartBody)mc.get(MultipartBody.INBOUND_MESSAGE_ATTACHMENTS)).getAllAttachments();
    }
    
    public static MultipartBody getMultipartBody(MessageContext mc,
        String attachmentDir, String attachmentThreshold) {
        if (attachmentDir != null) {
            mc.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, attachmentDir);
        }
        if (attachmentThreshold != null) {
            mc.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, attachmentThreshold);
        }
        return (MultipartBody)mc.get(MultipartBody.INBOUND_MESSAGE_ATTACHMENTS);
    }
    
    public static List<Attachment> getAttachments(MessageContext mc, 
        String attachmentDir, String attachmentThreshold) {
        return getMultipartBody(mc, attachmentDir, attachmentThreshold).getAllAttachments();
    }
    
    public static Attachment getMultipart(Class<Object> c, Annotation[] anns, 
        MediaType mt, List<Attachment> infos) throws IOException {
        Multipart id = AnnotationUtils.getAnnotation(anns, Multipart.class);
        if (id != null) {
            for (Attachment a : infos) {
                if (a.getContentId().equals(id.value())) {
                    checkMediaTypes(a.getContentType(), id.type());
                    return a;    
                }
            }
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("MULTTIPART_ID_NOT_FOUND", 
                                                       BUNDLE, 
                                                       id.value(),
                                                       mt.toString());
            LOG.warning(errorMsg.toString());
            return null;
            
        }
        
        return infos.size() > 0 ? infos.get(0) : null; 
    }

    @SuppressWarnings("unchecked")
    public static <T> MultivaluedMap<String, T> populateFormMap(MessageContext mc, Class<T> cls) {
        MultivaluedMap<String, T> data = new MetadataMap<String, T>();
        FormUtils.populateMapFromMultipart((MultivaluedMap)data, 
                                           AttachmentUtils.getMultipartBody(mc), true);
        return data;
    }
    
    public static MultivaluedMap<String, String> populateFormMap(MessageContext mc) {
        return populateFormMap(mc, String.class);
    }
    
    private static Map<String, Attachment> fromListToMap(List<Attachment> atts) {
        Map<String, Attachment> map = new LinkedHashMap<String, Attachment>();
        for (Attachment a : atts) {
            map.put(a.getContentId(), a);    
        }
        return map;
    }
    
    private static void checkMediaTypes(MediaType mt1, String mt2) {
        if (!mt1.isCompatible(MediaType.valueOf(mt2))) {                                            
            throw new WebApplicationException(415);
        }
    }
}
