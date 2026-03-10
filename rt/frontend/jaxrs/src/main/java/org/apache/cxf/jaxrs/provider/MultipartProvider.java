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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.attachment.ByteDataSource;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.ext.multipart.MultipartOutputFilter;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;
import org.apache.cxf.message.Message;

@Provider
@Consumes({"multipart/related", "multipart/mixed", "multipart/alternative", "multipart/form-data" })
@Produces({"multipart/related", "multipart/mixed", "multipart/alternative", "multipart/form-data" })
public class MultipartProvider extends AbstractConfigurableProvider
    implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    private static final String SUPPORT_TYPE_AS_MULTIPART = "support.type.as.multipart";
    private static final String SINGLE_PART_IS_COLLECTION = "single.multipart.is.collection";
    private static final Logger LOG = LogUtils.getL7dLogger(MultipartProvider.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(MultipartProvider.class);
    private static final Set<Class<?>> WELL_KNOWN_MULTIPART_CLASSES = new HashSet<>();
    private static final Set<String> MULTIPART_SUBTYPES = new HashSet<>();
    static {
        WELL_KNOWN_MULTIPART_CLASSES.add(MultipartBody.class);
        WELL_KNOWN_MULTIPART_CLASSES.add(Attachment.class);

        MULTIPART_SUBTYPES.add("form-data");
        MULTIPART_SUBTYPES.add("mixed");
        MULTIPART_SUBTYPES.add("related");
        MULTIPART_SUBTYPES.add("alternative");
    }

    @Context
    private MessageContext mc;
    private String attachmentDir;
    private String attachmentThreshold;
    private String attachmentMaxSize;
    
    @Context
    private HttpHeaders httpHeaders;

    public void setMessageContext(MessageContext context) {
        this.mc = context;
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

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                              MediaType mt) {
        return isSupported(type, annotations, mt);

    }

    private boolean isSupported(Class<?> type, Annotation[] anns, MediaType mt) {
        return mediaTypeSupported(mt)
            && (WELL_KNOWN_MULTIPART_CLASSES.contains(type)
                || Collection.class.isAssignableFrom(type)
                || Map.class.isAssignableFrom(type) && type != MultivaluedMap.class
                || AnnotationUtils.getAnnotation(anns, Multipart.class) != null
                || PropertyUtils.isTrue(mc.getContextualProperty(SUPPORT_TYPE_AS_MULTIPART)));
    }

    protected void checkContentLength() {
        if (mc != null && isPayloadEmpty(mc.getHttpHeaders())) {
            String message = new org.apache.cxf.common.i18n.Message("EMPTY_BODY", BUNDLE).toString();
            LOG.warning(message);
            throw new WebApplicationException(400);
        }
    }

    public Object readFrom(Class<Object> c, Type t, Annotation[] anns, MediaType mt,
                           MultivaluedMap<String, String> headers, InputStream is)
        throws IOException, WebApplicationException {
        checkContentLength();
        List<Attachment> infos = AttachmentUtils.getAttachments(
                mc, attachmentDir, attachmentThreshold, attachmentMaxSize);

        boolean collectionExpected = Collection.class.isAssignableFrom(c);
        if (collectionExpected
            && AnnotationUtils.getAnnotation(anns, Multipart.class) == null) {
            return getAttachmentCollection(t, infos, anns);
        }
        if (c.isAssignableFrom(Map.class)) {
            Map<String, Object> map = new LinkedHashMap<>(infos.size());
            Class<?> actual = getActualType(t, 1);
            for (Attachment a : infos) {
                map.put(a.getContentType().toString(), fromAttachment(a, actual, actual, anns));
            }
            return map;
        }
        if (MultipartBody.class.isAssignableFrom(c)) {
            return new MultipartBody(infos);
        }

        Multipart id = AnnotationUtils.getAnnotation(anns, Multipart.class);
        Attachment multipart = AttachmentUtils.getMultipart(id, mt, infos);
        if (multipart != null) {
            if (collectionExpected
                && !mediaTypeSupported(multipart.getContentType())
                && !PropertyUtils.isTrue(mc.getContextualProperty(SINGLE_PART_IS_COLLECTION))) {
                List<Attachment> allMultiparts = AttachmentUtils.getMatchingAttachments(id, infos);
                return getAttachmentCollection(t, allMultiparts, anns);
            }
            return fromAttachment(multipart, c, t, anns);
        }

        if (id != null && !id.required()) {
            /*
             * Return default value for a missing optional part
             */
            Object defaultValue = null;
            if (c.isPrimitive()) {
                defaultValue = PrimitiveUtils.read((Class<?>)c == boolean.class ? "false" : "0", c);
            }
            return defaultValue;
        }

        throw ExceptionUtils.toBadRequestException(null, null);

    }

    private Object getAttachmentCollection(Type t, List<Attachment> infos, Annotation[] anns) throws IOException {
        Class<?> actual = getActualType(t, 0);
        if (Attachment.class.isAssignableFrom(actual)) {
            return infos;
        }
        Collection<Object> objects = new ArrayList<>();
        for (Attachment a : infos) {
            objects.add(fromAttachment(a, actual, actual, anns));
        }
        return objects;
    }

    private Class<?> getActualType(Type type, int pos) {
        Class<?> actual = null;
        try {
            actual = InjectionUtils.getActualType(type, pos);
        } catch (Exception ex) {
            // ignore;
        }
        return actual != null && actual != Object.class ? actual : Attachment.class;
    }

    private <T> Object fromAttachment(Attachment multipart, Class<T> c, Type t, Annotation[] anns)
        throws IOException {
        if (DataHandler.class.isAssignableFrom(c)) {
            return multipart.getDataHandler();
        } else if (DataSource.class.isAssignableFrom(c)) {
            return multipart.getDataHandler().getDataSource();
        } else if (Attachment.class.isAssignableFrom(c)) {
            return multipart;
        } else {
            if (mediaTypeSupported(multipart.getContentType())) {
                mc.put("org.apache.cxf.multipart.embedded", true);
                mc.put("org.apache.cxf.multipart.embedded.ctype", multipart.getContentType());
                mc.put("org.apache.cxf.multipart.embedded.input",
                       multipart.getDataHandler().getInputStream());
                anns = new Annotation[]{};
            }
            MessageBodyReader<T> r =
                mc.getProviders().getMessageBodyReader(c, t, anns, multipart.getContentType());
            if (r != null) {
                InputStream is = multipart.getDataHandler().getInputStream();
                return r.readFrom(c, t, anns, multipart.getContentType(), multipart.getHeaders(),
                                  is);
            }
        }
        return null;
    }

    private boolean mediaTypeSupported(MediaType mt) {
        return "multipart".equals(mt.getType()) && MULTIPART_SUBTYPES.contains(mt.getSubtype());
    }

    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                               MediaType mt) {
        return isSupported(type, annotations, mt);
    }


    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException, WebApplicationException {

        List<Attachment> handlers = convertToDataHandlers(obj, type, genericType, anns, mt);
        if (mc.get(AttachmentUtils.OUT_FILTERS) != null) {
            List<MultipartOutputFilter> filters = CastUtils.cast((List<?>)mc.get(AttachmentUtils.OUT_FILTERS));
            for (MultipartOutputFilter filter : filters) {
                filter.filter(handlers);
            }
        }
        mc.put(MultipartBody.OUTBOUND_MESSAGE_ATTACHMENTS, handlers);
        handlers.get(0).getDataHandler().writeTo(os);
    }

    private List<Attachment> convertToDataHandlers(Object obj,
                                                   Class<?> type, Type genericType,
                                                   Annotation[] anns, MediaType mt)  throws IOException {
        if (Map.class.isAssignableFrom(obj.getClass())) {
            Map<Object, Object> objects = CastUtils.cast((Map<?, ?>)obj);
            List<Attachment> handlers = new ArrayList<>(objects.size());
            int i = 0;
            for (Iterator<Map.Entry<Object, Object>> iter = objects.entrySet().iterator();
                iter.hasNext();) {
                Map.Entry<Object, Object> entry = iter.next();
                Object value = entry.getValue();
                Attachment handler = createDataHandler(value, value.getClass(),
                                                       new Annotation[]{},
                                                       entry.getKey().toString(),
                                                       mt.toString(),
                                                       i++);
                handlers.add(handler);
            }
            return handlers;
        }
        String rootMediaType = getRootMediaType(anns, mt);
        if (List.class.isAssignableFrom(obj.getClass())) {
            return getAttachments((List<?>)obj, rootMediaType);
        }
        if (MultipartBody.class.isAssignableFrom(type)) {
            List<Attachment> atts = ((MultipartBody)obj).getAllAttachments();
            // these attachments may have no DataHandlers, but objects only
            return getAttachments(atts, rootMediaType);
        }
        Attachment handler = createDataHandler(obj,
                                               genericType, anns,
                                               rootMediaType, mt.toString(), 1);
        return Collections.singletonList(handler);
    }

    private List<Attachment> getAttachments(List<?> objects, String rootMediaType) throws IOException {
        List<Attachment> handlers = new ArrayList<>(objects.size());
        for (int i = 0; i < objects.size(); i++) {
            Object value = objects.get(i);
            Attachment handler = createDataHandler(value,
                                           value.getClass(), new Annotation[]{},
                                           rootMediaType, rootMediaType, i);
            handlers.add(handler);
        }
        return handlers;
    }

    private <T> Attachment createDataHandler(T obj,
                                             Type genericType,
                                             Annotation[] anns,
                                             String mimeType,
                                             String mainMediaType,
                                             int id) throws IOException {
        @SuppressWarnings("unchecked")
        Class<T> cls = (Class<T>)obj.getClass();
        return createDataHandler(obj, cls, genericType, anns, mimeType, mainMediaType, id);
    }
    private <T> Attachment createDataHandler(T obj,
                                         Class<T> cls,
                                         Type genericType,
                                         Annotation[] anns,
                                         String mimeType,
                                         String mainMediaType,
                                         int id) throws IOException {
        final DataHandler dh;
        if (InputStream.class.isAssignableFrom(obj.getClass())) {
            dh = createInputStreamDH((InputStream)obj, mimeType);
        } else if (DataHandler.class.isAssignableFrom(obj.getClass())) {
            dh = (DataHandler)obj;
        } else if (DataSource.class.isAssignableFrom(obj.getClass())) {
            dh = new DataHandler((DataSource)obj);
        } else if (File.class.isAssignableFrom(obj.getClass())) {
            File f = (File)obj;
            ContentDisposition cd = mainMediaType.startsWith(MediaType.MULTIPART_FORM_DATA)
                ? new ContentDisposition("form-data;name=file;filename=" + f.getName()) :  null;
            return new Attachment(AttachmentUtil.BODY_ATTACHMENT_ID, Files.newInputStream(f.toPath()), cd);
        } else if (Attachment.class.isAssignableFrom(obj.getClass())) {
            Attachment att = (Attachment)obj;
            if (att.getObject() == null) {
                return att;
            }
            dh = getHandlerForObject(att.getObject(),
                                     att.getObject().getClass(), new Annotation[]{},
                                     att.getContentType().toString(), id);
            MediaType mediaType = httpHeaders.getMediaType();
            Attachment ret = null;
            if (MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(mediaType)
                && att.getHeader("Content-Disposition") == null) {
                ContentDisposition cd = new 
                    ContentDisposition("form-data;name=\"" 
                        + att.getContentId() + "\"");
                MultivaluedMap<String, String> newHeaders = 
                    new MetadataMap<String, String>(att.getHeaders(), false, true);
                newHeaders.putSingle("Content-Disposition", cd.toString());
                ret = new Attachment(att.getContentId(), dh, newHeaders);
            } else {
                ret = new Attachment(att.getContentId(), dh, att.getHeaders());
            }
            return ret;
        } else if (byte[].class.isAssignableFrom(obj.getClass())) {
            ByteDataSource source = new ByteDataSource((byte[])obj);
            source.setContentType(mimeType);
            dh = new DataHandler(source);
        } else {
            dh = getHandlerForObject(obj, cls, genericType, anns, mimeType);
        }
        String contentId = getContentId(anns, id);
        MultivaluedMap<String, String> headers = new MetadataMap<>();
        headers.putSingle("Content-Type", mimeType);
        
        return new Attachment(contentId, dh, headers);
    }

    private String getContentId(Annotation[] anns, int id) {
        Multipart part = AnnotationUtils.getAnnotation(anns, Multipart.class);
        if (part != null && !"".equals(part.value())) {
            return part.value();
        }
        return id == 0 ? AttachmentUtil.BODY_ATTACHMENT_ID : Integer.toString(id);
    }

    private <T> DataHandler getHandlerForObject(T obj,
                                            Class<T> cls, Type genericType,
                                            Annotation[] anns,
                                            String mimeType) {
        MediaType mt = JAXRSUtils.toMediaType(mimeType);
        mc.put(ProviderFactory.ACTIVE_JAXRS_PROVIDER_KEY, this);

        final MessageBodyWriter<T> r;
        try {
            r = mc.getProviders().getMessageBodyWriter(cls, genericType, anns, mt);
        } finally {
            mc.put(ProviderFactory.ACTIVE_JAXRS_PROVIDER_KEY, null);
        }
        if (r == null) {
            org.apache.cxf.common.i18n.Message message =
                new org.apache.cxf.common.i18n.Message("NO_MSG_WRITER",
                                                   BUNDLE,
                                                   cls);
            LOG.severe(message.toString());
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }

        return new MessageBodyWriterDataHandler<T>(r, obj, cls, genericType, anns, mt);
    }
    private <T> DataHandler getHandlerForObject(T obj,
                                            Type genericType,
                                            Annotation[] anns,
                                            String mimeType, int id) {
        @SuppressWarnings("unchecked")
        Class<T> cls = (Class<T>)obj.getClass();
        return getHandlerForObject(obj, cls, genericType, anns, mimeType);
    }

    private DataHandler createInputStreamDH(InputStream is, String mimeType) {
        return new DataHandler(new InputStreamDataSource(is, mimeType));
    }

    private String getRootMediaType(Annotation[] anns, MediaType mt) {
        String mimeType = mt.getParameters().get("type");
        if (mimeType != null) {
            return mimeType;
        }
        Multipart id = AnnotationUtils.getAnnotation(anns, Multipart.class);
        if (id != null && !MediaType.WILDCARD.equals(id.type())) {
            mimeType = id.type();
        }
        if (mimeType == null) {
            if (PropertyUtils.isTrue(mc.getContextualProperty(Message.MTOM_ENABLED))) {
                mimeType = "text/xml";
            } else {
                mimeType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return mimeType;
    }

    private static class MessageBodyWriterDataHandler<T> extends DataHandler {
        private MessageBodyWriter<T> writer;
        private T obj;
        private Class<T> cls;
        private Type genericType;
        private Annotation[] anns;
        private MediaType contentType;
        MessageBodyWriterDataHandler(MessageBodyWriter<T> writer,
                                     T obj,
                                     Class<T> cls,
                                     Type genericType,
                                     Annotation[] anns,
                                     MediaType contentType) {
            super(new ByteDataSource("1".getBytes(), contentType.toString()));
            this.writer = writer;
            this.obj = obj;
            this.cls = cls;
            this.genericType = genericType;
            this.anns = anns;
            this.contentType = contentType;
        }

        @Override
        public void writeTo(OutputStream os) {
            try {
                writer.writeTo(obj, cls, genericType, anns, contentType,
                               new MetadataMap<String, Object>(), os);
            } catch (IOException ex) {
                throw ExceptionUtils.toInternalServerErrorException(ex, null);
            }
        }

        @Override
        public String getContentType() {
            return contentType.toString();
        }

        // TODO : throw UnsupportedOperationException for all other DataHandler methods
    }
}
