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
import java.util.List;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wadlto.WadlToolConstants;

import org.junit.Test;

public class JAXRSContainerTest extends ProcessorTestBase {

    @Test    
    public void testCodeGenInterfaces() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");
            
            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 10, true);
            verifyFiles("class", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 10, true);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenInterfacesMultipleInXmlReps() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");
            context.put(WadlToolConstants.CFG_MULTIPLE_XML_REPS, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 10, true);
            verifyFiles("class", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 10, true);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
        
    @Test    
    public void testCodeGenInterfacesWithBinding() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
            context.put(WadlToolConstants.CFG_BINDING, getLocation("/wadl/jaxbBinding.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 10, true);
            verifyFiles("class", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 10, true);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenWithImportedSchema() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstoreImport.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
            verifyFiles("class", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenWithMultipleInlinedSchemas() {
        doTestInlinedSchemasWithImport("/wadl/bookstoreMultipleSchemas.xml");
    }
    
    @Test    
    public void testCodeGenWithInlinedSchemaAndImport() {
        doTestInlinedSchemasWithImport("/wadl/bookstoreInlinedSchemaWithImport.xml");
    }
    
    private void doTestInlinedSchemasWithImport(String loc) {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation(loc));
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            List<File> files = FileUtils.getFilesRecurse(output, ".+\\." + "class" + "$");
            assertEquals(7, files.size());
            assertTrue(checkContains(files, "org.apache.cxf.jaxrs.model.wadl" + ".BookStore.class"));
            assertTrue(checkContains(files, "superbooks" + ".Book.class"));
            assertTrue(checkContains(files, "superbooks" + ".ObjectFactory.class"));
            assertTrue(checkContains(files, "superbooks" + ".package-info.class"));
            assertTrue(checkContains(files, "superchapters" + ".Chapter.class"));
            assertTrue(checkContains(files, "superchapters" + ".ObjectFactory.class"));
            assertTrue(checkContains(files, "superchapters" + ".package-info.class"));
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @Test
    public void testResourceWithEPR() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/resourceWithEPR.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            List<File> files = FileUtils.getFilesRecurse(output, ".+\\." + "class" + "$");
            assertEquals(4, files.size());
            assertTrue(checkContains(files, "application" + ".BookstoreResource.class"));
            assertTrue(checkContains(files, "superbooks" + ".Book.class"));
            assertTrue(checkContains(files, "superbooks" + ".ObjectFactory.class"));
            assertTrue(checkContains(files, "superbooks" + ".package-info.class"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenWithImportedSchemaAndResourceSet() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstoreResourceRef.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
            verifyFiles("class", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenWithImportedSchemaAndBinding() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstoreImport.xml"));
            context.put(WadlToolConstants.CFG_BINDING, getLocation("/wadl/jaxbBindingWithSchemaLoc.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
            verifyFiles("class", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenWithImportedSchemaAndCatalog() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstoreImportCatalog.xml"));
            context.put(WadlToolConstants.CFG_CATALOG, getLocation("/wadl/jax-rs-catalog.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
            verifyFiles("class", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenNoIds() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/singleResource.xml"));
            context.put(WadlToolConstants.CFG_RESOURCENAME, "CustomResource");
            context.put(WadlToolConstants.CFG_GENERATE_ENUMS, "true");
            context.put(WadlToolConstants.CFG_COMPILE, "true");
            
            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            List<File> javaFiles = FileUtils.getFilesRecurse(output, ".+\\." + "java" + "$");
            assertEquals(2, javaFiles.size());
            assertTrue(checkContains(javaFiles, "application.CustomResource.java"));
            assertTrue(checkContains(javaFiles, "application.Theid.java"));
            
            List<File> classFiles = FileUtils.getFilesRecurse(output, ".+\\." + "class" + "$");
            assertEquals(2, classFiles.size());
            assertTrue(checkContains(classFiles, "application.CustomResource.class"));
            assertTrue(checkContains(classFiles, "application.Theid.class"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenNoIds2() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/multipleResources.xml"));
            context.put(WadlToolConstants.CFG_COMPILE, "true");
            
            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            List<File> javaFiles = FileUtils.getFilesRecurse(output, ".+\\." + "java" + "$");
            assertEquals(2, javaFiles.size());
            assertTrue(checkContains(javaFiles, "application.BookstoreResource.java"));
            assertTrue(checkContains(javaFiles, "application.BooksResource.java"));
            List<File> classFiles = FileUtils.getFilesRecurse(output, ".+\\." + "class" + "$");
            assertEquals(2, classFiles.size());
            assertTrue(checkContains(classFiles, "application.BookstoreResource.class"));
            assertTrue(checkContains(classFiles, "application.BooksResource.class"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenWithResourceSet() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/singleResourceWithRefs.xml"));
            context.put(WadlToolConstants.CFG_RESOURCENAME, "CustomResource");
            context.put(WadlToolConstants.CFG_COMPILE, "true");
            
            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            List<File> javaFiles = FileUtils.getFilesRecurse(output, ".+\\." + "java" + "$");
            assertEquals(1, javaFiles.size());
            assertTrue(checkContains(javaFiles, "application.CustomResource.java"));
            
            List<File> classFiles = FileUtils.getFilesRecurse(output, ".+\\." + "class" + "$");
            assertEquals(1, classFiles.size());
            assertTrue(checkContains(classFiles, "application.CustomResource.class"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenInterfacesCustomPackage() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);
            
            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
            context.put(WadlToolConstants.CFG_PACKAGENAME, "custom.books");
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", true, false, "superbooks", "custom.books", 10, true);
            verifyFiles("class", true, false, "superbooks", "custom.books", 10, true);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenInterfacesCustomPackageForResourcesAndSchemas() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);
            
            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
            context.put(WadlToolConstants.CFG_PACKAGENAME, "custom.books.service");
            context.put(WadlToolConstants.CFG_SCHEMA_PACKAGENAME, "http://superbooks=custom.books.schema");
            context.put(WadlToolConstants.CFG_COMPILE, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", true, false, "custom.books.schema", "custom.books.service", 10, true);
            verifyFiles("class", true, false, "custom.books.schema", "custom.books.service", 10, true);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test    
    public void testCodeGenImpl() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
            context.put(WadlToolConstants.CFG_IMPL, "true");
            context.put(WadlToolConstants.CFG_COMPILE, "true");
            
            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 10, true);
            verifyFiles("class", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 10, true);
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }
    }
    
    @Test    
    public void testCodeGenInterfaceAndImpl() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);
         
            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
            context.put(WadlToolConstants.CFG_INTERFACE, "true");
            context.put(WadlToolConstants.CFG_IMPL, "true");
            context.put(WadlToolConstants.CFG_COMPILE, "true");
            
            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java", true, true, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 12, true);
            verifyFiles("class", true, true, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 12, true);
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }
    }
    
    @Test    
    public void testCodeGenTypesOnly() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);

            ToolContext context = new ToolContext();
            context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
            context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
            context.put(WadlToolConstants.CFG_TYPES, "true");

            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyTypes("superbooks", "java", true);
            
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }
    }
    
    private void verifyFiles(String ext, boolean subresourceExpected, boolean interfacesAndImpl, 
                             String schemaPackage, String resourcePackage, int expectedCount) {
        verifyFiles(ext, subresourceExpected, interfacesAndImpl, schemaPackage, resourcePackage,
                    expectedCount, false);
    }
    
    private void verifyFiles(String ext, boolean subresourceExpected, boolean interfacesAndImpl, 
                             String schemaPackage, String resourcePackage, int expectedCount,
                             boolean enumTypeExpected) {    
        List<File> files = FileUtils.getFilesRecurse(output, ".+\\." + ext + "$");
        int offset = enumTypeExpected ? 1 : 2;
        int size = interfacesAndImpl ? expectedCount : expectedCount - offset;
        if (!subresourceExpected) {
            size--;
        }
        assertEquals(size, files.size());
        doVerifyTypes(files, schemaPackage, ext);
        if (subresourceExpected) {
            assertTrue(checkContains(files, resourcePackage + ".FormInterface." + ext));
            assertTrue(checkContains(files, resourcePackage + ".FormInterface2." + ext));
        }
        assertTrue(checkContains(files, resourcePackage + ".BookStore." + ext));
        if (interfacesAndImpl) {
            if (subresourceExpected) {
                assertTrue(checkContains(files, resourcePackage + ".FormInterfaceImpl." + ext));
                assertTrue(checkContains(files, resourcePackage + ".FormInterface2Impl." + ext));
            }
            assertTrue(checkContains(files, resourcePackage + ".BookStoreImpl." + ext));
        }
    }
    
    private void verifyTypes(String schemaPackage, String ext, boolean enumTypeExpected) {
        List<File> files = FileUtils.getFilesRecurse(output, ".+\\." + ext + "$");
        assertEquals(enumTypeExpected ? 6 : 5, files.size());
        doVerifyTypes(files, schemaPackage, ext);
    }
    
    private void doVerifyTypes(List<File> files, String schemaPackage, String ext) {
        assertTrue(checkContains(files, schemaPackage + ".Book." + ext));
        assertTrue(checkContains(files, schemaPackage + ".TheBook2." + ext));
        assertTrue(checkContains(files, schemaPackage + ".Chapter." + ext));
        assertTrue(checkContains(files, schemaPackage + ".ObjectFactory." + ext));
        assertTrue(checkContains(files, schemaPackage + ".package-info." + ext));
    }
    
    private boolean checkContains(List<File> clsFiles, String name) {
        for (File f : clsFiles) {
            if (f.getAbsolutePath().replace(File.separatorChar, '.').endsWith(name)) {
                return true;
            }
        }
        return false;
    }
    
    protected String getLocation(String wsdlFile) throws URISyntaxException {
        return getClass().getResource(wsdlFile).toString();
    }
}
