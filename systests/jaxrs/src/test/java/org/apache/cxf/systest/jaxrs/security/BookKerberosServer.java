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

package org.apache.cxf.systest.jaxrs.security;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.interceptor.security.NamePasswordCallbackHandler;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.security.KerberosAuthenticationFilter;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
    
public class BookKerberosServer extends AbstractBusTestServerBase {
    public static final String PORT = TestUtil.getPortNumber("jaxrs-kerberos");
    
    protected void run() {
        
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(BookStore.class);
        //default lifecycle is per-request, change it to singleton
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore()));
        KerberosAuthenticationFilter filter = new KerberosAuthenticationFilter();
        filter.setLoginContextName("KerberosServer");
        filter.setCallbackHandler(getCallbackHandler("HTTP/localhost", "http"));
        //filter.setLoginContextName("KerberosServerKeyTab");
        //filter.setServicePrincipalName("HTTP/ktab");
        sf.setProvider(filter);
        sf.setAddress("http://localhost:" + PORT + "/");
      
        sf.create();        
    }

    public static void main(String[] args) {
        try {
            BookKerberosServer s = new BookKerberosServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
    
    public static CallbackHandler getCallbackHandler(final String username, final String password) {
        return new NamePasswordCallbackHandler(username, password);
    }
}
