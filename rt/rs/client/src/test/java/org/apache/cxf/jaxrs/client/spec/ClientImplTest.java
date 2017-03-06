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
package org.apache.cxf.jaxrs.client.spec;

import java.util.Arrays;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.ClientImpl.WebTargetImpl;
import org.apache.cxf.message.Message;

import org.junit.Assert;
import org.junit.Test;

public class ClientImplTest extends Assert {

    private static final String MY_INTERCEPTOR_NAME = "MyInterceptor";
    
    private static class MyInterceptor implements Interceptor<Message> {
        @Override
        public String toString() {
            return MY_INTERCEPTOR_NAME;
        }
        @Override
        public void handleMessage(Message message) throws Fault {
            // no-op
            
        }

        @Override
        public void handleFault(Message message) {
            // no-op
            
        }
    }
    
    /**
     * This test checks that we do not lose track of registered interceptors
     * on the original client implementation after we create a new impl with
     * the path(...) method - particularly when the path passed in to the
     * path(...) method contains a template.
     */
    @Test
    public void testClientConfigCopiedOnPathCallWithTemplates() {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target("http://localhost:8080/");
        WebClient webClient = getWebClient(webTarget);
        
        ClientConfiguration clientConfig = WebClient.getConfig(webClient);
        clientConfig.setOutInterceptors(Arrays.asList(new MyInterceptor()));
        assertTrue("Precondition failed - original WebTarget is missing expected interceptor",
                   doesClientConfigHaveMyInterceptor(webClient));
        
        WebTarget webTargetAfterPath = webTarget.path("/rest/{key}/").resolveTemplate("key", "myKey");
        WebClient webClientAfterPath = getWebClient(webTargetAfterPath);
        assertTrue("New WebTarget is missing expected interceptor specified on 'parent' WebTarget's client impl",
                   doesClientConfigHaveMyInterceptor(webClientAfterPath));

        
    }
    
    private WebClient getWebClient(WebTarget webTarget) {
        webTarget.request();
        WebTargetImpl webTargetImpl = (WebTargetImpl) webTarget;
        WebClient webClient = webTargetImpl.getWebClient();
        assertNotNull("No WebClient is associated with this WebTargetImpl", webClient);
        return webClient;
    }
    
    private boolean doesClientConfigHaveMyInterceptor(WebClient webClient) {
        ClientConfiguration clientConfigAfterPath = WebClient.getConfig(webClient);
        boolean foundMyInterceptor = false;
        for (Interceptor<?> i : clientConfigAfterPath.getOutInterceptors()) {
            if (MY_INTERCEPTOR_NAME.equals(i.toString())) {
                foundMyInterceptor = true;
                break;
            }
        }
        return foundMyInterceptor;
    }
    
    /**
     * Similar to <code>testClientConfigCopiedOnPathCallWithTemplates</code>,
     * this test uses a template, but in the initial call to target().  At this
     * point, the WebTargetImpl's targetClient field will be null, so we need
     * this test to ensure that there are no null pointers when creating and
     * using a template on the first call to target().
     */
    @Test
    public void testTemplateInInitialTarget() {
        String address = "http://localhost:8080/bookstore/{a}/simple";
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(address).resolveTemplate("a", "bookheaders");
        webTarget.request("application/xml").header("a", "b");
        WebClient webClient = getWebClient(webTarget);
        
        ClientConfiguration clientConfig = WebClient.getConfig(webClient);
        clientConfig.setOutInterceptors(Arrays.asList(new MyInterceptor()));
        assertTrue("Precondition failed - original WebTarget is missing expected interceptor",
                   doesClientConfigHaveMyInterceptor(webClient));
        
        WebTarget webTargetAfterPath = webTarget.path("/rest/{key}/").resolveTemplate("key", "myKey");
        WebClient webClientAfterPath = getWebClient(webTargetAfterPath);
        assertTrue("New WebTarget is missing expected interceptor specified on 'parent' WebTarget's client impl",
                   doesClientConfigHaveMyInterceptor(webClientAfterPath));

    }
}
