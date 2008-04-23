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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.handler_test.PingException;

/**
 * Describe class TestHandlerBase here.
 *
 *
 * Created: Fri Oct 21 14:02:50 2005
 *
 * @author <a href="mailto:codea@iona.com">codea</a>
 * @version 1.0
 */
public abstract class TestHandlerBase {
   
    private static final Logger LOG = LogUtils.getLogger(TestHandlerBase.class);

    private static int sid;
    private static int sinvokedOrder;

    private boolean handleMessageRet = true;

    private int invokeOrderOfHandleMessage;
    private int invokeOrderOfHandleFault;
    private int invokeOrderOfClose;

    private Map<String, Integer> methodCallCount = new HashMap<String, Integer>();
    private final int id;
    private final boolean isServerSideHandler;

    public TestHandlerBase(boolean serverSide) {
        id = ++sid; 
        isServerSideHandler = serverSide;
    }

    protected void methodCalled(String methodName) { 
        int val = 0;
        if (methodCallCount.keySet().contains(methodName)) { 
            val = methodCallCount.get(methodName);
        } 
        if ("handleMessage".equals(methodName)) {
            invokeOrderOfHandleMessage = ++sinvokedOrder;
        } else if ("handleFault".equals(methodName)) {
            invokeOrderOfHandleFault = ++sinvokedOrder;
        } else if ("close".equals(methodName)) {
            invokeOrderOfClose = ++sinvokedOrder;
        }

        val++; 
        methodCallCount.put(methodName, val);
    } 

    public int getInvokeOrderOfHandleMessage() {
        return invokeOrderOfHandleMessage;
    }
    
    public int getInvokeOrderOfHandleFault() {
        return invokeOrderOfHandleFault;
    }
    
    public int getInvokeOrderOfClose() {
        return invokeOrderOfClose;
    }  
    
    public int getId() {
        return id; 
    }
    
    public abstract String getHandlerId();

    public boolean isCloseInvoked() {

        return methodCallCount.containsKey("close");
    }

    public boolean isDestroyInvoked() {
        return methodCallCount.containsKey("destroy");
    }

    public boolean isHandleFaultInvoked() {
        return methodCallCount.containsKey("handleFault");
    }

    public int getHandleFaultInvoked() {
        return getMethodCallCount("handleFault");
    }

    public int getCloseInvoked() {
        return getMethodCallCount("close");
    }
    
    public boolean isHandleMessageInvoked() {
        return methodCallCount.containsKey("handleMessage");
    }

    public int getHandleMessageInvoked() {
        return getMethodCallCount("handleMessage");
    }
    
    public boolean isInitInvoked() {
        return methodCallCount.containsKey("init");
    }
    
    public boolean isPostConstructInvoked() {
        return methodCallCount.containsKey("doPostConstruct");
    }    
    
    public void setHandleMessageRet(boolean ret) { 
        handleMessageRet = ret; 
    }

    public boolean getHandleMessageRet() { 
        return handleMessageRet; 
    }
    
    public boolean isServerSideHandler() {
        return isServerSideHandler; 
    } 
    
    public void verifyJAXWSProperties(MessageContext ctx) throws PingException {
        if (isServerSideHandler() && isOutbound(ctx)) {
            /*
            QName operationName = (QName)ctx.get(MessageContext.WSDL_OPERATION);
            if (operationName == null) {
                throw new PingException("WSDL_OPERATION not found");
            }
            URI wsdlDescription = (URI)ctx.get(MessageContext.WSDL_DESCRIPTION);
            if (!wsdlDescription.toString().equals("http://localhost:9005/HandlerTest/SoapPort?wsdl")) {
                throw new PingException("WSDL_DESCRIPTION not found");
            }
            QName wsdlPort = (QName)ctx.get(MessageContext.WSDL_PORT);
            if (!wsdlPort.getLocalPart().equals("SoapPort")) {
                throw new PingException("WSDL_PORT not found");
            }       
            QName wsdlInterface = (QName)ctx.get(MessageContext.WSDL_INTERFACE);
            if (!wsdlInterface.getLocalPart().equals("HandlerTest")) {
                throw new PingException("WSDL_INTERFACE not found");
            }      
            QName wsdlService = (QName)ctx.get(MessageContext.WSDL_SERVICE);
            if (!wsdlService.getLocalPart().equals("HandlerTestService")) {
                throw new PingException("WSDL_SERVICE not found");
            }
            */
        }
    }

    protected void printHandlerInfo(String methodName, boolean outbound) { 
        String info = getHandlerId() + " "
            + (outbound ? "outbound" : "inbound") + " "
            + methodName + "   " + Thread.currentThread().getName();
        LOG.info(info);
    } 


    protected List<String> getHandlerInfoList(MessageContext ctx) { 
        List<String> handlerInfoList = null; 
        if (ctx.containsKey("handler.info")) { 
            handlerInfoList = CastUtils.cast((List)ctx.get("handler.info")); 
        } else {
            handlerInfoList = new ArrayList<String>();
            ctx.put("handler.info", handlerInfoList);
            ctx.setScope("handler.info", MessageContext.Scope.APPLICATION);
        }
        return handlerInfoList;
    }
    
    protected boolean isOutbound(MessageContext ctx) {
        return (Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
    }

    private int getMethodCallCount(String methodName) { 
        int ret = 0;
        if (methodCallCount.containsKey(methodName)) {
            ret = methodCallCount.get(methodName);             
        }
        return ret;
    } 
    
}
