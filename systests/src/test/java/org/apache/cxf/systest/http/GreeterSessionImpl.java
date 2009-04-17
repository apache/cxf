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

package org.apache.cxf.systest.http;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;


import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.types.GreetMeResponse;
import org.apache.cxf.greeter_control.types.PingMeResponse;
import org.apache.cxf.greeter_control.types.SayHiResponse;

@WebService(serviceName = "GreeterService",
            portName = "GreeterPort",
            endpointInterface = "org.apache.cxf.greeter_control.Greeter", 
            targetNamespace = "http://cxf.apache.org/greeter_control")
public class GreeterSessionImpl implements Greeter {
    private static final Logger LOG = 
        LogUtils.getLogger(GreeterSessionImpl.class,
                           null,
                           GreeterSessionImpl.class.getPackage().getName());
    
    @Resource
    private WebServiceContext context;
    
    // greetMe will use session to return last called name
    public String greetMe(String me) {
        LOG.info("Executing operation greetMe");        
        LOG.info("Message received: " + me);
        MessageContext mc = context.getMessageContext();
        HttpServletRequest req = (HttpServletRequest)mc.get(MessageContext.SERVLET_REQUEST);
        Cookie cookies[] = req.getCookies();
        String val = "";
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                val += ";" + cookie.getName() + "=" + cookie.getValue();
            }
        }
        
        HttpSession session = req.getSession();
        // Get a session property "counter" from context
        if (session == null) {
            throw new WebServiceException("No session in WebServiceContext");
        }
        String name = (String)session.getAttribute("name");
        if (name == null) {
            name = me;
            LOG.info("Starting the Session");
        } 
        
        session.setAttribute("name", me);
        
        return "Hello " + name + val;
    }
    

    public String sayHi() {
        LOG.info("Executing operation sayHi");
        
        return "Bonjour ";
    }
    
    public void pingMe() {
    }


    public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }


    public Response<GreetMeResponse> greetMeAsync(String requestType) {
        // TODO Auto-generated method stub
        return null;
    }


    public void greetMeOneWay(String requestType) {
        // TODO Auto-generated method stub
        
    }


    public Future<?> pingMeAsync(AsyncHandler<PingMeResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }


    public Response<PingMeResponse> pingMeAsync() {
        // TODO Auto-generated method stub
        return null;
    }


    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }


    public Response<SayHiResponse> sayHiAsync() {
        // TODO Auto-generated method stub
        return null;
    }

}
