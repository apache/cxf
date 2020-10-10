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


public class RxJava3SingleServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(RxJava3SingleServer.class);

    org.apache.cxf.endpoint.Server server;
    public RxJava3SingleServer() {
    }

    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        // Make sure default JSONProvider is not loaded
        bus.setProperty("skip.default.json.provider.registration", true);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setProvider(new JacksonJsonProvider());
        new ReactiveIOCustomizer().customize(sf);
        sf.getOutInterceptors().add(new LoggingOutInterceptor());
        sf.setResourceClasses(RxJava3SingleService.class);
        sf.setResourceProvider(RxJava3SingleService.class,
                               new SingletonResourceProvider(new RxJava3SingleService(), true));
        sf.setAddress("http://localhost:" + PORT + "/");
        server = sf.create();
    }

    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
        server = null;
    }

    public static void main(String[] args) {
        try {
            RxJava3SingleServer s = new RxJava3SingleServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
