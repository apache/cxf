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
package org.apache.cxf.systest.ws.wssec11.server;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.wssec11.RestrictedAlgorithmSuiteLoader;

public class StaxServer12Restricted extends AbstractServerRestricted {
    public static final String PORT = allocatePort(StaxServer12.class);

    public StaxServer12Restricted() throws Exception {
        super("http://localhost:" + PORT, true);
    }

    public StaxServer12Restricted(String baseUrl) throws Exception {
        super(baseUrl, true);
    }

    protected void run()  {
        Bus busLocal = new SpringBusFactory().createBus(
            "org/apache/cxf/systest/ws/wssec11/server.xml");
        new RestrictedAlgorithmSuiteLoader(busLocal);
        BusFactory.setDefaultBus(busLocal);
        setBus(busLocal);
        super.run();
    }

}
