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

import java.util.Collections;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.StreamingResponseProvider;
import org.apache.cxf.jaxrs.rx.server.ObservableInvoker;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;


public class ObservableServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(ObservableServer.class);

    org.apache.cxf.endpoint.Server server;
    public ObservableServer() {
    }

    protected void run() {
        Bus bus = BusFactory.getDefaultBus();
        // Make sure default JSONProvider is not loaded
        bus.setProperty("skip.default.json.provider.registration", true);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setInvoker(new ObservableInvoker());
        sf.setProvider(new JacksonJsonProvider());
        StreamingResponseProvider<HelloWorldBean> streamProvider = new StreamingResponseProvider<HelloWorldBean>();
        streamProvider.setProduceMediaTypes(Collections.singletonList("application/json"));
        sf.setProvider(streamProvider);
        sf.getOutInterceptors().add(new LoggingOutInterceptor());
        sf.setResourceClasses(ObservableService.class);
        sf.setResourceProvider(ObservableService.class,
                               new SingletonResourceProvider(new ObservableService(), true));
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
            ObservableServer s = new ObservableServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
