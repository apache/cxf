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
package org.apache.cxf.jaxrs.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class WriterInterceptorMBW implements WriterInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(WriterInterceptorMBW.class);
    
    private MessageBodyWriter<Object> writer;
    private Message m;
    public WriterInterceptorMBW(MessageBodyWriter<Object> writer, Message m) {
        this.writer = writer;
        this.m = m;
    }

    public MessageBodyWriter<Object> getMBW() {
        return writer;
    }
    
    @Override
    public void aroundWriteTo(WriterInterceptorContext c) throws IOException, WebApplicationException {
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Response EntityProvider is: " + writer.getClass().getName());
        }
        MultivaluedMap<String, Object> headers = c.getHeaders();
        Object mtObject = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        MediaType mt = mtObject == null ? c.getMediaType() : JAXRSUtils.toMediaType(mtObject.toString());
        m.put(Message.CONTENT_TYPE, mtObject.toString());
        
        HttpUtils.convertHeaderValuesToStringIfNeeded(headers);
        
        writer.writeTo(c.getEntity(), 
                       c.getType(), 
                       c.getGenericType(), 
                       c.getAnnotations(), 
                       mt, 
                       headers, 
                       c.getOutputStream());
    }
    
    
}
