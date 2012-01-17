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
package org.apache.cxf.tools.wsdlto.jaxws;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.wsdlto.AbstractCodeGenTest;
import org.apache.cxf.wsdl11.WSDLRuntimeException;

import org.junit.Test;

public class CodeGenOptionTest extends AbstractCodeGenTest {


    @Test
    public void testFlagForGenStandAlone() throws Exception {
        env.put(ToolConstants.CFG_GEN_TYPES, ToolConstants.CFG_GEN_TYPES);
        env.put(ToolConstants.CFG_GEN_SEI, ToolConstants.CFG_GEN_SEI);
        env.put(ToolConstants.CFG_GEN_IMPL, ToolConstants.CFG_GEN_IMPL);
        env.put(ToolConstants.CFG_GEN_SERVICE, ToolConstants.CFG_GEN_SERVICE);
        env.put(ToolConstants.CFG_GEN_SERVER, ToolConstants.CFG_GEN_SERVER);
        env.put(ToolConstants.CFG_GEN_FAULT, ToolConstants.CFG_GEN_FAULT);
        env.put(ToolConstants.CFG_GEN_ANT, ToolConstants.CFG_GEN_ANT);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        Class greeterServer = classLoader
            .loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter_SoapPort_Server");
        assertNotNull("Server should be generated", greeterServer);

    }

    @Test
    public void testFlagForGenAdditional() throws Exception {
        env.put(ToolConstants.CFG_IMPL, ToolConstants.CFG_IMPL);
        env.put(ToolConstants.CFG_SERVER, ToolConstants.CFG_SERVER);

        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        processor.setContext(env);
        processor.execute();

        Class greeterServer = classLoader
            .loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter_SoapPort_Server");
        assertNotNull("Server should be generated", greeterServer);
    }

    /**
     * Testing if the wsdlList option is used then all wsdls from a given file gets processed.
     */
    @Test
    public void testWSDLListOptionMultipleWSDL() throws Exception {

        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLLIST, ToolConstants.CFG_WSDLLIST);

        // Getting the full path of the wsdl
        String wsdl1 = getLocation("/wsdl2java_wsdl/hello_world.wsdl");
        String wsdl2 = getLocation("/wsdl2java_wsdl/cardealer.wsdl");

        doWSDLListOptionTest(null, Arrays.asList(wsdl1, wsdl2));

        Class greeterServer = classLoader
            .loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter_SoapPort_Server");
        assertNotNull("Server should be generated", greeterServer);

        Class carDealerServer = classLoader
            .loadClass("type_substitution.server.CarDealer_CarDealerPort_Server");
        assertNotNull("Server should be generated", carDealerServer);

    }

    /**
     * Testing if the wsdlList option is used and the file contains only one WSDL, then that WSDL is processed
     */
    @Test
    public void testWSDLListOptionOneWSDL() throws Exception {

        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLLIST, ToolConstants.CFG_WSDLLIST);

        // Getting the full path of the wsdl
        String wsdl1 = getLocation("/wsdl2java_wsdl/hello_world.wsdl");

        doWSDLListOptionTest(null, Arrays.asList(wsdl1));

        Class greeterServer = classLoader
            .loadClass("org.apache.cxf.w2j.hello_world_soap_http.Greeter_SoapPort_Server");
        assertNotNull("Server should be generated", greeterServer);
    }

    /**
     * Testing if the wsdlList option is used and the file contains an incorrect WSDL, then an exception is
     * thrown
     */
    @Test
    public void testWSDLListOptionIncorrectWSDLUrl() throws Exception {

        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLLIST, ToolConstants.CFG_WSDLLIST);

        // Getting the full path of the wsdl
        String wsdl1 = getLocation("/wsdl2java_wsdl/hello_world.wsdl");

        try {
            doWSDLListOptionTest(null, Arrays.asList(wsdl1, "test"));
        } catch (WSDLRuntimeException e) {
            return;
        }
        fail();
    }

    /**
     * Testing if the wsdlList option is used and a file that does not exist is specified, then an exception
     * occurs
     */
    @Test
    public void testWSDLListOptionIncorrectFile() throws Exception {

        env.put(ToolConstants.CFG_ALL, ToolConstants.CFG_ALL);
        env.put(ToolConstants.CFG_WSDLLIST, ToolConstants.CFG_WSDLLIST);

        // Getting the full path of the wsdl
        String wsdl1 = getLocation("/wsdl2java_wsdl/hello_world.wsdl");

        try {
            doWSDLListOptionTest("/Temp/temp.txt", Arrays.asList(wsdl1));
        } catch (ToolException e) {
            return;
        }
        fail();
    }
    
    /**
     * Performs the WSDLList option test for the specified list of parameters.
     * 
     * @param wsdlURL The url of the wsdlList. Can be null.
     * @param wsdls
     * @throws IOException
     * @throws ToolException
     */
    private void doWSDLListOptionTest(String wsdlURL, List<String> wsdls) 
        throws IOException, ToolException {
        
        File file = null;
        if (wsdlURL == null) {
            // Creating a file containing a list of wsdls URLs in a temp folder
            file = tmpDir.newFile("wsdl_list.txt");
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            for (String wsdl : wsdls) {
                writer.println(wsdl);
            }
            writer.close();
            
            wsdlURL = file.getPath();
        }
    
        env.put(ToolConstants.CFG_WSDLURL, wsdlURL);
        processor.setContext(env);
        
        try {
            processor.execute();
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    public void testHelloWorldExternalBindingFile() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world_jaxws_base.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/hello_world_jaxws_binding.wsdl"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        File org = new File(output, "org");
        assertTrue(org.exists());
        File apache = new File(org, "apache");
        assertTrue(apache.exists());

        Class clz = classLoader.loadClass("org.apache.cxf.w2j.hello_world_async_soap_http.GreeterAsync");
        assertEquals(3, clz.getMethods().length);

    }

    @Test
    public void testGenFault() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        env.remove(ToolConstants.CFG_COMPILE);
        env.remove(ToolConstants.CFG_IMPL);
        env.put(ToolConstants.CFG_GEN_FAULT, ToolConstants.CFG_GEN_FAULT);
        processor.setContext(env);
        processor.execute();

        File file = new File(output, "org/apache/cxf/w2j/hello_world_soap_http");
        assertEquals(2, file.list().length);

    }

    @Test
    public void testGetCatalog() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/test_catalog_replaceme.wsdl"));
        env.put(ToolConstants.CFG_CATALOG, getLocation("/wsdl2java_wsdl/test_catalog.xml"));
        env.put(ToolConstants.CFG_COMPILE, null);
        env.put(ToolConstants.CFG_CLASSDIR, null);

        processor.setContext(env);
        processor.execute();
    }

    @Test
    public void testGetCatalogPublic() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/cxf1053/myservice.wsdl"));
        env.put(ToolConstants.CFG_CATALOG, getLocation("/wsdl2java_wsdl/cxf1053/catalog.xml"));
        env.put(ToolConstants.CFG_COMPILE, null);
        env.put(ToolConstants.CFG_CLASSDIR, null);

        processor.setContext(env);
        processor.execute();
    }

    /**
     * Tests that, when 'mark-generated' option is set, @Generated annotations are inserted in all generated
     * java classes.
     */
    @Test
    public void testMarkGeneratedOption() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        env.put(ToolConstants.CFG_MARK_GENERATED, "true");
        env.put(ToolConstants.CFG_COMPILE, null);
        env.put(ToolConstants.CFG_CLASSDIR, null);
        processor.setContext(env);
        processor.execute();

        File dir = new File(output, "org");
        assertTrue("org directory is not found", dir.exists());
        dir = new File(dir, "apache");
        assertTrue("apache directory is not found", dir.exists());
        dir = new File(dir, "cxf");
        assertTrue("cxf directory is not found", dir.exists());
        dir = new File(dir, "w2j");
        assertTrue("w2j directory is not found", dir.exists());
        dir = new File(dir, "hello_world_soap_http");
        assertTrue("hello_world_soap_http directory is not found", dir.exists());
        File types = new File(dir, "types");
        assertTrue("types directory is not found", dir.exists());

        String str = IOUtils.readStringFromStream(new FileInputStream(new File(dir, "Greeter.java")));
        assertEquals(7, countGeneratedAnnotations(str));
        str = IOUtils.readStringFromStream(new FileInputStream(new File(types, "SayHi.java")));
        assertEquals(1, countGeneratedAnnotations(str));
        str = IOUtils.readStringFromStream(new FileInputStream(new File(types, "SayHiResponse.java")));
        assertEquals(4, countGeneratedAnnotations(str));
    }

    private int countGeneratedAnnotations(String str) {
        int count = 0;
        int idx = str.indexOf("@Generated");
        while (idx != -1) {
            count++;
            idx = str.indexOf("@Generated", idx + 1);
        }
        return count;
    }
    
    
    @Test
    public void testResourceURLForWsdlLocation() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));
        env.put(ToolConstants.CFG_WSDLLOCATION, "/wsdl2java_wsdl/hello_world.wsdl");
        env.put(ToolConstants.CFG_COMPILE, null);
        env.put(ToolConstants.CFG_CLASSDIR, null);
        processor.setContext(env);
        processor.execute();

        File dir = new File(output, "org");
        assertTrue("org directory is not found", dir.exists());
        dir = new File(dir, "apache");
        assertTrue("apache directory is not found", dir.exists());
        dir = new File(dir, "cxf");
        assertTrue("cxf directory is not found", dir.exists());
        dir = new File(dir, "w2j");
        assertTrue("w2j directory is not found", dir.exists());
        dir = new File(dir, "hello_world_soap_http");
        assertTrue("hello_world_soap_http directory is not found", dir.exists());

        String str = IOUtils.readStringFromStream(new FileInputStream(new File(dir,
                                                                               "SOAPService.java")));
        assertTrue(str, str.contains("getResource"));
    }

}
