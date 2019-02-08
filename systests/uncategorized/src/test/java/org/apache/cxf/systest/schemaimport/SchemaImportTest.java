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
package org.apache.cxf.systest.schemaimport;

import java.io.InputStream;
import java.net.URL;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SchemaImportTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;

    @BeforeClass
    public static void startservers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testImportSchema() throws Exception {
        String schemaURL = "http://localhost:" + PORT + "/schemaimport/sayHi" + "?xsd=sayhi/sayhi/sayhi-schema1.xsd";
        URL url = new URL(schemaURL);
        try (InputStream ins = url.openStream()) {
            String output = IOUtils.toString(ins);
            assertTrue(output.indexOf("sayHiArray") > -1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Can not access the import schema");
        }
    }

    @Test
    public void testImportSchema2() throws Exception {
        String schemaURL = "http://localhost:" + PORT + "/schemaimport/sayHi2"
                           + "?xsd=../sayhi/sayhi/sayhi-schema1.xsd";
        URL url = new URL(schemaURL);
        try (InputStream ins = url.openStream()) {
            String output = IOUtils.toString(ins);
            assertTrue(output.indexOf("sayHiArray") > -1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Can not access the import schema");
        }
    }

    @Test
    public void testImportWsdl() throws Exception {
        String wsdlURL = "http://localhost:" + PORT + "/schemaimport/sayHi"  + "?wsdl=sayhi/sayhi/a.wsdl";
        URL url = new URL(wsdlURL);
        try (InputStream ins = url.openStream()) {
            String output = IOUtils.toString(ins);
            assertTrue(output.indexOf("sayHiArray") > -1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Can not access the import wsdl");

        }
    }

    @Test
    public void testImportWsdl2() throws Exception {
        String wsdlURL = "http://localhost:" + PORT + "/schemaimport/sayHi2" + "?wsdl=../sayhi/sayhi/a.wsdl";
        URL url = new URL(wsdlURL);
        try (InputStream ins = url.openStream()) {
            String output = IOUtils.toString(ins);
            assertTrue(output.indexOf("sayHiArray") > -1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Can not access the import wsdl");

        }
    }

    @Test
    public void testAnotherSchemaImportl() throws Exception {
        String schemaURL = "http://localhost:" + PORT + "/schemaimport/service"  + "?xsd=schema1.xsd";
        URL url = new URL(schemaURL);
        try (InputStream ins = url.openStream()) {
            String output = IOUtils.toString(ins);
            assertTrue(output.indexOf("schemaimport/service?xsd=schema2.xsd") > -1);
            assertTrue(output.indexOf("schemaimport/service?xsd=schema5.xsd") > -1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Can not access the import wsdl");

        }
    }


    @Test
    public void testSchemaInclude() throws Exception {
        String schemaURL = "http://localhost:" + PORT + "/schemainclude/service?xsd=d1/d1/test.xsd";
        URL url = new URL(schemaURL);
        try (InputStream ins = url.openStream()) {
            String output = IOUtils.toString(ins);
            assertTrue(output.indexOf("msg:RequestType") > -1);
            assertTrue(output.indexOf("msg:SomeFeatureType") > -1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Can not access the include schema");

        }
    }

}
