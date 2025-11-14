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

package org.apache.cxf.jaxrs.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.attachment.AttachmentBoundaryDeserializer;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.EntityPartImpl;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;
import org.apache.cxf.message.Message;

final class EntityPartUtils {
    private static final String MULTIPART_FORM_DATA_TYPE = "form-data";
    private static final String CONTENT_DISPOSITION_FILES_PARAM = "files";

    private EntityPartUtils() {
    }

    public static List<EntityPart> getEntityParts(final MessageContext mc) {
        return from(mc.getProviders(), AttachmentUtils.getMultipartBody(mc));
    }
    
    public static EntityPart getEntityPart(final String value, final Message message) {
        final Providers providers = new ProvidersImpl(message);
        try (InputStream is = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
            final AttachmentBoundaryDeserializer deserializer = new AttachmentBoundaryDeserializer(message);
            final Attachment attachment = new Attachment(deserializer.read(is), providers);
            return createFromAttachment(providers, attachment);
        } catch (IOException ex) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
    }

    private static List<EntityPart> from(final Providers providers, final MultipartBody body) {
        final List<Attachment> atts = body.getAllAttachments();
        final List<EntityPart> parts = new ArrayList<>(atts.size());

        for (Attachment a : atts) {
            final EntityPart part = createFromAttachment(providers,  a);
            if (part != null) {
                parts.add(part);
            }
        }

        return parts;
    }

    private static EntityPart createFromAttachment(final Providers providers, Attachment a) {
        final ContentDisposition cd = a.getContentDisposition();
        if (cd != null && !MULTIPART_FORM_DATA_TYPE.equalsIgnoreCase(cd.getType())) {
            return null;
        }

        final String fileName = (cd != null) ? cd.getFilename() : null;
        final String cdName = cd == null ? null : cd.getParameter("name");
        final String contentId = a.getContentId();
        final String name = StringUtils.isEmpty(cdName) ? contentId : cdName.replace("\"", "").replace("'", "");
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(fileName)) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }

        if (CONTENT_DISPOSITION_FILES_PARAM.equals(name)) {
            return null;
        }

        try {
            final InputStream is = a.getDataHandler().getInputStream();
            if (!StringUtils.isEmpty(fileName)) {
                return new EntityPartImpl(providers, name, fileName, is, InputStream.class, null,
                    a.getHeaders(), a.getContentType());
            } else {
                return new EntityPartImpl(providers, name, null, is, InputStream.class, null,
                    a.getHeaders(), a.getContentType());
            }
        } catch (IOException ex) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
    }
}
