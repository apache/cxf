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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.Transferable;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

/**
 * This class represents an attachment; generally a multipart part.
 * Some constructors in here are intended only for
 * internal use in CXF, others are suitable or preparing
 * attachments to pass to the {@link org.apache.cxf.jaxrs.client.WebClient} API.
 * See the {@link AttachmentBuilder} for a convenient
 * way to create attachments for use with {@link org.apache.cxf.jaxrs.client.WebClient}.
 */
public class Attachment implements Transferable {

    private DataHandler handler;
    private MultivaluedMap<String, String> headers =
        new MetadataMap<>(false, true);
    private Object object;
    private Providers providers;

    public Attachment(org.apache.cxf.message.Attachment a,
                      Providers providers) {
        handler = a.getDataHandler();
        for (Iterator<String> i = a.getHeaderNames(); i.hasNext();) {
            String name = i.next();
            if ("Content-ID".equalsIgnoreCase(name)) {
                continue;
            }
            headers.add(name, a.getHeader(name));
        }
        headers.putSingle("Content-ID", a.getId());
        this.providers = providers;
    }

    public Attachment(String id, DataHandler dh, MultivaluedMap<String, String> headers) {
        handler = dh;
        this.headers = new MetadataMap<>(headers, false, true);
        this.headers.putSingle("Content-ID", id);
    }

    public Attachment(String id, DataSource ds, MultivaluedMap<String, String> headers) {
        this(id, new DataHandler(ds), headers);
    }

    public Attachment(MultivaluedMap<String, String> headers, Object object) {
        this.headers = headers;
        this.object = object;
    }

    public Attachment(InputStream is, MultivaluedMap<String, String> headers) {
        this(headers.getFirst("Content-ID"),
             new DataHandler(new InputStreamDataSource(is, headers.getFirst("Content-Type"))),
             headers);
    }

    public Attachment(String mediaType, Object object) {
        this(UUID.randomUUID().toString(), mediaType, object);
    }

    public Attachment(String id, String mediaType, Object object) {
        this.object = object;
        if (id != null) {
            headers.putSingle("Content-ID", id);
        }
        headers.putSingle("Content-Type", mediaType);
    }

    public Attachment(String id, InputStream is, ContentDisposition cd) {
        handler = new DataHandler(new InputStreamDataSource(is, "application/octet-stream"));
        if (cd != null) {
            headers.putSingle("Content-Disposition", cd.toString());
        }
        headers.putSingle("Content-ID", id);
        headers.putSingle("Content-Type", "application/octet-stream");
    }

    public Attachment(MultivaluedMap<String, String> headers, DataHandler handler, Object object) {
        this.headers = headers;
        this.handler = handler;
        this.object = object;
    }

    public ContentDisposition getContentDisposition() {
        String header = getHeader("Content-Disposition");

        return header == null ? null : new ContentDisposition(header);
    }

    public String getContentId() {
        return headers.getFirst("Content-ID");
    }

    public MediaType getContentType() {
        String value = handler != null && handler.getContentType() != null ? handler.getContentType()
            : headers.getFirst("Content-Type");
        return value == null ? MediaType.TEXT_PLAIN_TYPE : JAXRSUtils.toMediaType(value);
    }

    public DataHandler getDataHandler() {
        return handler;
    }
    public void setDataHandler(DataHandler dataHandler) {
        this.handler = dataHandler;
    }

    public Object getObject() {
        return object;
    }

    public <T> T getObject(Class<T> cls) {
        if (providers != null) {
            MessageBodyReader<T> mbr =
                providers.getMessageBodyReader(cls, cls, new Annotation[]{}, getContentType());
            if (mbr != null) {
                try {
                    return mbr.readFrom(cls, cls, new Annotation[]{}, getContentType(),
                                        headers, getDataHandler().getInputStream());
                } catch (Exception ex) {
                    throw ExceptionUtils.toInternalServerErrorException(ex, null);
                }
            }
        }
        return null;
    }

    public String getHeader(String name) {
        List<String> header = headers.get(name);
        if (header == null || header.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < header.size(); i++) {
            sb.append(header.get(i));
            if (i + 1 < header.size()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    public List<String> getHeaderAsList(String name) {
        return headers.get(name);
    }

    public MultivaluedMap<String, String> getHeaders() {
        return new MetadataMap<String, String>(headers, false, true);
    }

    public void transferTo(File destinationFile) throws IOException {
        IOUtils.transferTo(handler.getInputStream(), destinationFile);
    }

    @Override
    public int hashCode() {
        return headers.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Attachment)) {
            return false;
        }

        Attachment other = (Attachment)o;
        return headers.equals(other.headers);
    }


}
