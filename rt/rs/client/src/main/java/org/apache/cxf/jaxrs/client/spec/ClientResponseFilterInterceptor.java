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
package org.apache.cxf.jaxrs.client.spec;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

public class ClientResponseFilterInterceptor extends AbstractInDatabindingInterceptor {

    public ClientResponseFilterInterceptor() {
        super(Phase.PRE_PROTOCOL_FRONTEND);
    }

    public void handleMessage(Message inMessage) throws Fault {
        ClientProviderFactory pf = ClientProviderFactory.getInstance(inMessage);
        if (pf == null) {
            return;
        }

        List<ProviderInfo<ClientResponseFilter>> filters = pf.getClientResponseFilters();
        if (!filters.isEmpty()) {
            ClientRequestContext reqContext = new ClientRequestContextImpl(inMessage.getExchange().getOutMessage(),
                                                                        true);

            ClientResponseContext respContext =
                new ClientResponseContextImpl((ResponseImpl)getResponse(inMessage),
                                              inMessage);
            for (ProviderInfo<ClientResponseFilter> filter : filters) {
                InjectionUtils.injectContexts(filter.getProvider(), filter, inMessage);
                try {
                    filter.getProvider().filter(reqContext, respContext);
                } catch (IOException ex) {
                    throw new ProcessingException(ex);
                }
            }
        }
    }

    protected Response getResponse(Message inMessage) {
        Response resp = inMessage.getExchange().get(Response.class);
        if (resp != null) {
            return JAXRSUtils.copyResponseIfNeeded(resp);
        }
        ResponseBuilder rb = JAXRSUtils.toResponseBuilder((Integer)inMessage.get(Message.RESPONSE_CODE));
        rb.entity(inMessage.get(InputStream.class));

        @SuppressWarnings("unchecked")
        Map<String, List<String>> protocolHeaders =
            (Map<String, List<String>>)inMessage.get(Message.PROTOCOL_HEADERS);
        for (Map.Entry<String, List<String>> entry : protocolHeaders.entrySet()) {
            if (null == entry.getKey()) {
                continue;
            }
            if (entry.getValue().size() > 0) {
                for (String val : entry.getValue()) {
                    rb.header(entry.getKey(), val);
                }
            }
        }


        return rb.build();
    }
}
