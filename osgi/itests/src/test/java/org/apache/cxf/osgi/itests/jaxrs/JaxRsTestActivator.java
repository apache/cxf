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
package org.apache.cxf.osgi.itests.jaxrs;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class JaxRsTestActivator implements BundleActivator {
//    public static final String PORT = TestUtil.getPortNumber(JaxRsTestActivator.class);
    private Server server;

    @Override
    public void start(BundleContext arg0) throws Exception {
        Bus bus = BusFactory.newInstance().createBus();
        bus.setExtension(JaxRsTestActivator.class.getClassLoader(), ClassLoader.class);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStore.class);
        sf.setAddress("/jaxrs");
        server = sf.create();
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        server.stop();
        server.destroy();
    }

}
