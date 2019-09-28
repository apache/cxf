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

package org.apache.cxf.systest.jaxrs.reactor;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.reactor.server.ReactorCustomizer;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class ReactorServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(ReactorServer.class);

    org.apache.cxf.endpoint.Server server1;
    org.apache.cxf.endpoint.Server server2;
    
    @Override
    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        // Make sure default JSONProvider is not loaded
        bus.setProperty("skip.default.json.provider.registration", true);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.getProperties(true).put("useStreamingSubscriber", false);
        sf.setProvider(new JacksonJsonProvider());
        new ReactorCustomizer().customize(sf);
        sf.setResourceClasses(FluxService.class, MonoService.class);
        sf.setResourceProvider(FluxService.class,
                new SingletonResourceProvider(new FluxService(), true));
        sf.setResourceProvider(MonoService.class,
                new SingletonResourceProvider(new MonoService(), true));
        sf.setAddress("http://localhost:" + PORT + "/reactor");
        server1 = sf.create();
        
        JAXRSServerFactoryBean sf2 = new JAXRSServerFactoryBean();
        sf2.setProvider(new JacksonJsonProvider());
        sf2.setProvider(new IllegalArgumentExceptionMapper());
        new ReactorCustomizer().customize(sf2);
        sf2.setResourceClasses(FluxService.class);
        sf2.setResourceProvider(FluxService.class,
                new SingletonResourceProvider(new FluxService(), true));
        sf2.setAddress("http://localhost:" + PORT + "/reactor2");
        server2 = sf2.create();
    }

    @Override
    public void tearDown() throws Exception {
        server1.stop();
        server1.destroy();
        server1 = null;
        
        server2.stop();
        server2.destroy();
        server2 = null;
    }

    public static void main(String[] args) {
        ReactorServer server = new ReactorServer();
        System.out.println("Go to http://localhost:" + PORT + "/reactor/flux/textJsonImplicitListAsyncStream");
        server.start();

    }
}
