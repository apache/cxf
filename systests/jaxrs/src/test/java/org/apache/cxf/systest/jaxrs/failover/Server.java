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

package org.apache.cxf.systest.jaxrs.failover;


import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {
    public static final String PORT1 = allocatePort(Server.class, 0);
    public static final String PORT2 = allocatePort(Server.class, 1);
    public static final String PORT3 = allocatePort(Server.class, 3);

    public static final String ADDRESS1 = "http://localhost:" + PORT1 + "/rest";
    public static final String ADDRESS2 = "http://localhost:" + PORT2 + "/rest";
    public static final String ADDRESS3 = "http://localhost:" + PORT3 + "/work/rest";


    List<org.apache.cxf.endpoint.Server> servers = new ArrayList<>();

    protected void run()  {
        Bus bus = getBus();

        if (bus == null) {
            bus = new ExtensionManagerBus();
        }

        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);

        createEndpoint(ADDRESS2, bus);
        createEndpoint(ADDRESS3, bus);
    }
    public void tearDown() throws Exception {
        for (org.apache.cxf.endpoint.Server s : servers) {
            s.stop();
            s.destroy();
        }
        servers.clear();
    }

    private void createEndpoint(String address, Bus bus) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(BookStore.class);
        sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore(), false));
        sf.setAddress(address);
        sf.setBus(bus);
        servers.add(sf.create());
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
