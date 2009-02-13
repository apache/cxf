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

package org.apache.cxf.jaxws;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Factory;
import org.apache.cxf.service.invoker.SingletonFactory;

public class JAXWSMethodInvoker extends AbstractJAXWSMethodInvoker {

    public JAXWSMethodInvoker(final Object bean) {
        super(new SingletonFactory(bean));
    }
    
    public JAXWSMethodInvoker(Factory factory) {
        super(factory);
    }
     
    @Override
    protected Object invoke(Exchange exchange, final Object serviceObject, Method m, List<Object> params) {
        // set up the webservice request context 
        WrappedMessageContext ctx = new WrappedMessageContext(exchange.getInMessage(), Scope.APPLICATION);
        
        Map<String, Object> handlerScopedStuff = removeHandlerProperties(ctx);
        
        WebServiceContextImpl.setMessageContext(ctx);
        List<Object> res = null;
        try {
            res = CastUtils.cast((List)super.invoke(exchange, serviceObject, m, params));
            addHandlerProperties(ctx, handlerScopedStuff);
            //update the webservice response context
            updateWebServiceContext(exchange, ctx);
        } finally {
            //clear the WebServiceContextImpl's ThreadLocal variable
            WebServiceContextImpl.clear();
        }
        return res;
    }
    
}
