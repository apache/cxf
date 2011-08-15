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

package org.apache.cxf.systest.jaxrs.security.saml;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

import org.apache.cxf.rs.security.saml.SamlHeaderInHandler;
import org.apache.cxf.systest.jaxrs.security.BookStore;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
    
public class BookServerSaml extends AbstractBusTestServerBase {
    public static final String PORT = TestUtil.getPortNumber("jaxrs-saml");
    private static final String SERVER_CONFIG_FILE =
        "org/apache/cxf/systest/jaxrs/security/saml/server.xml";
    
    protected void run() {
        SpringBusFactory bf = new SpringBusFactory();
        Bus springBus = bf.createBus(SERVER_CONFIG_FILE);
        BusFactory.setDefaultBus(springBus);
        
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        
        sf.setResourceClasses(BookStore.class);
        
        sf.setProvider(new SamlHeaderInHandler());
        
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setAddress("https://localhost:" + PORT + "/");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("ws-security.callback-handler", 
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put("ws-security.signature.properties", 
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        sf.setProperties(properties);
        
        sf.create();        
    }

    public static void main(String[] args) {
        try {
            BookServerSaml s = new BookServerSaml();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
