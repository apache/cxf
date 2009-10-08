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
    private MultivaluedMap<String, String> headers = new MetadataMap<String, String>();
    private String contentId;
    
    public Attachment(org.apache.cxf.message.Attachment a) {
        handler = a.getDataHandler();
        contentId = a.getId();
        for (Iterator<String> i = a.getHeaderNames(); i.hasNext();) {
            String name = i.next();
            headers.add(name, a.getHeader(name));
        }
    }
    
    public Attachment(String id, DataHandler dh, MultivaluedMap<String, String> headers) {
        handler = dh;
        contentId = id;
        this.headers = new MetadataMap<String, String>(headers);
    }
    
    public Attachment(String id, DataSource ds, MultivaluedMap<String, String> headers) {
        this(id, new DataHandler(ds), headers);
    }
    
    public Attachment(InputStream is, MultivaluedMap<String, String> headers) {
        this(headers.getFirst("Content-ID"), 
             new DataHandler(new InputStreamDataSource(is, headers.getFirst("Content-Type"))), 
             headers);
    }
    
    public ContentDisposition getContentDisposition() {
        String header = getHeader("Content-Disposition");
        
        return header == null ? null : new ContentDisposition(header);
    }

    public String getContentId() {
        return contentId;
    }

    public MediaType getContentType() {
        String value = handler.getContentType();
        return value == null ? MediaType.TEXT_PLAIN_TYPE : MediaType.valueOf(value);
    }

    public DataHandler getDataHandler() {
        return handler;
    }

    public String getHeader(String name) {
        String header = headers.getFirst(name);
        return header == null ? headers.getFirst(name.toLowerCase()) : header; 
    }
    
    public List<String> getHeaderAsList(String name) {
        List<String> header = headers.get(name);
        return header == null ? headers.get(name.toLowerCase()) : header;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return new MetadataMap<String, String>(headers);
    }
    
    @Override
    public int hashCode() {
        return contentId.hashCode() + 37 * headers.hashCode(); 
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Attachment)) { 
            return false;
        }
        
        Attachment other = (Attachment)o;
        return contentId.equals(other.contentId) && headers.equals(other.headers);
    }
    

}
