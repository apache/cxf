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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

/**
 * Utility Exception class which makes it easier to get the response status,
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
    
    @Override
    public Response getResponse() {
        Response response = super.getResponse();
        
        ResponseBuilder rb = Response.status(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getMetadata();
        for (String header : headers.keySet()) {
            List<Object> values = headers.get(header);
            for (Object value : values) {
                rb.header(header, value);
            }
        }
        rb.entity(new ByteArrayInputStream(getMessage().getBytes()));
        return rb.build();
    }
    
    public int getStatus() {
        return super.getResponse().getStatus();    
    }
    
    @SuppressWarnings("unchecked")
    public MultivaluedMap<String, String> getHeaders() {
        return (MultivaluedMap<String, String>)((MultivaluedMap)super.getResponse().getMetadata());
    }
    
    @Override
    public String getMessage() {
        if (errorMessage == null) {
            errorMessage = readErrorMessage();
        }
        return errorMessage;
    }
    
    private String readErrorMessage() {
        Object entity = super.getResponse().getEntity();
        try {
            return entity == null ? "" : entity instanceof InputStream 
                ? IOUtils.readStringFromStream((InputStream)entity) : entity.toString();
        } catch (IOException ex) {
            return "";
        }
    }
    
    @Override
    public String toString() {
        String lineSep = SystemPropertyAction.getProperty("line.separator");
        
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
    
    /**
     * Returns the typed error message 
     * @param client the client
     * @param cls the entity class
     * @return the typed entity
     */
    @SuppressWarnings("unchecked")
    public <T> T toErrorObject(Client client, Class<T> entityCls) {
        Response response = getResponse();
        try {
            MultivaluedMap headers = response.getMetadata();
            Object contentType = headers.getFirst("Content-Type");
            InputStream inputStream = (InputStream)response.getEntity();
            if (contentType == null || inputStream == null) {
                return null;
            }
            Annotation[] annotations = new Annotation[]{};
            MediaType mt = MediaType.valueOf(contentType.toString());
            
            Endpoint ep = WebClient.getConfig(client).getConduitSelector().getEndpoint();
            Exchange exchange = new ExchangeImpl();
            Message inMessage = new MessageImpl();
            inMessage.setExchange(exchange);
            exchange.put(Endpoint.class, ep);
            exchange.setOutMessage(new MessageImpl());
            exchange.setInMessage(inMessage);
            inMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
            inMessage.put(Message.PROTOCOL_HEADERS, headers);
            
            ProviderFactory pf = (ProviderFactory)ep.get(ProviderFactory.class.getName());
            
            MessageBodyReader reader = pf.createMessageBodyReader(entityCls, 
                                                             entityCls, 
                                                             annotations, 
                                                             mt, 
                                                             inMessage);
            
            
            
            if (reader == null) {
                return null;
            }
            
            return (T)reader.readFrom(entityCls, entityCls, annotations, mt, 
                                      (MultivaluedMap<String, String>)headers, 
                                      inputStream);
        } catch (Exception ex) {
            throw new ClientWebApplicationException(ex);
        }
    }
}
