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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

@Produces({"application/x-www-form-urlencoded", "multipart/form-data" })
@Consumes({"application/x-www-form-urlencoded", "multipart/form-data" })
@Provider
public class FormEncodingProvider<T> extends AbstractConfigurableProvider
    implements MessageBodyReader<T>, MessageBodyWriter<T> {

    private FormValidator validator;
    @Context private MessageContext mc;
    private String attachmentDir;
    private String attachmentThreshold;
    private String attachmentMaxSize;

    private boolean expectEncoded;

    public FormEncodingProvider() {

    }

    public FormEncodingProvider(boolean expectEncoded) {
        this.expectEncoded = expectEncoded;
    }

    public void setExpectedEncoded(boolean expect) {
        this.expectEncoded = expect;
    }

    public void setAttachmentDirectory(String dir) {
        attachmentDir = dir;
    }

    public void setAttachmentThreshold(String threshold) {
        attachmentThreshold = threshold;
    }

    public void setAttachmentMaxSize(String maxSize) {
        attachmentMaxSize = maxSize;
    }

    public void setValidator(FormValidator formValidator) {
        validator = formValidator;
    }

    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mt) {
        return isSupported(type, mt);
    }

    public T readFrom(
        Class<T> clazz, Type genericType, Annotation[] annotations, MediaType mt,
        MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {
        if (is == null) {
            return null;
        }
        try {
            if (mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
                MultipartBody body = AttachmentUtils.getMultipartBody(mc);
                if (MultipartBody.class.isAssignableFrom(clazz)) {
                    return clazz.cast(body);
                } else if (Attachment.class.isAssignableFrom(clazz)) {
                    return clazz.cast(body.getRootAttachment());
                }
            }

            MultivaluedMap<String, String> params = createMap(clazz);
            populateMap(params, annotations, is, mt, !keepEncoded(annotations));
            validateMap(params);

            persistParamsOnMessage(params);

            return getFormObject(clazz, params);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtils.toBadRequestException(e, null);
        }
    }

    protected boolean keepEncoded(Annotation[] anns) {
        return AnnotationUtils.getAnnotation(anns, Encoded.class) != null
               || expectEncoded;
    }

    protected void persistParamsOnMessage(MultivaluedMap<String, String> params) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message != null) {
            message.put(FormUtils.FORM_PARAM_MAP, params);
        }
    }

    @SuppressWarnings("unchecked")
    protected MultivaluedMap<String, String> createMap(Class<?> clazz) throws Exception {
        if (clazz == MultivaluedMap.class || clazz == Form.class) {
            return new MetadataMap<String, String>();
        }
        return (MultivaluedMap<String, String>)clazz.newInstance();
    }

    private T getFormObject(Class<T> clazz, MultivaluedMap<String, String> params) {
        return clazz.cast(Form.class.isAssignableFrom(clazz) ? new Form(params) : params);
    }

    /**
     * Retrieve map of parameters from the passed in message
     */
    protected void populateMap(MultivaluedMap<String, String> params,
                               Annotation[] anns,
                               InputStream is,
                               MediaType mt,
                               boolean decode) {
        if (mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
            MultipartBody body =
                AttachmentUtils.getMultipartBody(mc, attachmentDir, attachmentThreshold, attachmentMaxSize);
            FormUtils.populateMapFromMultipart(params, body, PhaseInterceptorChain.getCurrentMessage(),
                                               decode);
        } else {
            String enc = HttpUtils.getEncoding(mt, StandardCharsets.UTF_8.name());

            Object servletRequest = mc != null ? mc.getHttpServletRequest() : null;
            if (servletRequest == null) {
                FormUtils.populateMapFromString(params,
                                                PhaseInterceptorChain.getCurrentMessage(),
                                                FormUtils.readBody(is, enc),
                                                enc,
                                                decode);
            } else {
                FormUtils.populateMapFromString(params,
                                                PhaseInterceptorChain.getCurrentMessage(),
                                                FormUtils.readBody(is, enc),
                                                enc,
                                                decode,
                                                (javax.servlet.http.HttpServletRequest)servletRequest);
            }
        }
    }

    protected void validateMap(MultivaluedMap<String, String> params) {
        if (validator != null) {
            validator.validate(params);
        }
    }

    public long getSize(T t, Class<?> type,
                        Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                               MediaType mt) {
        return isSupported(type, mt);
    }

    private static boolean isSupported(Class<?> type, MediaType mt) {
        return (MultivaluedMap.class.isAssignableFrom(type) || Form.class.isAssignableFrom(type))
            || (mt.getType().equalsIgnoreCase("multipart")
            && mt.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)
            && (MultivaluedMap.class.isAssignableFrom(type) || Form.class.isAssignableFrom(type)));
    }

    @SuppressWarnings("unchecked")
    public void writeTo(T obj, Class<?> c, Type t, Annotation[] anns,
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException, WebApplicationException {

        MultivaluedMap<String, String> map =
            (MultivaluedMap<String, String>)(obj instanceof Form ? ((Form)obj).asMap() : obj);
        boolean encoded = keepEncoded(anns);

        String enc = HttpUtils.getSetEncoding(mt, headers, StandardCharsets.UTF_8.name());

        FormUtils.writeMapToOutputStream(map, os, enc, encoded);

    }

}
