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
package org.apache.cxf.interceptor.security;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.easymock.EasyMock;

import org.junit.Before;

public class OperationInfoAuthorizingInterceptorTest extends SimpleAuthorizingInterceptorTest {

    @Before
    @Override
    public void setUp() throws Exception {
        Exchange ex = setUpExchange();
        Service service = EasyMock.createMock(Service.class);
        ex.put(Service.class, service);
        MethodDispatcher md = EasyMock.createMock(MethodDispatcher.class);
        EasyMock.expect(service.get(MethodDispatcher.class.getName())).andReturn(md).anyTimes();
        
        BindingOperationInfo boi = EasyMock.createMock(BindingOperationInfo.class);
        ex.put(BindingOperationInfo.class, boi);
        EasyMock.expect(md.getMethod(boi)).andReturn(null);
        OperationInfo opinfo = EasyMock.createMock(OperationInfo.class);
        EasyMock.expect(opinfo.getName()).andReturn(new QName("urn:test", "echo")).anyTimes();
        EasyMock.expect(boi.getOperationInfo()).andReturn(opinfo).anyTimes();
        EasyMock.replay(service, md, boi, opinfo);
    }
    
    @Override
    protected SimpleAuthorizingInterceptor createSimpleAuthorizingInterceptor() {
        return new OperationInfoAuthorizingInterceptor();
    }
    
    @Override
    protected SimpleAuthorizingInterceptor createSimpleAuthorizingInterceptorWithDenyRoles(final String role) {
        SimpleAuthorizingInterceptor in = new OperationInfoAuthorizingInterceptor() {
            @Override
            public List<String> getDenyRoles(String key) {
                return Collections.singletonList(role);
            }
        };
        return in;
    }
}
