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

package org.apache.cxf.systest.jaxws.beanpostprocessor;

import jakarta.annotation.Resource;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.service.Service;

/**
 * a web service that can be launched and spoken to by the test.
 */
@WebService(name = "Horatio", serviceName = "Alger",
            endpointInterface = "org.apache.cxf.systest.jaxws.beanpostprocessor.IWebServiceRUs")
public class WebServiceRUs implements IWebServiceRUs {

    private static org.apache.cxf.service.Service service;

    @Resource
    WebServiceContext injectedContext;

    private void noteService() {
        MessageContext ctx = injectedContext.getMessageContext();
        WrappedMessageContext wmc = (WrappedMessageContext) ctx;
        org.apache.cxf.message.Message msg = wmc.getWrappedMessage();
        service = msg.getExchange().getService();
    }

    @WebMethod(exclude = true)
    public static Service getService() {
        return service;
    }

    /** {@inheritDoc}*/
    @WebMethod
    public String consultTheOracle() {
        noteService();
        return "All your bases belong to us.";
    }
}
