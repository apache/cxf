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

package org.apache.cxf.jaxws.binding;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;

import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;

public abstract class AbstractBindingImpl implements Binding {
    private List<Handler> handlerChain = new ArrayList<Handler>();
    private final JaxWsEndpointImpl endpoint;
    
    public AbstractBindingImpl(JaxWsEndpointImpl imp) {
        endpoint = imp;
    }
    
    
    public List<Handler> getHandlerChain() {
        //per spec, this should be a copy
        return new ArrayList<Handler>(handlerChain);
    }

    public void setHandlerChain(List<Handler> hc) {
        handlerChain = hc;
        if (handlerChain == null || handlerChain.isEmpty()) {
            endpoint.removeHandlerInterceptors();
        } else {
            endpoint.addHandlerInterceptors();
        }
    }

    public abstract String getBindingID();
}
