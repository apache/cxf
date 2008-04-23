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
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientFactoryBean;
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
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.junit.Test;

public class ObjectBindingTest extends AbstractCXFTest {
    private Message response;

    @Test
    public void testServer() throws Exception {
        ServerFactoryBean sfb = new ServerFactoryBean();
        sfb.setBindingId(ObjectBindingFactory.BINDING_ID);
        sfb.setServiceClass(EchoImpl.class);
        sfb.setAddress("local://Echo");
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

        Conduit c = getLocalConduit("local://Echo");
        ex.setConduit(c);
        
        new ObjectDispatchOutInterceptor().handleMessage(m);


        ex.setConduit(c);

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

    @Test
    public void testClient() throws Exception {
        ClientFactoryBean cfb = new ClientFactoryBean();
        cfb.setBindingId(ObjectBindingFactory.BINDING_ID);
        cfb.setServiceClass(EchoImpl.class);
        cfb.setAddress("local://Echo");
        Client client = cfb.create();

        final List<Object> content = new ArrayList<Object>();
        content.add("Hello");

        final Destination d = getLocalDestination("local://Echo");

        d.setMessageObserver(new MessageObserver() {

            public void onMessage(Message inMsg) {
                // formulate the response message
                MessageImpl outMsg = new MessageImpl();
                outMsg.setContent(List.class, content);
                outMsg.put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);

                inMsg.getExchange().setInMessage(outMsg);
                try {
                    Conduit backChannel = d.getBackChannel(inMsg, null, null);
                    backChannel.prepare(outMsg);
                    backChannel.close(outMsg);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        Object[] res = client.invoke("echo", content.toArray());
        assertNotNull(res);
        assertEquals(1, res.length);
        assertEquals("Hello", res[0]);

    }

    @Test
    public void testClientServer() throws Exception {
        ClientFactoryBean cfb = new ClientFactoryBean();
        cfb.setBindingId(ObjectBindingFactory.BINDING_ID);
        cfb.setServiceClass(EchoImpl.class);
        cfb.setAddress("local://Echo");
        Client client = cfb.create();

        ServerFactoryBean sfb = new ServerFactoryBean();
        sfb.setBindingId(ObjectBindingFactory.BINDING_ID);
        sfb.setServiceClass(EchoImpl.class);
        sfb.setAddress("local://Echo");
        sfb.create();

        Object[] res = client.invoke("echo", new Object[] {"Hello"});
        assertNotNull(res);
        assertEquals(1, res.length);
        assertEquals("Hello", res[0]);
    }

    private Destination getLocalDestination(String string) throws BusException, IOException {
        DestinationFactoryManager dfm = getBus().getExtension(DestinationFactoryManager.class);

        DestinationFactory df = dfm.getDestinationFactory(LocalTransportFactory.TRANSPORT_ID);
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(string);

        return df.getDestination(ei);
    }

    private Conduit getLocalConduit(String string) throws BusException, IOException {
        ConduitInitiatorManager cim = getBus().getExtension(ConduitInitiatorManager.class);

        ConduitInitiator ci = cim.getConduitInitiator(LocalTransportFactory.TRANSPORT_ID);
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(string);
        return ci.getConduit(ei);
    }
}
