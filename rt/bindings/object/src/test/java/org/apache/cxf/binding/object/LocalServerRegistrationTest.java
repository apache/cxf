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
package org.apache.cxf.binding.object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.junit.Test;

public class LocalServerRegistrationTest extends AbstractCXFTest {
    private Message response;

    @Test
    public void testServer() throws Exception {
        // Enable the auto registration of a default local endpoint when we use other transports
        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
        ObjectBindingFactory obj = (ObjectBindingFactory)
            bfm.getBindingFactory(ObjectBindingFactory.BINDING_ID);
        obj.setAutoRegisterLocalEndpoint(true);
        
        // Create an HTTP endpoint
        ServerFactoryBean sfb = new ServerFactoryBean();
        sfb.setServiceClass(EchoImpl.class);
        sfb.setAddress("http://localhost:9001/echo");
        Server server = sfb.create();

        List<Object> content = new ArrayList<Object>();
        content.add("Hello");

        ServiceInfo serviceInfo = server.getEndpoint().getEndpointInfo().getService();
        BindingInfo bi = serviceInfo.getBindings().iterator().next();
        BindingOperationInfo bop = bi.getOperations().iterator().next();

        assertNotNull(bop.getOperationInfo());       
        
        MessageImpl m = new MessageImpl();
        m.setContent(List.class, content);
        ExchangeImpl ex = new ExchangeImpl();
        ex.setInMessage(m);
        ex.put(BindingOperationInfo.class, bop);

        Conduit c = getLocalConduit("local://" + server);
        ex.setConduit(c);
        
        new ObjectDispatchOutInterceptor().handleMessage(m);
        


        c.setMessageObserver(new MessageObserver() {
            public void onMessage(Message message) {
                response = message;
            }
        });
        c.prepare(m);
        c.close(m);
        
        Thread.sleep(1000);
        assertNotNull(response);

        List<?> content2 = CastUtils.cast((List<?>)response.getContent(List.class));
        assertNotNull(content2);
        assertEquals(1, content2.size());

    }

    private Conduit getLocalConduit(String string) throws BusException, IOException {
        ConduitInitiatorManager cim = getBus().getExtension(ConduitInitiatorManager.class);

        ConduitInitiator ci = cim.getConduitInitiator(LocalTransportFactory.TRANSPORT_ID);
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(string);
        return ci.getConduit(ei);
    }
}
