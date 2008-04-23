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
package org.apache.cxf.binding.soap.interceptor;

import java.util.Set;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractEndpointSelectionInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;

/**
 * Selects the most appropriate endpoint based on the SOAP version used.
 * 
 * @param message
 * @param eps
 * @return
 */
public class EndpointSelectionInterceptor extends AbstractEndpointSelectionInterceptor {
    
    public EndpointSelectionInterceptor() {
        super(Phase.READ);
        getAfter().add(ReadHeadersInterceptor.class.getName());
    }

    protected Endpoint selectEndpoint(Message message, Set<Endpoint> eps) {
        SoapVersion sv = ((SoapMessage)message).getVersion();

        for (Endpoint e : eps) {
            EndpointInfo ei = e.getEndpointInfo();
            BindingInfo binding = ei.getBinding();

            if (binding instanceof SoapBindingInfo 
                && ((SoapBindingInfo)binding).getSoapVersion().equals(sv)) {
                return e;
            }
        }

        return null;
    }

}
