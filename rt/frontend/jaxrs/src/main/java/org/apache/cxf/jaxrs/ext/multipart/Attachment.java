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

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;

public class Attachment {

    private DataHandler handler;
    private MultivaluedMap<String, String> headers = 
        new MetadataMap<String, String>(false, true);
    private Object object;
    
    public Attachment(org.apache.cxf.message.Attachment a) {
        handler = a.getDataHandler();
        for (Iterator<String> i = a.getHeaderNames(); i.hasNext();) {
            String name = i.next();
            if ("Content-ID".equalsIgnoreCase(name)) {
                continue;
            }
            headers.add(name, a.getHeader(name));
        }
        headers.putSingle("Content-ID", a.getId());
    }
    
    public Attachment(String id, DataHandler dh, MultivaluedMap<String, String> headers) {
        handler = dh;
        this.headers = new MetadataMap<String, String>(headers, false, true);
        this.headers.putSingle("Content-ID", id);
    }
    
    public Attachment(String id, DataSource ds, MultivaluedMap<String, String> headers) {
        this(id, new DataHandler(ds), headers);
    }
    
    public Attachment(InputStream is, MultivaluedMap<String, String> headers) {
        this(headers.getFirst("Content-ID"), 
             new DataHandler(new InputStreamDataSource(is, headers.getFirst("Content-Type"))), 
             headers);
    }
    
    public Attachment(String id, String mediaType, Object object) {
        this.object = object;
        headers.putSingle("Content-ID", id);
        headers.putSingle("Content-Type", mediaType);
    }
    
    public Attachment(String id, InputStream is, ContentDisposition cd) {
        handler = new DataHandler(new InputStreamDataSource(is, "application/octet-stream"));
        headers.putSingle("Content-Disposition", cd.toString());
        headers.putSingle("Content-ID", id);
        headers.putSingle("Content-Type", "application/octet-stream");
    }
    
    public ContentDisposition getContentDisposition() {
        String header = getHeader("Content-Disposition");
        
        return header == null ? null : new ContentDisposition(header);
    }

    public String getContentId() {
        return headers.getFirst("Content-ID");
    }

    public MediaType getContentType() {
        String value = handler != null ? handler.getContentType() : headers.getFirst("Content-Type");
        return value == null ? MediaType.TEXT_PLAIN_TYPE : MediaType.valueOf(value);
    }

    public DataHandler getDataHandler() {
        return handler;
    }

    public Object getObject() {
        return object;
    }
    
    public String getHeader(String name) {
        List<String> header = headers.get(name);
        if (header == null || header.size() == 0) {
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
        return new MetadataMap<String, String>(headers);
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
