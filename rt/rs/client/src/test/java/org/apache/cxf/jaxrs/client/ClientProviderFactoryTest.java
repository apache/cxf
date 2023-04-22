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

package org.apache.cxf.jaxrs.client;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertSame;


public class ClientProviderFactoryTest {

    @Test
    public void testParameterHandlerProviderWithPriority() throws Exception {
        final Bus bus = new ExtensionManagerBus();
        final ProviderFactory pf = ClientProviderFactory.createInstance(bus);
        
        ParamConverterProvider h = new CustomerParameterHandler();
        ParamConverterProvider hp = new PriorityCustomerParameterHandler();
        pf.registerUserProvider(h);
        pf.registerUserProvider(hp);
        ParamConverter<Customer> h2 = pf.createParameterHandler(Customer.class, Customer.class, null,
                                                                new MessageImpl());
        assertSame(h2, hp);
    }

    @Test
    public void testParameterHandlerProviderWithConstraints() throws Exception {
        final Bus bus = new ExtensionManagerBus();
        final ProviderFactory pf = ClientProviderFactory.createInstance(bus);

        @ConstrainedTo(RuntimeType.SERVER)
        class ServerParameterHandler extends CustomerParameterHandler {
            // Server parameter handler
        }

        @ConstrainedTo(RuntimeType.CLIENT)
        class ClientParameterHandler extends CustomerParameterHandler {
            // Client parameter handler
        }

        ParamConverterProvider h = new ServerParameterHandler();
        ParamConverterProvider hp = new ClientParameterHandler();
        pf.registerUserProvider(h);
        pf.registerUserProvider(hp);
        ParamConverter<Customer> h2 = pf.createParameterHandler(Customer.class, Customer.class, null,
                                                                new MessageImpl());
        assertSame(h2, hp);
    }
}
