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

package org.apache.cxf.systest.ws.policy;

import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.Assert;

public class JavaFirstPolicyServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(JavaFirstPolicyServer.class);
    public static final String PORT2 = allocatePort(JavaFirstPolicyServer.class, 2);
    public static final String PORT3 = allocatePort(JavaFirstPolicyServer.class, 3);

    protected void run()  {
        URL busFile = JavaFirstPolicyServer.class.getResource("javafirstserver.xml");
        Bus busLocal = new SpringBusFactory().createBus(busFile);
        BusFactory.setDefaultBus(busLocal);
        Assert.assertNotNull(busLocal);
        setBus(busLocal);
    }

    public static void main(String[] args) throws Exception {
        new JavaFirstPolicyServer().start();
    }
}
