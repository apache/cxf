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
package org.apache.cxf.rs.security.xml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.ws.security.WSSConfig;


public abstract class AbstractXmlSecOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractXmlSecOutInterceptor.class);
    
    static {
        WSSConfig.init();
    }
    
    public AbstractXmlSecOutInterceptor() {
        super(Phase.WRITE);
    } 

    public void handleMessage(Message message) throws Fault {
        try {
            Document doc = getDomDocument(message);
            if (doc == null) {
                return;
            }
            Document finalDoc = processDocument(message, doc);
            
            message.setContent(List.class, 
                new MessageContentsList(new DOMSource(finalDoc)));
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            LOG.warning(sw.toString());
            throw new Fault(new RuntimeException(ex.getMessage() + ", stacktrace: " + sw.toString()));
        }
    }
    
    protected abstract Document processDocument(Message message, Document doc)
        throws Exception; 
    
    
    
    private Object getRequestBody(Message message) {
        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs == null || objs.size() == 0) {
            return null;
        } else {
            return objs.get(0);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Document getDomDocument(Message m) throws Exception {
        
        Object body = getRequestBody(m);
        if (body == null) {
            return null;
        }
        
        if (body instanceof Document) {
            return (Document)body;
        }
        if (body instanceof DOMSource) {
            return (Document)((DOMSource)body).getNode();
        }
        
        ProviderFactory pf = ProviderFactory.getInstance(m);
        
        Object providerObject = pf.createMessageBodyWriter(body.getClass(), 
                                   body.getClass(), new Annotation[]{}, 
                                   MediaType.APPLICATION_XML_TYPE, m);
        if (!(providerObject instanceof JAXBElementProvider)) {
            return null;
        }
        JAXBElementProvider provider = (JAXBElementProvider)providerObject;
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        m.setContent(XMLStreamWriter.class, writer);
        provider.writeTo(body, body.getClass(), 
                         body.getClass(), new Annotation[]{},
                         MediaType.APPLICATION_XML_TYPE,
                         (MultivaluedMap)m.get(Message.PROTOCOL_HEADERS), null);
        return writer.getDocument();
    }
    
}
