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

package org.apache.cxf.systest.jaxrs.reactive;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.rx3.server.ReactiveIOCustomizer;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;


public class RxJava3FlowableServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(RxJava3FlowableServer.class);

    org.apache.cxf.endpoint.Server server;
    org.apache.cxf.endpoint.Server server2;
    public RxJava3FlowableServer() {
    }

    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        // Make sure default JSONProvider is not loaded
        bus.setProperty("skip.default.json.provider.registration", true);
        server = createFactoryBean(bus, false, "/rx3").create();
        server = createFactoryBean(bus, true, "/rx33").create();
    }

    private JAXRSServerFactoryBean createFactoryBean(Bus bus, boolean useStreamingSubscriber,
                                                     String relAddress) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.getProperties(true).put("useStreamingSubscriber", useStreamingSubscriber);
        sf.setProvider(new JacksonJsonProvider());
        new ReactiveIOCustomizer().customize(sf);
        sf.getOutInterceptors().add(new LoggingOutInterceptor());
        sf.setResourceClasses(RxJava3FlowableService.class);
        sf.setResourceProvider(RxJava3FlowableService.class,
                               new SingletonResourceProvider(new RxJava3FlowableService(), true));
        sf.setAddress("http://localhost:" + PORT + relAddress);
        return sf;
    }

    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
        server = null;
    }

    public static void main(String[] args) {
        try {
            RxJava3FlowableServer s = new RxJava3FlowableServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
