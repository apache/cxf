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

package org.apache.cxf.systest.jaxws;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.catalog.CatalogWSDLLocator;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLManagerImpl;

import org.apache.hello_world.Greeter;
import org.apache.hello_world.GreeterImpl;
import org.apache.hello_world.services.SOAPService;

import org.junit.Assert;
import org.junit.Test;

public class OASISCatalogTest extends Assert {
    static final String PORT = TestUtil.getPortNumber(OASISCatalogTest.class);

    private final QName serviceName = 
        new QName("http://apache.org/hello_world/services",
                  "SOAPService");    

    private final QName portName = 
        new QName("http://apache.org/hello_world/services",
                  "SoapPort");

    @Test
    public void testWSDLPublishWithCatalogs() throws Exception {
        Endpoint ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort",
                                       new GreeterImpl());
        try {
            URL url = new URL("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                              + "xsd=hello_world_schema2.xsd");
            assertNotNull(url.getContent());
            String result = IOUtils.toString((InputStream)url.getContent());
            assertTrue(result, result.contains("xsd=hello_world_schema.xsd"));
            
            
            url = new URL("http://localhost:" + PORT + "/SoapContext/SoapPort"
                          + "?xsd=hello_world_schema.xsd");
            result = IOUtils.toString((InputStream)url.getContent());
            assertTrue(result, result.contains("xsd=hello_world_schema2.xsd"));

            url = new URL("http://localhost:" + PORT + "/SoapContext/SoapPort"
                          + "?wsdl=testutils/others/hello_world_messages_catalog.wsdl");
            result = IOUtils.toString((InputStream)url.getContent());
            assertTrue(result, result.contains("xsd=hello_world_schema.xsd"));

        } finally {
            ep.stop();
        }
    }
    
    @Test
    public void testClientWithDefaultCatalog() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/catalog/hello_world_services.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        assertNotNull(greeter);
    }

    @Test
    public void testClientWithoutCatalog() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/catalog/hello_world_services.wsdl");
        assertNotNull(wsdl);
        
        // set Catalog on the Bus
        Bus bus = BusFactory.getDefaultBus();
        OASISCatalogManager catalog = new OASISCatalogManager();
        bus.setExtension(catalog, OASISCatalogManager.class);
        // prevent cache from papering over the lack of a schema.
        WSDLManagerImpl mgr = (WSDLManagerImpl)bus.getExtension(WSDLManager.class);
        mgr.setDisableSchemaCache(true);
        
        try {
            SOAPService service = new SOAPService(wsdl, serviceName);
            service.getPort(portName, Greeter.class);
            fail("Test did not fail as expected");
        } catch (WebServiceException e) {
            // ignore
        }

        // update catalog dynamically now
        Enumeration<URL> jaxwscatalog = 
            getClass().getClassLoader().getResources("META-INF/jax-ws-catalog.xml");
        assertNotNull(jaxwscatalog);

        while (jaxwscatalog.hasMoreElements()) {
            URL url = jaxwscatalog.nextElement();
            catalog.loadCatalog(url);
        }

        SOAPService service = new SOAPService(wsdl, serviceName);
        Greeter greeter = service.getPort(portName, Greeter.class);
        assertNotNull(greeter);
        bus.shutdown(true);
    }

    @Test
    public void testWSDLLocatorWithDefaultCatalog() throws Exception {
        URL wsdl = 
            getClass().getResource("/wsdl/catalog/hello_world_services.wsdl");
        assertNotNull(wsdl);

        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
       
        CatalogWSDLLocator wsdlLocator =
            new CatalogWSDLLocator(wsdl.toString(),
                                   OASISCatalogManager.getCatalogManager(null));
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        wsdlReader.readWSDL(wsdlLocator);
    }

    @Test
    public void testWSDLLocatorWithoutCatalog() throws Exception {
        URL wsdl = 
            getClass().getResource("/wsdl/catalog/hello_world_services.wsdl");
        assertNotNull(wsdl);

        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
       
        OASISCatalogManager catalog = new OASISCatalogManager();
        CatalogWSDLLocator wsdlLocator =
            new CatalogWSDLLocator(wsdl.toString(), catalog);
        try {
            wsdlReader.readWSDL(wsdlLocator);
            fail("Test did not fail as expected");
        } catch (WSDLException e) {
            // ignore
        }
    }

}
