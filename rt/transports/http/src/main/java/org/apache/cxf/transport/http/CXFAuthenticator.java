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

package org.apache.cxf.transport.http;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.Conduit;

/**
 * 
 */
public class CXFAuthenticator extends Authenticator {
    static Authenticator wrapped;
    static boolean setup;
    
    
    public CXFAuthenticator() {
        try {
            for (Field f : Authenticator.class.getDeclaredFields()) {
                if (f.getType().equals(Authenticator.class)) {
                    f.setAccessible(true);
                    wrapped = (Authenticator)f.get(null);
                }
            }
        } catch (Throwable ex) {
            //ignore
        }
    }

    public static synchronized void addAuthenticator() { 
        if (!setup) {
            try {
                Authenticator.setDefault(new CXFAuthenticator());
            } catch (Throwable t) {
                //ignore
            }
            setup = true;
        }
    }
    
    protected PasswordAuthentication getPasswordAuthentication() { 
        PasswordAuthentication auth = null;
        if (wrapped != null) {
            try {
                for (Field f : Authenticator.class.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        f.set(wrapped, f.get(this));
                    }
                }
                Method m = Authenticator.class.getMethod("getPasswordAuthentication");
                m.setAccessible(true);
                auth = (PasswordAuthentication)m.invoke(wrapped);
            } catch (Throwable t) {
                //ignore
            }
        }
        if (auth == null) {
            Message m = PhaseInterceptorChain.getCurrentMessage();
            Exchange exchange = m.getExchange();
            Conduit conduit = exchange.getConduit(m);
            if (conduit instanceof HTTPConduit) {
                HTTPConduit httpConduit = (HTTPConduit)conduit;
                if (getRequestorType() == RequestorType.PROXY
                    && httpConduit.getProxyAuthorization() != null) {
                    
                    auth = new PasswordAuthentication(httpConduit.getProxyAuthorization().getUserName(),
                                                      httpConduit.getProxyAuthorization()
                                                          .getPassword().toCharArray());
                } else if (getRequestorType() == RequestorType.SERVER
                    && httpConduit.getAuthorization() != null) {
                    auth = new PasswordAuthentication(httpConduit.getAuthorization().getUserName(),
                                                      httpConduit.getAuthorization()
                                                          .getPassword().toCharArray());
                }
            }
        }
        return auth;
    }
}
