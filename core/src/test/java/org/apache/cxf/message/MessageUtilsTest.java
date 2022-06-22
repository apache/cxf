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
package org.apache.cxf.message;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.factory.SimpleMethodDispatcher;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MessageUtilsTest {


    @Test
    public void getTargetMethodFromBindingOperationInfo() throws Exception {
        Method method = MessageUtilsTest.class.getMethod("getTargetMethodFromBindingOperationInfo");
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        OperationInfo oi = new OperationInfo();
        oi.setName(QName.valueOf("getTargetMethod_fromBindingOperationInfo"));
        BindingOperationInfo boi = new BindingOperationInfo(null, oi);
        ServiceImpl serviceImpl = new ServiceImpl();
        MethodDispatcher md = new SimpleMethodDispatcher();
        md.bind(oi, method);

        serviceImpl.put(MethodDispatcher.class.getName(), md);
        exchange.put(Service.class, serviceImpl);
        exchange.put(BindingOperationInfo.class, boi);

        Optional<Method> optMethod = MessageUtils.getTargetMethod(message);
        assertTrue(optMethod.isPresent());
        assertEquals(method, optMethod.get());
    }

    @Test
    public void getTargetMethodFromProperty() throws Exception {
        Method method = MessageUtilsTest.class.getMethod("getTargetMethodFromProperty");
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put("org.apache.cxf.resource.method", method);

        Optional<Method> optMethod = MessageUtils.getTargetMethod(message);
        assertTrue(optMethod.isPresent());
        assertEquals(method, optMethod.get());
    }

    @Test
    public void getTargetMethodNoMethodNoException() throws Exception {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());

        assertFalse(MessageUtils.getTargetMethod(message).isPresent());
    }

    @Test
    public void getContextualIntegers() {
        Message message = new MessageImpl();
        message.put("key1", "1,2,invalid,3");
        MatcherAssert.assertThat(MessageUtils.getContextualIntegers(message, "key1", List.of(0)), Matchers.contains(1,2,3));
        MatcherAssert.assertThat(MessageUtils.getContextualIntegers(message, "invalid-key", List.of(0, 1)), Matchers.contains(0,1));
    }
}
