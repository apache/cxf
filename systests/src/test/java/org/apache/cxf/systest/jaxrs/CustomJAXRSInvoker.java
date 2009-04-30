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
package org.apache.cxf.systest.jaxrs;

import java.lang.reflect.Method;
import java.security.Principal;

import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.systest.jaxrs.security.SecureBookStore;

public class CustomJAXRSInvoker extends JAXRSInvoker {

    @Override
    public Object invoke(Exchange exchange, Object requestParams, Object resourceObject) {
        
        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
        Method m = ori.getMethodToInvoke();
        Class<?> realClass = ClassHelper.getRealClass(resourceObject);
        
        Principal p = new SecurityContextImpl(exchange.getInMessage()).getUserPrincipal();
        if (realClass == SecureBookStore.class && "getThatBook".equals(m.getName())
            && "baddy".equals(p.getName())) {
            return new MessageContentsList(Response.status(Response.Status.FORBIDDEN).build());
        }
        
        return super.invoke(exchange, requestParams, resourceObject);
    }
}
