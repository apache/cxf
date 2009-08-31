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

package org.apache.cxf.systest.provider.datasource;

import java.util.logging.Logger;

import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import org.apache.cxf.common.logging.LogUtils;

public abstract class AbstractProvider<T> implements WebProvider {
    static final Logger LOG = LogUtils.getLogger(AbstractProvider.class);
    protected WebServiceContext wsContext;

    public T invoke(T req) {
        
        MessageContext mc = wsContext.getMessageContext();
        String method = (String)mc.get(MessageContext.HTTP_REQUEST_METHOD); 
        LOG.info("method: " + method);

        T ret = null;
        if ("GET".equalsIgnoreCase(method)) {
            ret = get(req);
        }  else if ("POST".equalsIgnoreCase(method)) {
            ret = post(req);
        }

        return ret;
    }

    protected  T get(T req) {
        return req;
    }

    
    public WebServiceContext getWebServiceContext() { 
        return wsContext;
    }
    
    protected T post(T req) {
        return req;
    }

    
    public void publish(String url) { 
        Endpoint ep = Endpoint.create(HTTPBinding.HTTP_BINDING, this);
        ep.publish(url);
    }

    public void setWebServiceContext(WebServiceContext wsc) {
        wsContext = wsc;
    }
}
