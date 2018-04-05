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

package org.apache.cxf.systest.ldap.jaxrs;

import java.util.Collections;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

public class UserLDAPServer extends AbstractBusTestServerBase {
    public static final String PORT = TestUtil.getPortNumber("jaxrs-ldap");
    public static final String PORT2 = TestUtil.getPortNumber("jaxrs-ldap-2");

    protected void run() {
        // First server
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(UserService.class);
        sf.setResourceProvider(UserService.class,
                               new SingletonResourceProvider(new UserServiceImpl()));
        sf.setProviders(Collections.singletonList(new org.apache.cxf.jaxrs.ext.search.SearchContextProvider()));
        sf.setAddress("http://localhost:" + PORT + "/");

        sf.create();

        // Second server - don't encode query values
        sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(UserService.class);
        UserServiceImpl userService = new UserServiceImpl();
        userService.setEncodeQueryValues(false);
        sf.setResourceProvider(UserService.class,
                               new SingletonResourceProvider(userService));
        sf.setProviders(Collections.singletonList(new org.apache.cxf.jaxrs.ext.search.SearchContextProvider()));
        sf.setAddress("http://localhost:" + PORT2 + "/");

        sf.create();
    }

    public static void main(String[] args) {
        try {
            UserLDAPServer s = new UserLDAPServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
