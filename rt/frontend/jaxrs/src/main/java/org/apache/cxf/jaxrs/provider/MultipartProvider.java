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

package org.apache.cxf.jaxrs.provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;

@Provider
@Consumes({"multipart/related", "multipart/mixed", "multipart/alternative" })
public class MultipartProvider implements MessageBodyReader<Object> {

    @Context
    private MessageContext mc;
    private String attachmentDir;
    private String attachmentThreshold;

    public void setAttachmentDirectory(String dir) {
        attachmentDir = dir;
    }
    
    public void setAttachmentThreshold(String threshold) {
        attachmentThreshold = threshold;
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, 
                              MediaType mt) {
        if (DataHandler.class.isAssignableFrom(type) || DataSource.class.isAssignableFrom(type)
            || Attachment.class.isAssignableFrom(type) || MultipartBody.class.isAssignableFrom(type)
            || mediaTypeSupported(mt)) {
            return true;
        }
        return false;
    }

    public Object readFrom(Class<Object> c, Type t, Annotation[] anns, MediaType mt, 
                           MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException, WebApplicationException {
        
        List<Attachment> infos = 
            AttachmentUtils.getAttachments(mc, attachmentDir, attachmentThreshold);
        
        if (List.class.isAssignableFrom(c)) {
            Class<?> actual = InjectionUtils.getActualType(t);
            if (actual.isAssignableFrom(Attachment.class)) {
                return infos;
            }
            List<Object> objects = new ArrayList<Object>();
            for (Attachment a : infos) {
                objects.add(fromAttachment(a, actual, actual, anns));
            }
            return objects;
        }
        if (MultipartBody.class.isAssignableFrom(c)) {
            return new MultipartBody(infos);
        }
        
        Attachment multipart = AttachmentUtils.getMultipart(c, anns, mt, infos);
        if (multipart != null) {
            return fromAttachment(multipart, c, t, anns);
        }
        throw new WebApplicationException(404);
    }
    
    @SuppressWarnings("unchecked")
    private Object fromAttachment(Attachment multipart, Class<?> c, Type t, Annotation anns[]) 
        throws IOException {
        if (DataHandler.class.isAssignableFrom(c)) {
            return multipart.getDataHandler();
        } else if (DataSource.class.isAssignableFrom(c)) {
            return multipart.getDataHandler().getDataSource();
        } else if (Attachment.class.isAssignableFrom(c)) {
            return multipart;
        } else {
            MessageBodyReader<Object> r = 
                mc.getProviders().getMessageBodyReader((Class)c, t, anns, multipart.getContentType());
            if (r != null) {
                return r.readFrom((Class)c, t, anns, multipart.getContentType(), multipart.getHeaders(), 
                                  multipart.getDataHandler().getInputStream());
            }
        }
        return null;
    }
    
    private boolean mediaTypeSupported(MediaType mt) {
        return mt.getType().equals("multipart") && (mt.getSubtype().equals("related") 
            || mt.getSubtype().equals("mixed") || mt.getSubtype().equals("alternative"));
    }
}
