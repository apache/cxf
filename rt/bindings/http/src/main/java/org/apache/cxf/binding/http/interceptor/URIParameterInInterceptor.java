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
package org.apache.cxf.binding.http.interceptor;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.apache.cxf.binding.http.IriDecoderHelper;
import org.apache.cxf.binding.http.IriDecoderHelper.Param;
import org.apache.cxf.binding.http.URIMapper;
import org.apache.cxf.binding.xml.interceptor.XMLMessageInInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.DocLiteralInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.staxutils.StaxUtils;


public class URIParameterInInterceptor extends AbstractPhaseInterceptor<Message> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(URIParameterInInterceptor.class);

    public URIParameterInInterceptor() {
        super(Phase.UNMARSHAL);
        addBefore(XMLMessageInInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        String path = (String)message.get(DispatchInterceptor.RELATIVE_PATH);
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        String contentType = (String)message.get(Message.CONTENT_TYPE);

        LOG.info("URIParameterInterceptor handle message on path [" + path 
                 + "] with content-type ["  + contentType + "]");
        
        BindingOperationInfo op = message.getExchange().get(BindingOperationInfo.class);

        URIMapper mapper = (URIMapper)message.getExchange().get(Service.class).get(URIMapper.class.getName());
        String location = mapper.getLocation(op);

        List<MessagePartInfo> parts = op.getOperationInfo().getInput().getMessageParts();

        if (parts.size() == 0) {
            message.setContent(Object.class, Collections.EMPTY_LIST);
            return;
        }

        if (parts.size() > 1) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("SINGLE_PART_REQUIRED", LOG));
        }

        message.getInterceptorChain().add(new XMLMessageInInterceptor());
        message.getInterceptorChain().add(new DocLiteralInInterceptor());

        MessagePartInfo part = parts.get(0);

        List<Param> params = null;
        if ("application/x-www-form-urlencoded".equals(contentType)) {
            params = IriDecoderHelper.decode(path, location, message.getContent(InputStream.class));
        } else if ("application/xml".equals(contentType)) {
            params = IriDecoderHelper.decodeIri(path, location);
        } else if ("text/xml".equals(contentType)) {
            params = IriDecoderHelper.decodeIri(path, location);
        } else if ("multipart/form-data".equals(contentType)) {
            // TODO
        } else {
            params = IriDecoderHelper.decodeIri(path, location);
        }

        mergeParams(message, path, method, part, params);
    }

    private void mergeParams(Message message, String path, String method, MessagePartInfo part,
                             List<Param> params) {
        // TODO: If its a POST/PUT operation we probably need to merge the
        // incoming doc
        Document doc;
        Collection<SchemaInfo> schemas = part.getMessageInfo().getOperation()
            .getInterface().getService().getSchemas();
        if ("POST".equals(method) || "PUT".equals(method)) {
            XMLInputFactory inputFactory = StaxInInterceptor.getXMLInputFactory(message);
            try {
                XMLStreamReader reader;
                synchronized (inputFactory) {
                    reader = inputFactory.createXMLStreamReader(message.getContent(InputStream.class));
                }
                doc = StaxUtils.read(reader);
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
            doc = IriDecoderHelper.interopolateParams(doc, 
                                                      part.getXmlSchema(),
                                                      schemas,
                                                      params);
        } else {
            doc = IriDecoderHelper.buildDocument(part.getXmlSchema(),
                                                 schemas,
                                                 params);
        }

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new DOMSource(doc));
        try {
            reader.next();
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
        message.setContent(XMLStreamReader.class, reader);
    }
}
