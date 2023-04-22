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

package org.apache.cxf.systest.hc5.https.trust;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.systest.hc5.GreeterImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;

public class TrustServerNoSpring extends AbstractBusTestServerBase {

    public TrustServerNoSpring() {

    }

    protected void run()  {
        Bus busLocal = BusFactory.getDefaultBus(true);
        setBus(busLocal);

        String address = "https://localhost:" + TrustManagerTest.PORT3 + "/SoapContext/HttpsPort";

        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/Bethal.jks",
                                                               this.getClass()),
                          "password".toCharArray());

            KeyManagerFactory kmf  =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());

            TLSServerParameters tlsParams = new TLSServerParameters();
            tlsParams.setKeyManagers(kmf.getKeyManagers());

            ClientAuthentication clientAuthentication = new ClientAuthentication();
            clientAuthentication.setRequired(false);
            clientAuthentication.setWant(true);
            tlsParams.setClientAuthentication(clientAuthentication);

            Map<String, TLSServerParameters> map = new HashMap<>();
            map.put("tlsId", tlsParams);

            JettyHTTPServerEngineFactory factory =
                busLocal.getExtension(JettyHTTPServerEngineFactory.class);
            factory.setTlsServerParametersMap(map);
            factory.createJettyHTTPServerEngine("localhost", Integer.parseInt(TrustManagerTest.PORT3),
                                                "https", "tlsId");

            factory.initComplete();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Endpoint.publish(address, new GreeterImpl());
    }
}
