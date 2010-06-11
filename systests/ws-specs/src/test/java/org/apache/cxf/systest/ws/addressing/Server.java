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

package org.apache.cxf.systest.ws.addressing;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase implements VerificationCache {

    private String verified;
    private String address;
    private Class<?> cls;
 
    public Server(String[] args) throws Exception {
        address = args[0];
        cls = Class.forName(args[1]);
    }
    
    protected void run() {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/ws/addressing/server.xml");
        BusFactory.setDefaultBus(bus);
        setBus(bus);

        addVerifiers();

        try {
            AbstractGreeterImpl implementor = (AbstractGreeterImpl)cls.newInstance();
            implementor.verificationCache = this;
            Endpoint.publish(address, implementor);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void addVerifiers() {
        MAPVerifier mapVerifier = new MAPVerifier();
        mapVerifier.verificationCache = this;
        HeaderVerifier headerVerifier = new HeaderVerifier();
        headerVerifier.verificationCache = this;
        Interceptor[] interceptors = {mapVerifier, headerVerifier};
        addInterceptors(getBus().getInInterceptors(), interceptors);
        addInterceptors(getBus().getInFaultInterceptors(), interceptors);
        addInterceptors(getBus().getOutInterceptors(), interceptors);
        addInterceptors(getBus().getOutFaultInterceptors(), interceptors);
    }

    private void addInterceptors(List<Interceptor> chain,
                                 Interceptor[] interceptors) {
        for (int i = 0; i < interceptors.length; i++) {
            chain.add(interceptors[i]);
        }
    }

        
    public static void main(String[] args) {
        try { 
            Server s = new Server(args); 
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally { 
            System.out.println("done!");
        }
    }

    public void put(String verification) {
        if (verification != null) {
            verified = verified == null
                       ? verification
                : verified + "; " + verification;
        }
    }

    /**
     * Used to facilitate assertions on server-side behaviour.
     *
     * @param log logger to use for diagnostics if assertions fail
     * @return true if assertions hold
     */
    protected boolean verify(Logger log) {
        if (verified != null) {
            System.out.println("MAP/Header verification failed: " + verified);
            log.log(Level.WARNING, 
                    "MAP/Header verification failed: {0}",
                    verified);
        }
        return verified == null;
    }
}
