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

package org.apache.cxf.jaxrs.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;

/**
 * Utility Exception class which makes it easier to get to the status,
 * headers and error message if any
 */
public class ServerWebApplicationException extends WebApplicationException {

    private String errorMessage;
    
    public ServerWebApplicationException() {
        
    }
    
    public ServerWebApplicationException(Response response) {
        super(response);
    }
    
    public ServerWebApplicationException(Throwable cause, Response response) {
        super(cause, response);
    }
    
    public int getStatus() {
        return getResponse().getStatus();    
    }
    
    public MultivaluedMap<String, String> getHeaders() {
        MultivaluedMap<String, Object> metadata = getResponse().getMetadata();
        MultivaluedMap<String, String> headers = new MetadataMap<String, String>(metadata.size());
        for (String key : metadata.keySet()) {
            for (Object strObject : metadata.get(key)) {
                headers.add(key, strObject.toString());
            }
        }
        return headers;
    }
    
    @Override
    public String getMessage() {
        if (errorMessage == null) {
            errorMessage = readErrorMessage();
        }
        return errorMessage;
    }
    
    private String readErrorMessage() {
        Object entity = getResponse().getEntity();
        try {
            return entity == null ? "" : entity instanceof InputStream 
                ? IOUtils.readStringFromStream((InputStream)entity) : entity.toString();
        } catch (IOException ex) {
            return "";
        }
    }
    
    @Override
    public String toString() {
        String lineSep = System.getProperty("line.separator");
        
        StringBuilder sb = new StringBuilder();
        sb.append("Status : " + getStatus()).append(lineSep);
        sb.append("Headers : ").append(lineSep);
        
        MultivaluedMap<String, String> headers = getHeaders();
        for (String header : headers.keySet()) {
            sb.append(header + " :");
            for (Iterator<String> it = headers.get(header).iterator(); it.hasNext();) {
                sb.append(' ').append(it.next());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append(lineSep);
        }
        
        String message = getMessage();
        if (!StringUtils.isEmpty(message)) {
            sb.append("Error message : ").append(lineSep);
            sb.append(message).append(lineSep); 
        }
        return sb.toString();
    }
}
