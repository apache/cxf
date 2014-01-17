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

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.Conduit;

/**
 * 
 */
public class CXFAuthenticator extends Authenticator {
    static CXFAuthenticator instance;
    
    
    public CXFAuthenticator() {
    }
    
    public static synchronized void addAuthenticator() { 
        if (instance == null) {
            instance = new CXFAuthenticator();
            Authenticator wrapped = null;
            for (final Field f : Authenticator.class.getDeclaredFields()) {
                if (f.getType().equals(Authenticator.class)) {
                    ReflectionUtil.setAccessible(f);
                    try {
                        wrapped = (Authenticator)f.get(null);
                        if (wrapped != null 
                            && wrapped.getClass().getName().equals(ReferencingAuthenticator.class.getName())) {
                            Method m = wrapped.getClass().getMethod("check");
                            m.setAccessible(true);
                            m.invoke(wrapped);
                        }
                        wrapped = (Authenticator)f.get(null);
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
            
            try {
                ClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                        public ClassLoader run() {
                            return new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
                        }
                    }, null);
                
                
                Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, 
                                                               byte[].class, Integer.TYPE, Integer.TYPE);
                
                InputStream ins = ReferencingAuthenticator.class
                        .getResourceAsStream("ReferencingAuthenticator.class");
                byte b[] = IOUtils.readBytesFromStream(ins);
                
                ReflectionUtil.setAccessible(m).invoke(loader, ReferencingAuthenticator.class.getName(),
                                                       b, 0, b.length);
                Class<?> cls = loader.loadClass(ReferencingAuthenticator.class.getName());
                Authenticator auth = (Authenticator)cls.getConstructor(Authenticator.class, Authenticator.class)
                    .newInstance(instance, wrapped);
                
                if (System.getSecurityManager() == null) {
                    Authenticator.setDefault(auth);
                } else {
                    AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            Authenticator.setDefault(auth);
                            return true;
                        }
                    });

                }
                try {
                    //clear the acc field that can hold onto the webapp classloader
                    Field f = loader.getClass().getDeclaredField("acc");
                    ReflectionUtil.setAccessible(f).set(loader, null);
                } catch (Throwable t) {
                    //ignore
                }
            } catch (Throwable t) {
                //ignore
            }
        }
    }
    
    protected PasswordAuthentication getPasswordAuthentication() { 
        PasswordAuthentication auth = null;
        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (m != null) {
            Exchange exchange = m.getExchange();
            Conduit conduit = exchange.getConduit(m);
            if (conduit instanceof HTTPConduit) {
                HTTPConduit httpConduit = (HTTPConduit)conduit;
                if (getRequestorType() == RequestorType.PROXY
                    && httpConduit.getProxyAuthorization() != null) {
                    String un = httpConduit.getProxyAuthorization().getUserName();
                    String pwd =  httpConduit.getProxyAuthorization().getPassword();
                    if (un != null && pwd != null) {
                        auth = new PasswordAuthentication(un, pwd.toCharArray());
                    }
                } else if (getRequestorType() == RequestorType.SERVER
                    && httpConduit.getAuthorization() != null) {
                    if ("basic".equals(getRequestingScheme()) || "digest".equals(getRequestingScheme())) {
                        return null;
                    }

                    String un = httpConduit.getAuthorization().getUserName();
                    String pwd =  httpConduit.getAuthorization().getPassword();
                    if (un != null && pwd != null) {
                        auth = new PasswordAuthentication(un, pwd.toCharArray());
                    }
                }
            }
        }
        // else PhaseInterceptorChain.getCurrentMessage() is null,
        // this HTTP call has therefore not been generated by CXF
        return auth;
    }
}
