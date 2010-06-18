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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.binding.http.HttpConstants;
import org.apache.cxf.binding.http.URIMapper;
import org.apache.cxf.binding.xml.interceptor.XMLMessageOutInterceptor;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.interceptor.WrappedOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class DatabindingOutSetupInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final WrappedOutInterceptor WRAPPED_OUT = new WrappedOutInterceptor();
    private static final XMLMessageOutInterceptor XML_OUT = new XMLMessageOutInterceptor();
    private static final StaxOutInterceptor STAX_OUT = new StaxOutInterceptor();
    
    public DatabindingOutSetupInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    public void handleMessage(Message message) throws Fault {
        boolean client = Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE));
        
        InterceptorChain chain = message.getInterceptorChain();
        
        if (client) {
            Document document = DOMUtils.createDocument();
            message.setContent(Node.class, document);
            
            XMLStreamWriter writer = new W3CDOMStreamWriter(document);
            try {
                MapNamespaceContext nsMap = new MapNamespaceContext();
                nsMap.addNamespace(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD);
                writer.setNamespaceContext(nsMap);
            } catch (XMLStreamException e) {
                // ignore
            }
            message.setContent(XMLStreamWriter.class, writer);
           
            WrappedOutInterceptor wrappedOut = new WrappedOutInterceptor(Phase.PRE_LOGICAL);
            wrappedOut.addAfter(getId());
            chain.add(wrappedOut);

            final XMLMessageOutInterceptor xmlOut = new XMLMessageOutInterceptor(Phase.PRE_LOGICAL);
            xmlOut.addAfter(wrappedOut.getId());
            chain.add(xmlOut);

            Endpoint ep = message.getExchange().get(Endpoint.class);
            URIMapper mapper = (URIMapper) ep.getService().get(URIMapper.class.getName());
            BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
            
            String verb = mapper.getVerb(bop);
            message.put(Message.HTTP_REQUEST_METHOD, verb);
            boolean putOrPost = verb.equals(HttpConstants.POST) || verb.equals(HttpConstants.PUT);
            
            if (putOrPost) { 
                chain.add(new URIParameterOutInterceptor());
                chain.add(new DocumentWriterInterceptor());
                chain.add(new AbstractPhaseInterceptor<Message>("remove-writer", 
                        Phase.PREPARE_SEND) {
                    {
                        addAfter(xmlOut.getId());
                        addBefore(MessageSenderInterceptor.class.getName());
                    }
                    public void handleMessage(Message message) throws Fault {
                        message.removeContent(XMLStreamWriter.class);
                    }
                });
                chain.add(STAX_OUT);
            } else {
                chain.add(new URIParameterOutInterceptor());
            }
           
        } else {
            chain.add(STAX_OUT);
            chain.add(WRAPPED_OUT);
            chain.add(XML_OUT);
        }
    }

}
