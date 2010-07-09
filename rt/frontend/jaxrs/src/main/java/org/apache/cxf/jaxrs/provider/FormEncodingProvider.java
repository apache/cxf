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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;

@Produces({"application/x-www-form-urlencoded", "multipart/form-data" })
@Consumes({"application/x-www-form-urlencoded", "multipart/form-data" })
@Provider
public class FormEncodingProvider implements 
    MessageBodyReader<Object>, MessageBodyWriter<Object> {
        
    private FormValidator validator;
    @Context private MessageContext mc;
    private String attachmentDir;
    private String attachmentThreshold;

    public void setAttachmentDirectory(String dir) {
        attachmentDir = dir;
    }
    
    public void setAttachmentThreshold(String threshold) {
        attachmentThreshold = threshold;
    }
    
    public void setValidator(FormValidator formValidator) {
        validator = formValidator;
    }
    
    public boolean isReadable(Class<?> type, Type genericType, 
                              Annotation[] annotations, MediaType mt) {
        return isSupported(type, genericType, annotations, mt);
    }

    public Object readFrom(
        Class<Object> clazz, Type genericType, Annotation[] annotations, MediaType mt, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        try {
            if (mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
                MultipartBody body = AttachmentUtils.getMultipartBody(mc);
                if (MultipartBody.class.isAssignableFrom(clazz)) {
                    return body;
                } else if (Attachment.class.isAssignableFrom(clazz)) {
                    return body.getRootAttachment();
                }  
            }
            
            MultivaluedMap<String, String> params = createMap(clazz);
            populateMap(params, is, mt,
                        AnnotationUtils.getAnnotation(annotations, Encoded.class) == null);
            validateMap(params);
            return params;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected MultivaluedMap<String, String> createMap(Class<?> clazz) throws Exception {
        if (clazz == MultivaluedMap.class) {
            return new MetadataMap<String, String>();
        }
        return (MultivaluedMap<String, String>)clazz.newInstance();
    }
    
    /**
     * Retrieve map of parameters from the passed in message
     *
     * @param message
     * @return a Map of parameters.
     */
    protected void populateMap(MultivaluedMap<String, String> params, 
                               InputStream is, MediaType mt, boolean decode) {
        if (mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
            MultipartBody body = 
                AttachmentUtils.getMultipartBody(mc, attachmentDir, attachmentThreshold);
            FormUtils.populateMapFromMultipart(params, body, decode);
        } else {
            FormUtils.populateMapFromString(params, 
                                            FormUtils.readBody(is, mt), 
                                            decode,
                                            mc != null ? mc.getHttpServletRequest() : null);
        }
    }
    
    protected void validateMap(MultivaluedMap<String, String> params) {
        if (validator != null) {
            validator.validate(params);
        }
    }

    public long getSize(Object t, Class<?> type, 
                        Type genericType, Annotation[] annotations, 
                        MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, 
                               MediaType mt) {
        return isSupported(type, genericType, annotations, mt)
            || mt.getType().equalsIgnoreCase("multipart")
            && mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE) && File.class == type;
    }

    private boolean isSupported(Class<?> type, Type genericType, Annotation[] annotations, 
                                MediaType mt) {
        return MultivaluedMap.class.isAssignableFrom(type)
            || mt.getType().equalsIgnoreCase("multipart") 
            && mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)
            && (MultipartBody.class.isAssignableFrom(type) || Attachment.class.isAssignableFrom(type));
    }
    
    @SuppressWarnings("unchecked")
    public void writeTo(Object obj, Class<?> c, Type t, Annotation[] anns, 
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException, WebApplicationException {
        
        if (mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
            Object body = obj;
            if (obj.getClass() == File.class) {
                File f = (File)obj;
                ContentDisposition cd = new ContentDisposition("attachment;filename=" + f.getName());
                body = new Attachment(AttachmentUtil.BODY_ATTACHMENT_ID, new FileInputStream(f), cd);
            }
            MultipartProvider provider = new MultipartProvider();
            provider.setMessageContext(mc);
            provider.writeTo(body, body.getClass(), body.getClass(), anns, mt, headers, os);
        } else {
            MultivaluedMap<String, String> map = (MultivaluedMap<String, String>)obj;
            boolean encoded = AnnotationUtils.getAnnotation(anns, Encoded.class) != null;
            
            String encoding = HttpUtils.getSetEncoding(mt, headers, "UTF-8");
            
            for (Iterator<Map.Entry<String, List<String>>> it = map.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, List<String>> entry = it.next();
                for (Iterator<String> entryIterator = entry.getValue().iterator(); entryIterator.hasNext();) {
                    String value = entryIterator.next();
                    os.write(entry.getKey().getBytes(encoding));
                    os.write('=');
                    String data = encoded ? value : HttpUtils.urlEncode(value);
                    os.write(data.getBytes(encoding));
                    if (entryIterator.hasNext() || it.hasNext()) {
                        os.write('&');
                    }
                }
    
            }
        }
    }

}
