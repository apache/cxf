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

package org.apache.cxf.systest.jaxrs.logging;


import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class LoggingServer extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(LoggingServer.class);

    protected void run() {
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();

        sf.setResourceClasses(BookStore.class);
        sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore(), false));
        sf.setAddress("http://localhost:" + PORT + "/");

        sf.getFeatures().add(new LoggingFeature());

        sf.create();
    }

    public static void main(String[] args) {
        try {
            final LoggingServer s = new LoggingServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
