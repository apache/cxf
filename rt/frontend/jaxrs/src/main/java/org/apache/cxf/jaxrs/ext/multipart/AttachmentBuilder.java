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

import javax.activation.DataHandler;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;

/**
 * Fluid builder class for {@link Attachment} objects.
 */
public class AttachmentBuilder {
    private MultivaluedMap<String, String> headers = 
        new MetadataMap<String, String>(false, true);
    private Object object;
    private DataHandler dataHandler;
    private ContentDisposition contentDisposition;
    
    public AttachmentBuilder() {
        //
    }
    
    public AttachmentBuilder id(String id) {
        headers.putSingle("Content-Id", id);
        return this;
        
    }
    
    public AttachmentBuilder mediaType(String mediaType) {
        headers.putSingle("Content-Type", mediaType);
        return this;
    }
    
    public AttachmentBuilder object(Object theObject) {
        this.object = theObject;
        return this;
    }
    
    public AttachmentBuilder dataHandler(DataHandler newDataHandler) {
        this.dataHandler = newDataHandler;
        return this;
    }
   
    
    public AttachmentBuilder header(String key, String value) {
        headers.putSingle(key, value);
        return this;
    }
    
    /**
     * Set all of the headers. This will overwrite any content ID,
     * media type, ContentDisposition, or other header set by previous calls.
     * @param allHeaders
     * @return
     */
    public AttachmentBuilder headers(MultivaluedMap<String, String> allHeaders) {
        headers = allHeaders;
        contentDisposition = null;
        return this;
    }
    
    public AttachmentBuilder contentDisposition(ContentDisposition newContentDisposition) {
        this.contentDisposition = newContentDisposition;
        return this;
    }
    
    public Attachment build() {
        if (contentDisposition != null) {
            headers.putSingle("Content-Disposition", contentDisposition.toString());
        }
        return new Attachment(headers, dataHandler, object);
    }


}
