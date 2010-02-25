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

package org.apache.cxf.jaxws.binding.http;

import java.util.List;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.http.HTTPBinding;

import org.apache.cxf.jaxws.binding.AbstractBindingImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.service.model.BindingInfo;

public class HTTPBindingImpl extends AbstractBindingImpl implements HTTPBinding {
        
    public HTTPBindingImpl(BindingInfo sb, JaxWsEndpointImpl ep) {
        super(ep);
    }
    
    public String getBindingID() {
        //REVISIT: JIRA CXF-613
        return "http://cxf.apache.org/bindings/xformat";
    }
    
    @Override
    public void setHandlerChain(List<Handler> hc) {
        super.setHandlerChain(hc);
        validate();
    }

    private void validate() {
        for (Handler handler : this.getHandlerChain()) {
            if (!(handler instanceof LogicalHandler)) {
                throw new WebServiceException("Adding an incompatible handler in HTTPBinding: "
                                              + handler.getClass());
            }
        }        
    }    
}
