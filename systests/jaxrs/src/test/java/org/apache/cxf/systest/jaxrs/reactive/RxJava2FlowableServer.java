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

package org.apache.cxf.systest.jaxrs.reactive;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.rx2.server.ReactiveIOCustomizer;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;


public class RxJava2FlowableServer extends AbstractServerTestServerBase {
    public static final String PORT = allocatePort(RxJava2FlowableServer.class);

    @Override
    protected Server createServer(Bus bus) throws Exception {
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);

        createFactoryBean(false, "/rx2").create();
        return createFactoryBean(true, "/rx22").create();
    }

    private JAXRSServerFactoryBean createFactoryBean(boolean useStreamingSubscriber,
                                                     String relAddress) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.getProperties(true).put("useStreamingSubscriber", useStreamingSubscriber);
        sf.setProvider(new JacksonJsonProvider());
        sf.setProvider(new IllegalArgumentExceptionMapper());
        sf.setProvider(new IllegalStateExceptionMapper());
        new ReactiveIOCustomizer().customize(sf);
        sf.getOutInterceptors().add(new LoggingOutInterceptor());
        sf.setResourceClasses(RxJava2FlowableService.class);
        sf.setResourceProvider(RxJava2FlowableService.class,
                               new SingletonResourceProvider(new RxJava2FlowableService(), true));
        sf.setAddress("http://localhost:" + PORT + relAddress);
        return sf;
    }

    public static void main(String[] args) throws Exception {
        new RxJava2FlowableServer().start();
    }

}
