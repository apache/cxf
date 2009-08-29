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

package org.apache.cxf.systest.coloc;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;

import org.apache.commons.logging.Log;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public abstract class AbstractColocTest extends Assert {
    /**
     * Cxf Bus
     */
    protected Bus bus;

    /**
     * WS endpoint object
     */
    protected Endpoint endpoint;

    /**
     * Setup this test case
     */
    @Before
    public void setUp() throws Exception {
        // initialize Mule Manager
        URL cxfConfig = null;
        if (getCxfConfig() != null) {
            cxfConfig = ClassLoaderUtils.getResource(getCxfConfig(), AbstractColocTest.class);
            if (cxfConfig == null) {
                throw new Exception("Make sure " + getCxfConfig() + " is in the CLASSPATH");
            }
            assertTrue(cxfConfig.toExternalForm() != null);
        }

        //Bus is shared by client, router and server.
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus(cxfConfig);
        BusFactory.setDefaultBus(bus);

        //Start the Remote Server
        // the endpoint of the "real" cxf server
        endpoint = Endpoint.publish(getTransportURI(), getServiceImpl());
    }

    /**
     * Tear down this test case
     */
    @After
    public void tearDown() throws Exception {

        getLogger().debug("tearDown ...");

        if (endpoint != null) {
            endpoint.stop();
        }

        if (bus != null) {
            bus.shutdown(true);
        }
    }
    /**
     * @return cxf configuration file name
     */
    protected String getCxfConfig() {
        return "org/apache/cxf/systest/coloc/cxf.xml";
    }

    /**
     * Create client and return the port
     * @return port for a interface represented by cls.
     */
    protected <T> T getPort(QName serviceName, QName portName,
                                  String wsdlLocation, Class<T> cls) {
        Service srv = Service.create(
                         AbstractColocTest.class.getResource(wsdlLocation),
                         serviceName);
        return srv.getPort(portName, cls);
    }
    /**
     * @return the greeter impl object
     */
    protected abstract Object getServiceImpl();

    /**
     * @return logger object
     */
    protected abstract Log getLogger();

    /**
     * @return transport URI for the WS Endpoint
     */
    protected abstract String getTransportURI();


}
