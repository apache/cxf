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

package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.HTTPSession;
import org.apache.cxf.transport.http.HttpServletRequestSnapshot;


public class ServletDestination extends AbstractHTTPDestination {
    
    static final Logger LOG = LogUtils.getL7dLogger(ServletDestination.class);
        
    private static final long serialVersionUID = 1L;        
    
    final ServletTransportFactory factory;
    final String path;
    
    /**
     * Constructor, allowing subsititution of configuration.
     * 
     * @param b the associated Bus
     * @param ci the associated conduit initiator
     * @param ei the endpoint info of the destination 
     * @param cfg the configuration
     * @throws IOException
     */    
    public ServletDestination(Bus b,
                              ConduitInitiator ci,
                              EndpointInfo ei,
                              ServletTransportFactory fact,
                              String p)
        throws IOException {
        // would add the default port to the address
        super(b, ci, ei, false);
        factory = fact;
        path = p;
    }
    
    
    protected Logger getLogger() {
        return LOG;
    }

    public void invoke(final ServletContext context, 
                       final HttpServletRequest req, 
                       final HttpServletResponse resp) throws IOException {
        invoke(null, context, req, resp);
    }
    
    public void invoke(final ServletConfig config,
                       final ServletContext context, 
                       final HttpServletRequest req, 
                       final HttpServletResponse resp) throws IOException {
        
        MessageImpl inMessage = new MessageImpl();
        setupMessage(inMessage,
                     config,
                     context,
                     req,
                     resp);
        //the HttpServletRequest will be recycled in another thread when the operation 
        //is oneway and WSA enabled. This SNAPSHOT request will be used in tihs case.
        
        inMessage.put("HTTP.REQUEST.SNAPSHOT", new HttpServletRequestSnapshot(req));
        ExchangeImpl exchange = new ExchangeImpl();
        exchange.setInMessage(inMessage);
        exchange.setSession(new HTTPSession(req));
        inMessage.setDestination(this);

        incomingObserver.onMessage(inMessage);
 
    }
    protected String getBasePath(String contextPath) throws IOException {
        return contextPath + getAddress().getAddress().getValue();
    }
    
    @Override
    public void shutdown() {
        try {
            factory.removeDestination(path);
        } catch (IOException ex) {
            //ignore
        }
        
        super.shutdown();
    }
    
}
