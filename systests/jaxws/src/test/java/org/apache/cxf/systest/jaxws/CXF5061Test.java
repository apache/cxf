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

package org.apache.cxf.systest.jaxws;



import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CXF5061Test extends AbstractBusClientServerTestBase {

    public static final String ADDRESS
        = "http://localhost:" + TestUtil.getPortNumber("org.apache.cxf.systest.jaxws.CXF5061Test")
            + "/cxf5061";

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            new SpringBusFactory().createBus("org/apache/cxf/systest/jaxws/springWebService.xml");
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

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testCxf5061() throws Exception {
        //using dcf to generate client from the wsdl which ensure the wsdl is valid
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        dcf.createClient(ADDRESS + "?wsdl");
    }

}
