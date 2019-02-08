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

package org.apache.cxf.systest.jaxb;


import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.systest.jaxb.shareclasses.model.NameElement;
import org.apache.cxf.systest.jaxb.shareclasses.model.bar.BarName;
import org.apache.cxf.systest.jaxb.shareclasses.model.foo.FooName;
import org.apache.cxf.systest.jaxb.shareclasses.server.bar.BarServiceImpl;
import org.apache.cxf.systest.jaxb.shareclasses.server.foo.FooServiceImpl;
import org.apache.cxf.systest.jaxb.shareclasses.service.Getter;
import org.apache.cxf.systest.jaxb.shareclasses.service.bar.BarService;
import org.apache.cxf.systest.jaxb.shareclasses.service.foo.FooService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class MultipleServiceShareClassTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(MultipleServiceShareClassTest.class);
    static final String ADDRESS1 = "http://localhost:" + PORT + "/BarService";
    static final String ADDRESS2 = "http://localhost:" + PORT + "/FooService";

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            registerService(FooService.class, new FooServiceImpl());
            registerService(BarService.class, new BarServiceImpl());

        }

        private void registerService(final Class<?> service, final Object serviceImpl) {
            final JaxWsServerFactoryBean builder = new JaxWsServerFactoryBean();
            builder.setBus(getBus());
            builder.setAddress("http://localhost:" + PORT + "/" + service.getSimpleName());
            builder.setServiceBean(serviceImpl);
            builder.setServiceClass(service);
            builder.create();
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testCallMultipleService() throws Exception {

        final NameElement barName = new BarName();
        barName.setName("Bob");
        callBar(barName);
        final NameElement fooName = new FooName();
        fooName.setName("Alice");
        callFoo(fooName);

    }

    private void callFoo(final NameElement nameElement) {
        FooService fooClient = createGetterService(FooService.class);
        assertEquals(fooClient.getName(nameElement), "Alice");
    }

    private void callBar(final NameElement nameElement) {
        BarService barClient = createGetterService(BarService.class);
        assertEquals(barClient.getName(nameElement), "Bob");
    }

    private <T extends Getter> T createGetterService(final Class<T> serviceClass) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + PORT + "/" + serviceClass.getSimpleName());
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        return factory.create(serviceClass);
    }

}
