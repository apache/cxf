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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;

public class SoapActionInInterceptor extends AbstractSoapInterceptor {
    
    public SoapActionInInterceptor() {
        super(Phase.READ);
        addAfter(ReadHeadersInterceptor.class.getName());
        addAfter(EndpointSelectionInterceptor.class.getName());
    }
    
    public void handleMessage(SoapMessage message) throws Fault {
        if (message.getVersion() instanceof Soap11) {
            Map<String, List<String>> headers = CastUtils.cast((Map)message.get(Message.PROTOCOL_HEADERS));
            if (headers != null) {
                List<String> sa = headers.get(SoapBindingConstants.SOAP_ACTION);
                if (sa != null && sa.size() > 0) {
                    String action = sa.get(0);
                    if (action.startsWith("\"")) {
                        action = action.substring(1, action.length() - 1);
                    }
                    getAndSetOperation(message, action);
                }
            }
        } else if (message.getVersion() instanceof Soap12) {
            String ct = (String) message.get(Message.CONTENT_TYPE);
            
            if (ct == null) {
                return;
            }
            
            int start = ct.indexOf("action=");
            if (start != -1) {
                int end;
                if (ct.charAt(start + 7) == '\"') {
                    start += 8;
                    end = ct.indexOf('\"', start);
                } else {
                    start += 7;
                    end = ct.indexOf(';', start);
                    if (end == -1) {
                        end = ct.length();
                    }
                }
                
                getAndSetOperation(message, ct.substring(start, end));
            }
        }
    }

    private void getAndSetOperation(SoapMessage message, String action) {
        if (StringUtils.isEmpty(action)) {
            return;
        }
        
        Exchange ex = message.getExchange();
        Endpoint ep = ex.get(Endpoint.class);
        
        BindingOperationInfo bindingOp = null;
        
        Collection<BindingOperationInfo> bops = ep.getBinding().getBindingInfo().getOperations();
        if (bops == null) {
            return;
        }
        for (BindingOperationInfo boi : bops) {
            SoapOperationInfo soi = (SoapOperationInfo) boi.getExtensor(SoapOperationInfo.class);
            if (soi != null && action.equals(soi.getAction())) {
                if (bindingOp != null) {
                    //more than one op with the same action, will need to parse normally
                    return;
                }
                bindingOp = boi;
            }
        }
        if (bindingOp != null) {
            ex.put(BindingOperationInfo.class, bindingOp);
            ex.put(OperationInfo.class, bindingOp.getOperationInfo());
        }
    }

}
