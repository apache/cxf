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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.WebServiceException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.CatalogWSDLLocator;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.GreeterImpl;
import org.apache.hello_world.services.SOAPService;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OASISCatalogTest {
    static final String PORT = TestUtil.getPortNumber(OASISCatalogTest.class);

    private final QName serviceName =
        new QName("http://apache.org/hello_world/services",
                  "SOAPService");

    private final QName portName =
        new QName("http://apache.org/hello_world/services",
                  "SoapPort");

    @Test
    public void testWSDLPublishSamePath() throws Exception {
        Endpoint ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort",
                                       new GreeterImpl());

        try {
            String result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "wsdl=http://apache.org/hello_world/types2/hello_world_messages_catalog.wsdl");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/d/shared.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/types2/d/shared.xsd");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/common.xsd"));
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/d/common.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/types2/common.xsd");
            assertFalse(result, result.contains("schemaLocation"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/types2/d/common.xsd");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/common.xsd"));
        } finally {
            ep.stop();
        }
    }

    @Test
    public void testWSDLPublishWithCatalogs() throws Exception {
        Endpoint ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort",
                                       new GreeterImpl());

        try {
            String result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=hello_world_schema2.xsd");
            assertTrue(result, result.contains("xsd=hello_world_schema.xsd"));
            assertTrue(result, result.contains("xsd=hello_world_schema3.xsd"));
            assertTrue(result, result.contains("xsd=d/hello_world_schema4.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=hello_world_schema3.xsd");
            assertTrue(result.length() > 0);

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=d/hello_world_schema4.xsd");
            assertTrue(result, result.contains("xsd=d/d/hello_world_schema4.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort"
                    + "?xsd=hello_world_schema.xsd");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/hello_world_schema2.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort"
                    + "?wsdl=hello_world_messages_catalog.wsdl");
            assertTrue(result, result.contains("xsd=hello_world_schema.xsd"));
        } finally {
            ep.stop();
        }
    }

    /**
     * This is test case for https://issues.apache.org/jira/browse/CXF-6234
     *
     * It's using paths that will be rewritten by following catalog rule:
     *
     *     &lt;rewriteSystem systemIdStartString="http://apache.org/hello_world/types2/"
     *          rewritePrefix="/wsdl/others/"/&gt;
     *
     */
    @Test
    public void testWSDLPublishWithCatalogsRewritePaths() throws Exception {
        Endpoint ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort",
                new GreeterImpl());
        try {
            // schemas in the same directory as WSDL

            String result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/types2/hello_world_schema2.xsd");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/hello_world_schema.xsd"));
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/hello_world_schema3.xsd"));
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/d/hello_world_schema4.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/types2/hello_world_schema.xsd");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/hello_world_schema2.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/types2/hello_world_schema3.xsd");
            assertTrue(result.length() > 0);

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/types2/d/hello_world_schema4.xsd");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/types2/d/d/hello_world_schema4.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/types2/d/d/hello_world_schema4.xsd");
            assertFalse(result.contains("schemaLocation"));

            // schemas in separate directory which is not subdirectory of WSDL dir

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "wsdl=http://apache.org/hello_world/types2/hello_world_messages_catalog.wsdl");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/schemas-in-separate-dir/schema.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/schemas-in-separate-dir/schema.xsd");
            assertTrue(result,
                    result.contains("xsd=http://apache.org/hello_world/schemas-in-separate-dir/d/included.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/schemas-in-separate-dir/d/included.xsd");
            assertTrue(result,
                    result.contains("xsd=http://apache.org/hello_world/schemas-in-separate-dir/d/d/included.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/schemas-in-separate-dir/d/d/included.xsd");
            assertFalse(result, result.contains("schemaLocation"));

            // rewrite rule that doesn't begin with 'classpath:' but contains only the path

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/schemas-in-separate-dir-non-cp/another-schema.xsd");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/schemas-in-separate-dir-non-cp/d/"
                    + "another-included.xsd"));

            result = readUrl("http://localhost:" + PORT + "/SoapContext/SoapPort?"
                    + "xsd=http://apache.org/hello_world/schemas-in-separate-dir-non-cp/d/another-included.xsd");
            assertTrue(result, result.contains("xsd=http://apache.org/hello_world/schemas-in-separate-dir-non-cp/d/d/"
                    + "another-included.xsd"));
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

    private static String readUrl(String address) throws IOException {
        try (InputStream is = new URL(address).openStream()) {
            return IOUtils.toString(is);
        }
    }

}