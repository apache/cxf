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

package org.apache.cxf.systest.ws.policy;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.policy.server.JavaFirstPolicyServer;
import org.apache.cxf.systest.ws.wssec11.client.UTPasswordCallback;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;

import org.junit.BeforeClass;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JavaFirstPolicyServiceTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(JavaFirstPolicyServer.class);
    static final String PORT2 = allocatePort(JavaFirstPolicyServer.class, 2);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("Server failed to launch",
        // run the server in the same process
        // set this to false to fork
                   launchServer(JavaFirstPolicyServer.class, true));
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    
    @org.junit.Test
    public void testUsernameTokenInterceptorNoPasswordValidation() {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext("org/apache/cxf/systest/ws/policy/client/javafirstclient.xml");
        
        JavaFirstAttachmentPolicyService svc = 
            (JavaFirstAttachmentPolicyService) ctx.getBean("JavaFirstAttachmentPolicyServiceClient");
        
        Client client = ClientProxy.getClient(svc);
        client.getEndpoint().getEndpointInfo().setAddress(
                                "http://localhost:" + PORT + "/JavaFirstAttachmentPolicyService");
       
        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor();
        client.getEndpoint().getOutInterceptors().add(wssOut);
        
        // just some basic sanity tests first to make sure that auth is working where password is provided.
        wssOut.setProperties(getPasswordProperties("alice", "password"));
        svc.doInputMessagePolicy();
        
        wssOut.setProperties(getPasswordProperties("alice", "passwordX"));
        try {
            svc.doInputMessagePolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        wssOut.setProperties(getNoPasswordProperties("alice"));
        
        try {
            svc.doInputMessagePolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            assertTrue(true);
        }
    }
    
    @org.junit.Test
    public void testUsernameTokenPolicyValidatorNoPasswordValidation() {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext("org/apache/cxf/systest/ws/policy/client/javafirstclient.xml");
        
        SslUsernamePasswordAttachmentService svc = 
            (SslUsernamePasswordAttachmentService) ctx.getBean("SslUsernamePasswordAttachmentServiceClient");
        
        Client client = ClientProxy.getClient(svc);
        client.getEndpoint().getEndpointInfo().setAddress(
                                "https://localhost:" + PORT2 + "/SslUsernamePasswordAttachmentService");
       
        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor();
        client.getEndpoint().getOutInterceptors().add(wssOut);
        
        // just some basic sanity tests first to make sure that auth is working where password is provided.
        wssOut.setProperties(getPasswordProperties("alice", "password"));
        svc.doSslAndUsernamePasswordPolicy();
        
        wssOut.setProperties(getPasswordProperties("alice", "passwordX"));
        try {
            svc.doSslAndUsernamePasswordPolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        wssOut.setProperties(getNoPasswordProperties("alice"));
        
        try {
            svc.doSslAndUsernamePasswordPolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            assertTrue(true);
        }
    }
    
    private Map<String, Object> getPasswordProperties(String username, String password) {
        UTPasswordCallback callback = new UTPasswordCallback();
        callback.setAliasPassword(username, password);
        
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        outProps.put(WSHandlerConstants.PW_CALLBACK_REF, callback);
        outProps.put(WSHandlerConstants.USER, username);
        return outProps;
    }
    
    private Map<String, Object> getNoPasswordProperties(String username) {
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_NONE);
        outProps.put(WSHandlerConstants.USER, username);
        return outProps;
    }

}
