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
package org.apache.cxf.systest.jms;

import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.hello_world_jms.BadRecordLitFault;
import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.NoSuchCodeLitFault;
import org.apache.cxf.hello_world_jms.types.BadRecordLit;
import org.apache.cxf.hello_world_jms.types.ErrorCode;
import org.apache.cxf.hello_world_jms.types.NoSuchCodeLit;
import org.apache.cxf.hello_world_jms.types.TestRpcLitFaultResponse;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.JMSPropertyType;

public class TwoWayJMSImplBase implements HelloWorldPortType {

    @Resource
    protected WebServiceContext wsContext;
    public String greetMe(String me) {
        if (me.startsWith("PauseForTwoSecs")) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                //ignore
            }
            me = me.substring("PauseForTwoSecs".length()).trim();
        }

        MessageContext mc = wsContext.getMessageContext();
        JMSMessageHeadersType headers =
            (JMSMessageHeadersType) mc.get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);
        System.out.println("get the message headers JMSCorrelationID: " + headers.getJMSCorrelationID());
        System.out.println("Reached here :" + me);
        
        // set reply header custom property
        JMSPropertyType testProperty = new JMSPropertyType();
        testProperty.setName("Test_Prop");
        testProperty.setValue("some return value "  + me);
        
        System.out.println("found property in request headers at index: " 
                           + headers.getProperty().indexOf(testProperty));
        
        JMSMessageHeadersType responseHeaders =
            (JMSMessageHeadersType) mc.get(JMSConstants.JMS_SERVER_RESPONSE_HEADERS);
        responseHeaders.getProperty().add(testProperty);
        
        return "Hello " + me;
    }

    public String sayHi() {        
        return "Bonjour";
    }
    
    public void greetMeOneWay(String requestType) {
        System.out.println("*********  greetMeOneWay: " + requestType);
    }
    
    public TestRpcLitFaultResponse testRpcLitFault(String faultType) 
        throws BadRecordLitFault, NoSuchCodeLitFault {
        BadRecordLit badRecord = new BadRecordLit();
        badRecord.setReason("BadRecordLitFault");
        if (faultType.equals(BadRecordLitFault.class.getSimpleName())) {
            throw new BadRecordLitFault("TestBadRecordLit", badRecord);
        }
        if (faultType.equals(NoSuchCodeLitFault.class.getSimpleName())) {
            ErrorCode ec = new ErrorCode();
            ec.setMajor((short)1);
            ec.setMinor((short)1);
            NoSuchCodeLit nscl = new NoSuchCodeLit();
            nscl.setCode(ec);
            throw new NoSuchCodeLitFault("TestNoSuchCodeLit", nscl);
        }
        
        return new TestRpcLitFaultResponse();
    }

    public Response<String> greetMeAsync(String stringParam0) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Future<?> greetMeAsync(String stringParam0, AsyncHandler<String> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Response<String> sayHiAsync() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Future<?> sayHiAsync(AsyncHandler<String> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Response<TestRpcLitFaultResponse> testRpcLitFaultAsync(String in) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Future<?> testRpcLitFaultAsync(String in, AsyncHandler<TestRpcLitFaultResponse> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

}
