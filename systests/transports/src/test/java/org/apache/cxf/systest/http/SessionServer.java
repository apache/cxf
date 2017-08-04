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

package org.apache.cxf.systest.http;

import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class SessionServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(SessionServer.class);
    Bus bus;

    @Override
    protected void run() {
        String configurationFile = "SessionServer.xml";
        URL configure =
            SessionServer.class.getResource(configurationFile);
        bus = new SpringBusFactory().createBus(configure, true);
        BusFactory.setDefaultBus(bus);
    }

    @Override
    public void tearDown() {
        bus.shutdown(true);
        bus = null;
    }

    public static void main(String[] args) {
        try {
            // System.out.println("!!!!start");
            SessionServer s = new SessionServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

}
