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

package org.apache.cxf.systest.jaxrs.security.jose.jwt;

import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

public class BookServerJwtAuthnAuthz extends AbstractBusTestServerBase {
    public static final String PORT = TestUtil.getPortNumber("jaxrs-jwt-authn-authz");
    private static final URL SERVER_CONFIG_FILE =
        BookServerJwtAuthnAuthz.class.getResource("authn-authz-server.xml");

    protected void run() {
        SpringBusFactory bf = new SpringBusFactory();
        Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
        BusFactory.setDefaultBus(springBus);
        setBus(springBus);

        try {
            new BookServerJwtAuthnAuthz();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {
            BookServerJwtAuthnAuthz s = new BookServerJwtAuthnAuthz();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
