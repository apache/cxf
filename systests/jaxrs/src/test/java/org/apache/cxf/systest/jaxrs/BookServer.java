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

package org.apache.cxf.systest.jaxrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.BinaryDataProvider;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
    
public class BookServer extends AbstractBusTestServerBase {

    protected void run() {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(BookStore.class, BookStorePerRequest.class);
        
        List<Object> providers = new ArrayList<Object>();
        
        //default lifecycle is per-request, change it to singleton
        BinaryDataProvider p = new BinaryDataProvider();
        p.setProduceMediaTypes(Collections.singletonList("application/bar"));
        p.setEnableBuffering(true);
        
        providers.add(p);
        providers.add(new GenericHandlerWriter());
        sf.setProviders(providers);
        List<Interceptor> ints = new ArrayList<Interceptor>();
        ints.add(new CustomOutInterceptor());
        sf.setOutInterceptors(ints);
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setAddress("http://localhost:9080/");

        sf.create();        
    }

    public static void main(String[] args) {
        try {
            BookServer s = new BookServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
