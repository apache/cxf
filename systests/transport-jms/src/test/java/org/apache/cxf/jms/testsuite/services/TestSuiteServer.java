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
package org.apache.cxf.jms.testsuite.services;

import java.util.LinkedList;
import java.util.List;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class TestSuiteServer extends AbstractBusTestServerBase {

    private static String jndiUrl;

    List<Endpoint> endpoints = new LinkedList<>();
    protected void run() {
        setBus(BusFactory.getDefaultBus());
        startEndpoint("test0001", new Test0001Impl());
        startEndpoint("test0101", new Test0101Impl());
        startEndpoint("test0003", new Test0003Impl());
        startEndpoint("test0005", new Test0005Impl());
        startEndpoint("test0006", new Test0006Impl());
        startEndpoint("test0008", new Test0008Impl());
        startEndpoint("test0009", new Test0009Impl());
        startEndpoint("test0010", new Test0010Impl());
        startEndpoint("test0011", new Test0011Impl());
        startEndpoint("test0012", new Test0012Impl());
        startEndpoint("test0013", new Test0013Impl());
        startEndpoint("test0014", new Test0014Impl());
        startEndpoint("test1001", new Test1001Impl());
        startEndpoint("test1002", new Test1002Impl());
        startEndpoint("test1003", new Test1003Impl());
        startEndpoint("test1004", new Test1004Impl());
        startEndpoint("test1006", new Test1006Impl());
        startEndpoint("test1007", new Test1007Impl());
        startEndpoint("test1008", new Test1008Impl());
        startEndpoint("test1101", new Test1101Impl());
        startEndpoint("test1102", new Test1102Impl());
        startEndpoint("test1103", new Test1103Impl());
        startEndpoint("test1104", new Test1104Impl());
        startEndpoint("test1105", new Test1105Impl());
        startEndpoint("test1106", new Test1106Impl());
        startEndpoint("test1107", new Test1107Impl());
        startEndpoint("test1108", new Test1108Impl());
    }

    private void startEndpoint(String endpointName, Object testImpl) {
        String partAddress = JMSTestUtil.getTestCase(endpointName).getAddress().trim();
        String address = JMSTestUtil.getFullAddress(partAddress, jndiUrl);
        endpoints.add(Endpoint.publish(address, testImpl));
    }

    public void tearDown() {
        for (Endpoint ep : endpoints) {
            ep.stop();
        }
        endpoints.clear();
    }
    public static void main(String[] args) {
        try {
            TestSuiteServer s = new TestSuiteServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

    public static void setJndiUrl(String jndiUrl) {
        TestSuiteServer.jndiUrl = jndiUrl;
    }
}
