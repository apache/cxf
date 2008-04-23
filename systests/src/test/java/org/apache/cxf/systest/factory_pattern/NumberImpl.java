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

package org.apache.cxf.systest.factory_pattern;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.factory_pattern.IsEvenResponse;
import org.apache.cxf.factory_pattern.ObjectFactory;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

@WebService(serviceName = "NumberService",
            endpointInterface = "org.apache.cxf.factory_pattern.Number", 
            targetNamespace = "http://cxf.apache.org/factory_pattern")
            
public class NumberImpl implements org.apache.cxf.factory_pattern.Number {

    @Resource
    protected WebServiceContext wsContext;
    
    public IsEvenResponse isEven() {

        String id = idFromWebServiceContext(getWebSercviceContext());
        
        int num = stateFromId(id);
        boolean ret = evalIsEeven(num);
        return genResponse(ret);
    }

    protected WebServiceContext getWebSercviceContext() {
        return wsContext;
    }
    
    protected String idFromWebServiceContext(WebServiceContext wsC) {
        MessageContext mc = wsC.getMessageContext();
        return idFromMessageContext(mc);
    }

    protected String idFromMessageContext(MessageContext mc) {        
        String id = EndpointReferenceUtils.getEndpointReferenceId(mc);
        
        boolean jmsInvoke = null != mc.get(JMSConstants.JMS_REQUEST_MESSAGE);
        if ("999".equals(id) && jmsInvoke) {
            // verification that this is indeed JMS
            throw new RuntimeException("This is indeed JMS, id=" + id);
        }
        
        return id;
    }

    private IsEvenResponse genResponse(boolean v) {
        IsEvenResponse resp = new ObjectFactory().createIsEvenResponse();
        resp.setEven(v);
        return resp;
    }

    private boolean evalIsEeven(int num) {
        boolean isEven = true;
        if (num != 0 && num % 2 != 0) {
            isEven = false;
        }
        return isEven;
    }

    private int stateFromId(String id) {
        int num = 0;
        if (id != null) {
            Integer val = Integer.valueOf(id);
            num = val.intValue();
        } else {
            throw new RuntimeException("State is an empty string, cannot determine val");
        }
        return num;
    }
}
