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

package org.apache.cxf.transport.websocket.jetty;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class JettyWebSocketManagerTest extends Assert {
    private IMocksControl control;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
    @Test
    public void testServiceUsingJettyDestination() throws Exception {
        JettyWebSocketManager jwsm = new JettyWebSocketManager();
        
        JettyHTTPDestination dest = control.createMock(JettyWebSocketDestination.class);
                
        HttpServletRequest request = control.createMock(HttpServletRequest.class);
        HttpServletResponse response = control.createMock(HttpServletResponse.class);
        
        dest.invoke(EasyMock.isNull(ServletConfig.class), EasyMock.anyObject(ServletContext.class), 
                    EasyMock.eq(request), EasyMock.eq(response));
        EasyMock.expectLastCall();
        control.replay();
        jwsm.init(dest);
        
        jwsm.service(request, response);
        control.verify();
    }

    @Test
    public void testServiceUsingServletDestination() throws Exception {
        JettyWebSocketManager jwsm = new JettyWebSocketManager();
        
        ServletDestination dest = control.createMock(JettyWebSocketServletDestination.class);
                
        HttpServletRequest request = control.createMock(HttpServletRequest.class);
        HttpServletResponse response = control.createMock(HttpServletResponse.class);
        
        dest.invoke(EasyMock.isNull(ServletConfig.class), EasyMock.anyObject(ServletContext.class), 
                    EasyMock.eq(request), EasyMock.eq(response));
        EasyMock.expectLastCall();
        control.replay();
        jwsm.init(dest);
        
        jwsm.service(request, response);
        control.verify();
    }
}
