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
package org.apache.cxf.systest.handlers;


import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
//import javax.jws.HandlerChain;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.helpers.CastUtils;
import org.apache.handler_test.HandlerTest;
import org.apache.handler_test.PingException;
import org.apache.handler_test.types.PingFaultDetails;

@WebService(serviceName = "HandlerTestService",
            portName = "SoapPort",
            endpointInterface = "org.apache.handler_test.HandlerTest",
            targetNamespace = "http://apache.org/handler_test",
            wsdlLocation = "testutils/handler_test.wsdl")
@HandlerChain(file = "./handlers_invocation.xml", name = "TestHandlerChain")
public class HandlerTestImpl implements HandlerTest {

    private WebServiceContext context;

    public final List<String> ping() {

        try {
            List<String> handlerInfoList = getHandlersInfo(context.getMessageContext());
            handlerInfoList.add("servant");
            context.getMessageContext().remove("handler.info");
            //System.out.println(">> servant returning list: " + handlerInfoList);
            return handlerInfoList;

        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }

    public final void pingOneWay() {
    }

    public final List<String> pingWithArgs(String handlerCommand) throws PingException {

        List<String> ret = new ArrayList<String>();
        ret.add(handlerCommand);
        ret.addAll(getHandlersInfo(context.getMessageContext()));

        if (handlerCommand.contains("servant throw exception")) {
            PingFaultDetails details = new PingFaultDetails();
            details.setDetail(ret.toString());
            throw new PingException("from servant", details);
        } else if (handlerCommand.contains("servant throw RuntimeException")) {
            throw new RuntimeException("servant throw RuntimeException");
        } else if (handlerCommand.contains("servant throw SOAPFaultException")) {
            throw createSOAPFaultException("servant throws SOAPFaultException");
        } else if (handlerCommand.contains("servant throw WebServiceException")) {
            RuntimeException re = new RuntimeException("servant throws RuntimeException");
            throw new WebServiceException("RemoteException with nested RuntimeException", re);
        }

        return ret;
    }

    private SOAPFaultException createSOAPFaultException(String faultString) {
        try {
            SOAPFault fault = SOAPFactory.newInstance().createFault();
            fault.setFaultString(faultString);
            fault.setFaultCode(new QName("http://cxf.apache.org/faultcode", "Server"));
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            // do nothing
        }
        return null;
    }

    @Resource
    public void setWebServiceContext(WebServiceContext ctx) {
        context = ctx;
    }

    private List<String> getHandlersInfo(MessageContext ctx) {
        List<String> ret = CastUtils.cast((List)ctx.get("handler.info"));
        if (ret == null) {
            ret = new ArrayList<String>();
        }
        return ret;
    }

}
