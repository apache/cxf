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

package org.apache.cxf.frontend;

import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;

public class WSDLGetInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final WSDLGetInterceptor INSTANCE = new WSDLGetInterceptor();
    public static final String DOCUMENT_HOLDER = WSDLGetInterceptor.class.getName() + ".documentHolder";

    public WSDLGetInterceptor() {
        super(Phase.READ);
        getAfter().add(EndpointSelectionInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        String query = (String)message.get(Message.QUERY_STRING);
        if (!"GET".equals(method) || StringUtils.isEmpty(query)) {
            return;
        }

        String baseUri = (String)message.get(Message.REQUEST_URL);
        String ctx = (String)message.get(Message.PATH_INFO);

        // cannot have two wsdl's being written for the same endpoint at the same
        // time as the addresses may get mixed up
        synchronized (message.getExchange().getEndpoint()) {
            Map<String, String> map = UrlUtils.parseQueryString(query);
            if (isRecognizedQuery(map, baseUri, ctx, message.getExchange().getEndpoint().getEndpointInfo())) {
                Document doc = getDocument(message, baseUri, map, ctx, 
                                           message.getExchange().getEndpoint().getEndpointInfo());

                Endpoint e = message.getExchange().get(Endpoint.class);
                Message mout = new MessageImpl();
                mout.setExchange(message.getExchange());
                mout = e.getBinding().createMessage(mout);
                mout.setInterceptorChain(OutgoingChainInterceptor.getOutInterceptorChain(message
                    .getExchange()));
                message.getExchange().setOutMessage(mout);

                mout.put(DOCUMENT_HOLDER, doc);

                // FIXME - how can I change this to provide a different interceptor chain that just has the
                // stax, gzip and message sender components.
                Iterator<Interceptor<? extends Message>> iterator = mout.getInterceptorChain().iterator();
                while (iterator.hasNext()) {
                    Interceptor<? extends Message> inInterceptor = iterator.next();
                    if (!inInterceptor.getClass().equals(StaxOutInterceptor.class)
                        && !inInterceptor.getClass().equals(GZIPOutInterceptor.class)
                        && !inInterceptor.getClass().equals(MessageSenderInterceptor.class)) {
                        mout.getInterceptorChain().remove(inInterceptor);
                    }
                }

                mout.getInterceptorChain().add(WSDLGetOutInterceptor.INSTANCE);

                // skip the service executor and goto the end of the chain.
                message.getInterceptorChain().doInterceptStartingAt(
                        message,
                        OutgoingChainInterceptor.class.getName());
            }
        }
    }

    private Document getDocument(Message message, String base, Map<String, String> params, String ctxUri,
                                EndpointInfo endpointInfo) {
        return new WSDLGetUtils().getDocument(message, base, params, ctxUri, endpointInfo);
    }

    private boolean isRecognizedQuery(Map<String, String> map, String baseUri, String ctx,
                                     EndpointInfo endpointInfo) {

        if (map.containsKey("wsdl") || map.containsKey("xsd")) {
            return true;
        }
        return false;
    }
}