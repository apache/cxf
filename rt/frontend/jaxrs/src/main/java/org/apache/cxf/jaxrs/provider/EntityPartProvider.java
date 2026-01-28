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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import jakarta.activation.DataHandler;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.ext.multipart.MultipartOutputFilter;
import org.apache.cxf.jaxrs.impl.EntityPartImpl;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;

/**
 * Jakarta RESTful Web Services 3.1: 
 * {@code java.util.List<EntityPart>}, Multipart data ( multipart/form-data )
 */
@Consumes({ "multipart/form-data" })
@Produces({ "multipart/form-data" })
@Provider
public class EntityPartProvider extends AbstractConfigurableProvider
        implements MessageBodyReader<List<EntityPart>>, MessageBodyWriter<List<EntityPart>> {
    private static final Logger LOG = LogUtils.getL7dLogger(EntityPartProvider.class);

    @Context private Providers providers;
    @Context private MessageContext mc;
    private String attachmentDir;
    private String attachmentThreshold;
    private String attachmentMaxSize;

    public void setAttachmentDirectory(String dir) {
        attachmentDir = dir;
    }

    public void setAttachmentThreshold(String threshold) {
        attachmentThreshold = threshold;
    }

    public void setAttachmentMaxSize(String maxSize) {
        attachmentMaxSize = maxSize;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {

        if (List.class.isAssignableFrom(type) && ParameterizedType.class.isInstance(genericType)) {
            final Type arg = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            if (arg instanceof Class<?>) {
                return EntityPart.class.isAssignableFrom((Class<?>)arg);
            }
        }

        return false;
    }

    @Override
    public void writeTo(List<EntityPart> parts, Class<?> type, Type genericType,
            Annotation[] anns, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream os)
            throws IOException, WebApplicationException {

        final List<Attachment> handlers = convertToDataHandlers(parts, type, genericType, anns);
        if (mc.get(AttachmentUtils.OUT_FILTERS) != null) {
            List<MultipartOutputFilter> filters = CastUtils.cast((List<?>)mc.get(AttachmentUtils.OUT_FILTERS));
            for (MultipartOutputFilter filter : filters) {
                filter.filter(handlers);
            }
        }

        mc.put(MultipartBody.OUTBOUND_MESSAGE_ATTACHMENTS, handlers);
        handlers.get(0).getDataHandler().writeTo(os);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (List.class.isAssignableFrom(type) && ParameterizedType.class.isInstance(genericType)) {
            final Type arg = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            if (arg instanceof Class<?>) {
                return EntityPart.class.isAssignableFrom((Class<?>)arg);
            }
        }

        return false;
    }

    @Override
    public List<EntityPart> readFrom(final Class<List<EntityPart>> type, final Type genericType,
            final Annotation[] anns, final MediaType mediaType, final MultivaluedMap<String, String> headers,
                final InputStream is) throws IOException, WebApplicationException {

        checkContentLength();
        final List<Attachment> infos = AttachmentUtils.getAttachments(
                mc, attachmentDir, attachmentThreshold, attachmentMaxSize);

        final List<EntityPart> parts = new ArrayList<>();
        for (Attachment a : infos) {
            parts.add(fromAttachment(a, InputStream.class, genericType, anns, headers, mediaType));
        }

        return parts;
    }
    
    private <T> EntityPart fromAttachment(Attachment attachment, Class<T> c, final Type type, Annotation[] anns, 
                final MultivaluedMap<String, String> headers, final MediaType mediaType) throws IOException {

        final GenericType<T> genericType = (type != null) ? new GenericType<>(type) : null;
        final MessageBodyReader<T> r = providers.getMessageBodyReader(c, type, anns, attachment.getContentType());
        if (r != null) {
            final InputStream is = attachment.getDataHandler().getInputStream();
            final ContentDisposition cd = attachment.getContentDisposition();
            
            final String fileName = (cd != null) ? cd.getFilename() : null;
            final String cdName = cd == null ? null : cd.getParameter("name");
            final String contentId = attachment.getContentId();
            final String name = StringUtils.isEmpty(cdName) ? contentId : cdName.replace("\"", "").replace("'", "");

            if (!StringUtils.isEmpty(fileName)) {
                return new EntityPartImpl(mc.getProviders(), name, fileName, is, c, genericType, headers,
                    attachment.getContentType());
            } else {
                return new EntityPartImpl(mc.getProviders(), name, null, is, c, genericType, headers,
                    attachment.getContentType());
            }
        } else {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
    }

    protected void checkContentLength() {
        if (mc != null && isPayloadEmpty(mc.getHttpHeaders())) {
            String message = new org.apache.cxf.common.i18n.Message("EMPTY_BODY", BUNDLE).toString();
            LOG.warning(message);
            throw new WebApplicationException(400);
        }
    }

    private static <T> List<Attachment> convertToDataHandlers(final List<EntityPart> parts, final  Class<T> type,
            final Type genericType, final Annotation[] anns)  throws IOException {
        final List<Attachment> attachments = new ArrayList<>(parts.size());
        for (EntityPart part: parts) {
            attachments.add(createDataHandler(part, type, genericType, anns));
        }
        return attachments;
    }

    private static <T> Attachment createDataHandler(final EntityPart part, final Class<T> type,
            final Type genericType, final Annotation[] anns) throws IOException {

        final String mt = Objects
            .requireNonNullElse(part.getMediaType(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .toString();

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (part.getHeaders() != null) {
            headers.putAll(part.getHeaders());
        }

        return part.getFileName()
            .map(fileName -> {
                final ContentDisposition cd = new ContentDisposition("form-data;name=file;filename=" + fileName);
                return new Attachment(AttachmentUtil.BODY_ATTACHMENT_ID, part.getContent(), cd);
            })
            .orElseGet(() -> {
                headers.putSingle("Content-Type", mt);
                return new Attachment(part.getName(),
                    new DataHandler(new InputStreamDataSource(part.getContent(), mt)), headers);
            });
    }
}
