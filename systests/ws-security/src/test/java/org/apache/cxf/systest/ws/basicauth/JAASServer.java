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

package org.apache.cxf.systest.ws.basicauth;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.security.JAASLoginInterceptor;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class JAASServer extends AbstractBusTestServerBase {

    public JAASServer() {

    }

    protected void run()  {
        URL busFile = JAASServer.class.getResource("server-continuation.xml");
        Bus busLocal = new SpringBusFactory().createBus(busFile);
        BusFactory.setDefaultBus(busLocal);
        busLocal.getInInterceptors().add(this.createTestJaasLoginInterceptor());
        busLocal.getInInterceptors().add(new BeforeServiceInvokerInterceptor());
        setBus(busLocal);

        try {
            new JAASServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private JAASLoginInterceptor createTestJaasLoginInterceptor() {
        JAASLoginInterceptor jaasInt = new JAASLoginInterceptor();
        jaasInt.setReportFault(true);
        Configuration config = new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, String> options = new HashMap<>();
                AppConfigurationEntry configEntry = new AppConfigurationEntry(
                                                                              TestUserPasswordLoginModule.class
                                                                                  .getName(),
                                                                              LoginModuleControlFlag.REQUIRED,
                                                                              options);
                return Collections.singleton(configEntry).toArray(new AppConfigurationEntry[] {});
            }
        };
        jaasInt.setLoginConfig(config);
        return jaasInt;
    }
}
