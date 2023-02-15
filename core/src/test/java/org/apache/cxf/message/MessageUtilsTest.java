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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.factory.SimpleMethodDispatcher;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
        message.put("key1", "1, 2,invalid,3");
        assertThat(MessageUtils.getContextualIntegers(message, "key1", Arrays.asList(0)),
            contains(1, 2, 3));
        assertThat(MessageUtils.getContextualIntegers(message, "invalid-key", Arrays.asList(0, 1)), 
            contains(0, 1));
    }

    @Test
    public void getContextualStrings() {
        Message message = new MessageImpl();
        String key = "key1";
        message.put(key, "aaaa, bbb  ,  cc, d");
        Set<String> contextualStrings = MessageUtils.getContextualStrings(message, key, Collections.emptySet());
        assertEquals(4, contextualStrings.size());
        assertTrue(contextualStrings.remove("aaaa"));
        assertTrue(contextualStrings.remove("bbb"));
        assertTrue(contextualStrings.remove("cc"));
        assertTrue(contextualStrings.remove("d"));
        assertTrue(contextualStrings.isEmpty());

        Set<String> defaults = new TreeSet<>();
        defaults.add("aaa");
        defaults.add("zzz");
        defaults.add("eee");
        Set<String> contextualStringsDefault = MessageUtils.getContextualStrings(message, "unknownKey", defaults);
        assertEquals(defaults, contextualStringsDefault);
    }
}
