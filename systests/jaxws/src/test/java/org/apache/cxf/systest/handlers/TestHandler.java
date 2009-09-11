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


import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.Soap11;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.handler_test.PingException;
import org.apache.handler_test.types.Ping;
import org.apache.handler_test.types.PingResponse;
import org.apache.handler_test.types.PingWithArgs;


public class TestHandler<T extends LogicalMessageContext> 
    extends TestHandlerBase implements LogicalHandler<T> {

    private final JAXBContext jaxbCtx;

    public TestHandler() {
        this(true);
    } 

    public TestHandler(boolean serverSide) {
        super(serverSide); 

        try {
            jaxbCtx = JAXBContext.newInstance(PackageUtils.getPackageName(Ping.class));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
 
    } 

    public String getHandlerId() { 
        return "handler" + getId();
    } 

    public boolean handleMessage(T ctx) {
        methodCalled("handleMessage");
        printHandlerInfo("handleMessage", isOutbound(ctx));

        boolean outbound = (Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        boolean ret = getHandleMessageRet(); 

        if (!isServerSideHandler()) {
            return true;
        }

        try {
            verifyJAXWSProperties(ctx);
        } catch (PingException e) {
            e.printStackTrace();
            throw new ProtocolException(e);
        }
        
        Object obj = ctx.getMessage().getPayload(jaxbCtx);
        
        if (obj instanceof Ping 
            || obj instanceof PingResponse) {
            ret = handlePingMessage(outbound, ctx);
        } else if (obj instanceof PingWithArgs) {
            ret = handlePingWithArgsMessage(outbound, ctx); 
        }
        return ret;
    }


    private boolean handlePingWithArgsMessage(boolean outbound, T ctx) { 

        
        LogicalMessage msg = ctx.getMessage();
        Object payload = msg.getPayload(jaxbCtx); 
        addHandlerId(ctx.getMessage(), ctx, outbound);

        boolean ret = true;
        if (payload instanceof PingWithArgs) {
            String arg = ((PingWithArgs)payload).getHandlersCommand();
            
            StringTokenizer strtok = new StringTokenizer(arg, " ");
            String hid = "";
            String direction = "";
            String command = "";
            if (strtok.countTokens() >= 3) {
                hid = strtok.nextToken();
                direction = strtok.nextToken();
                command = strtok.nextToken();
            }
            
            if (!getHandlerId().equals(hid)) {
                return true;
            }
            
            if ("stop".equals(command)) {
                if (!outbound && "inbound".equals(direction)) {
                    PingResponse resp = new PingResponse();
                    resp.getHandlersInfo().addAll(getHandlerInfoList(ctx));
                    msg.setPayload(resp, jaxbCtx);
                    ret = false;
                } else if (outbound && "outbound".equals(direction)) {
                    ret = false;
                }
            } else if ("throw".equals(command)) {
                String exceptionType = null;
                String exceptionText = "HandleMessage throws exception";
                if (strtok.hasMoreTokens()) {
                    exceptionType = strtok.nextToken();
                }
                if (strtok.hasMoreTokens()) {
                    exceptionText = strtok.nextToken();
                }
                if (exceptionType != null && !outbound && "inbound".equals(direction)) {
                    if ("RuntimeException".equals(exceptionType)) {
                        throw new RuntimeException(exceptionText);
                    } else if ("ProtocolException".equals(exceptionType)) {
                        throw new ProtocolException(exceptionText);
                    }
                } else if (exceptionType != null && outbound && "outbound".equals(direction)) {
                    if ("RuntimeException".equals(exceptionType)) {
                        throw new RuntimeException(exceptionText);
                    } else if ("ProtocolException".equals(exceptionType)) {
                        throw new ProtocolException(exceptionText);
                    }
                }
             
            }
        }

        return ret;
    } 

    private boolean handlePingMessage(boolean outbound, T ctx) { 

        LogicalMessage msg = ctx.getMessage();
        addHandlerId(msg, ctx, outbound);
        return getHandleMessageRet();
    } 

    private void addHandlerId(LogicalMessage msg, T ctx, boolean outbound) { 

        Object obj = msg.getPayload(jaxbCtx);
        if (obj instanceof PingResponse) {
            PingResponse origResp = (PingResponse)obj;
            PingResponse newResp = new PingResponse();
            newResp.getHandlersInfo().addAll(origResp.getHandlersInfo());
            newResp.getHandlersInfo().add(getHandlerId());
            msg.setPayload(newResp, jaxbCtx);
        } else if (obj instanceof Ping || obj instanceof PingWithArgs) {
            getHandlerInfoList(ctx).add(getHandlerId());
        }
    }

    public boolean handleFault(T ctx) {
        methodCalled("handleFault"); 
        printHandlerInfo("handleFault", isOutbound(ctx));

        if (isServerSideHandler()) {

            if (!"handler2".equals(getHandlerId())) {
                return true;
            }
              
            DOMSource source = (DOMSource)ctx.getMessage().getPayload();
            Node node = source.getNode();

            Map<String, String> ns = new HashMap<String, String>();
            ns.put("s", Soap11.SOAP_NAMESPACE);
            XPathUtils xu = new XPathUtils(ns);
            String exceptionText = (String)xu.getValue("//s:Fault/faultstring/text()", node,
                                                       XPathConstants.STRING);
            //XMLUtils.writeTo(node, System.out);

            if ("handler2HandleFaultThrowsRunException".equals(exceptionText)) {
                throw new RuntimeException("handler2 HandleFault throws RuntimeException");
            } else if ("handler2HandleFaultThrowsSOAPFaultException".equals(exceptionText)) {
                throw createSOAPFaultException("handler2 HandleFault " 
                                               + "throws SOAPFaultException");               
            }                

        }
        
        return true;
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
    
    public void close(MessageContext arg0) {
        methodCalled("close");
    }

    public void init(Map arg0) {
        methodCalled("init");
    }

    public void destroy() {
        methodCalled("destroy");
    }

    public String toString() { 
        return getHandlerId(); 
    } 
}    
