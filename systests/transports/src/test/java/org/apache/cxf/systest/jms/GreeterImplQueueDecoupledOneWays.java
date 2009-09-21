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

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.hello_world_jms.HelloWorldOneWayPort;
import org.apache.cxf.hello_world_jms.HelloWorldQueueDecoupledOneWaysService;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;



@WebService(serviceName = "HelloWorldQueueDecoupledOneWaysService", 
            portName = "HelloWorldQueueDecoupledOneWaysPort", 
            endpointInterface = "org.apache.cxf.hello_world_jms.HelloWorldOneWayPort",
            targetNamespace = "http://cxf.apache.org/hello_world_jms",
            wsdlLocation = "testutils/jms_test.wsdl")
public class GreeterImplQueueDecoupledOneWays implements HelloWorldOneWayPort {
    
    @Resource
    private WebServiceContext context;
    private Throwable asyncEx;
    private String request;
    private String reply;
    private CountDownLatch latch = new CountDownLatch(1);
    private boolean specCompliant;

    public GreeterImplQueueDecoupledOneWays() {
    }

    public GreeterImplQueueDecoupledOneWays(boolean specCompliant) {
        this.specCompliant = specCompliant;
    }
    
    public void greetMeOneWay(String value) {
        synchronized (this) {
            request = value;
            notifyAll();
        }
        try {
            if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
                synchronized (this) {
                    asyncEx = new Exception("Time out while waiting for command to send reply");
                    notifyAll();
                }
                return;
            }
        } catch (InterruptedException e) {
            synchronized (this) {
                asyncEx = e;
                notifyAll();
            }
            return;
        }
        sendReply();
    }
    
    protected void sendReply() {
        JMSMessageHeadersType headers = 
            (JMSMessageHeadersType)context.getMessageContext().get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);
        if (headers == null || headers.getJMSReplyTo() == null) {
            synchronized (this) {
                if (!specCompliant) {
                    asyncEx = new Exception("ReplyTo header in the server Request context was null");
                }
                notifyAll();
            }
            return;
        } else if (headers != null && headers.getJMSReplyTo() != null && specCompliant) {
            synchronized (this) {
                asyncEx = new Exception("ReplyTo header in the server Request context was not null");
                notifyAll();
            }
            return;
        }
        
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms",
                                      "HelloWorldQueueDecoupledOneWaysService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms",
                                   "HelloWorldQueueDecoupledOneWaysReplyPort");
        Throwable e = null;
        
        try {
            URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
            HelloWorldQueueDecoupledOneWaysService service = 
                new HelloWorldQueueDecoupledOneWaysService(wsdl, serviceName);
            HelloWorldOneWayPort greeter = service.getPort(portName, HelloWorldOneWayPort.class);
            reply = "Re:" + request;
            greeter.greetMeOneWay(reply);
        } catch (Throwable t) {
            e = t;
        }
        synchronized (this) {
            asyncEx = e;
            notifyAll();
        }

    }

    public void proceedWithReply() {
        latch.countDown();
    }
        
    public String ackRequestReceived(long timeout) {
        synchronized (this) {
            if (request != null) {
                return request;
            }
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                asyncEx = e;
                return null;
            }
        }
        return request;
    }

    public String ackReplySent(long timeout) {
        synchronized (this) {
            if (asyncEx != null) {
                return null;
            }
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                asyncEx = e;
                return null;
            }
        }
        return reply;
    }

    public boolean ackNoReplySent(long timeout) {
        synchronized (this) {
            if (asyncEx != null) {
                return false;
            }
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                asyncEx = e;
            }
        }
        return asyncEx == null;
    }

    public Throwable getException() {
        return asyncEx;
    }
}
