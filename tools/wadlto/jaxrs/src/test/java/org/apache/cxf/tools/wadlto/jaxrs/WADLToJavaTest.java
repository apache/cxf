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

package org.apache.cxf.tools.wadlto.jaxrs;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wadlto.WADLToJava;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WADLToJavaTest extends ProcessorTestBase {

    @Test
    public void testCodeGenInterfaces() throws Exception {
        String[] args = new String[] {
            "-d",
            output.getCanonicalPath(),
            "-p",
            "custom.service",
            "-tMap",
            "{http://www.w3.org/2001/XMLSchema}date=java.util.List..String",
            "-tMap",
            "{http://www.w3.org/2001/XMLSchema}base64Binary=byte[]",
            "-async getName,delete",
            "-inheritResourceParams first",
            "-compile",
            getLocation("/wadl/bookstore.xml"),
        };
        WADLToJava tool = new WADLToJava(args);
        tool.run(new ToolContext());
        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "custom.service");
        verifyFiles("class", true, false, "superbooks", "custom.service");
    }

    @Test
    public void testGenerateJAXBToString() throws Exception {
        String[] args = new String[] {
            "-d",
            output.getCanonicalPath(),
            "-p",
            "custom.service",
            "-async getName,delete",
            "-compile",
            "-xjc-episode " + output.getAbsolutePath() + "/test.episode",
            "-xjc-XtoString",
            getLocation("/wadl/bookstore.xml"),
        };

        WADLToJava tool = new WADLToJava(args);
        tool.run(new ToolContext());
        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "custom.service");
        verifyFiles("class", true, false, "superbooks", "custom.service");
        assertTrue(new File(output.getAbsolutePath() + "/test.episode").exists());

        List<Class<?>> schemaClassFiles = getSchemaClassFiles();
        assertEquals(4, schemaClassFiles.size());
        for (Class<?> c : schemaClassFiles) {
            c.getMethod("toString");
        }
    }

    @Test
    public void testGenerateRxJava8() throws Exception {
        String[] args = new String[] {
            "-d",
            output.getCanonicalPath(),
            "-p",
            "custom.service",
            "-rx",
            "java8",
            "-compile",
            "-xjc-episode " + output.getAbsolutePath() + "/test.episode",
            "-xjc-XtoString",
            getLocation("/wadl/bookstore.xml"),
        };

        WADLToJava tool = new WADLToJava(args);
        tool.run(new ToolContext());
        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "custom.service");
        verifyFiles("class", true, false, "superbooks", "custom.service");
        assertTrue(new File(output.getAbsolutePath() + "/test.episode").exists());

        List<Class<?>> schemaClassFiles = getSchemaClassFiles();
        assertEquals(4, schemaClassFiles.size());
        for (Class<?> c : schemaClassFiles) {
            c.getMethod("toString");
        }
    }

    private List<Class<?>> getSchemaClassFiles() throws Exception {
        try (URLClassLoader cl = new URLClassLoader(new URL[] {output.toURI().toURL()},
                                            Thread.currentThread().getContextClassLoader())) {
            return Arrays.asList(
                    cl.loadClass("superbooks.EnumType"),
                    cl.loadClass("superbooks.Book"),
                    cl.loadClass("superbooks.TheBook2"),
                    cl.loadClass("superbooks.Chapter"));
        }
    }

    @Test
    public void testGenerateJAXBToStringAndEqualsAndHashCode() throws Exception {
        String[] args = new String[] {
            "-d",
            output.getCanonicalPath(),
            "-p",
            "custom.service",
            "-async getName,delete",
            "-compile",
            "-xjc-XtoString",
            "-xjc-Xequals",
            "-xjc-XhashCode",
            getLocation("/wadl/bookstore.xml"),
        };

        WADLToJava tool = new WADLToJava(args);
        tool.run(new ToolContext());
        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "custom.service");
        verifyFiles("class", true, false, "superbooks", "custom.service");

        List<Class<?>> schemaClassFiles = getSchemaClassFiles();
        assertEquals(4, schemaClassFiles.size());
        for (Class<?> c : schemaClassFiles) {
            c.getMethod("toString");
            c.getMethod("hashCode");
            c.getMethod("equals", Object.class);
        }
    }


    private void verifyFiles(String ext, boolean subresourceExpected, boolean interfacesAndImpl,
                             String schemaPackage, String resourcePackage) {
        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, "." + ext);
        int size = interfacesAndImpl ? 11 : 10;
        if (!subresourceExpected) {
            size--;
        }
        assertEquals(size, files.size());
        doVerifyTypes(files, schemaPackage, ext);
        if (subresourceExpected) {
            assertTrue(checkContains(files, resourcePackage + ".FormInterface." + ext));
        }
        assertTrue(checkContains(files, resourcePackage + ".BookStore." + ext));
        if (interfacesAndImpl) {
            if (subresourceExpected) {
                assertTrue(checkContains(files, resourcePackage + ".FormInterfaceImpl." + ext));
            }
            assertTrue(checkContains(files, resourcePackage + ".BookStoreImpl." + ext));
        }
    }

    private static void doVerifyTypes(List<File> files, String schemaPackage, String ext) {
        assertTrue(checkContains(files, schemaPackage + ".EnumType." + ext));
        assertTrue(checkContains(files, schemaPackage + ".Book." + ext));
        assertTrue(checkContains(files, schemaPackage + ".TheBook2." + ext));
        assertTrue(checkContains(files, schemaPackage + ".Chapter." + ext));
        assertTrue(checkContains(files, schemaPackage + ".ObjectFactory." + ext));
        assertTrue(checkContains(files, schemaPackage + ".package-info." + ext));
    }

    private static boolean checkContains(List<File> clsFiles, String name) {
        for (File f : clsFiles) {
            if (checkFileContains(f, name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkFileContains(File f, String name) {
        return f.getAbsolutePath().replace(File.separatorChar, '.').endsWith(name);
    }

    protected String getLocation(String wsdlFile) throws URISyntaxException {
        return getClass().getResource(wsdlFile).toString();
    }
}