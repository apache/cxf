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

package org.apache.cxf.https.ssl3;

import java.net.URL;
import java.security.Security;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class SSLv3Server extends AbstractBusTestServerBase {

    public SSLv3Server() {
        // Remove "SSLv3" from the default disabled algorithm list for the purposes of this test
        Security.setProperty("jdk.tls.disabledAlgorithms", "MD5");
        if (JavaUtils.getJavaMajorVersion() >= 14) {
            // Since Java 14, the SSLv3 aliased to TLSv1 (so SSLv3 effectively is not
            // supported). To make it work, the custom SSL context has to be created and
            // SSLv3 and TLSv1 has to be explicitly enabled: 
            //   -Djdk.tls.client.protocols=SSLv3
            System.setProperty("jdk.tls.client.protocols", "SSLv3,TLSv1");
        }
    }

    protected void run()  {
        URL busFile = SSLv3Server.class.getResource("sslv3-server.xml");
        Bus busLocal = new SpringBusFactory().createBus(busFile);
        BusFactory.setDefaultBus(busLocal);
        setBus(busLocal);

        try {
            new SSLv3Server();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
