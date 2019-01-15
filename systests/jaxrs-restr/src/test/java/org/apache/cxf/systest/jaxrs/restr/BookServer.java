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

package org.apache.cxf.systest.jaxrs.restr;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class BookServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(BookServer.class);

    org.apache.cxf.endpoint.Server server;

    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        setBus(bus);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStore.class);
        List<Object> providers = new ArrayList<>();

        providers.add(new JacksonJsonProvider());
        sf.setProviders(providers);
        List<Interceptor<? extends Message>> inInts = new ArrayList<>();
        inInts.add(new LoggingInInterceptor());
        sf.setInInterceptors(inInts);
        List<Interceptor<? extends Message>> outInts = new ArrayList<>();
        outInts.add(new LoggingOutInterceptor());
        sf.setOutInterceptors(outInts);
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setAddress("http://localhost:" + PORT + "/");

        server = sf.create();
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
    }

    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
        server = null;
    }

    public static void main(String[] args) {
        try {
            BookServer s = new BookServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
