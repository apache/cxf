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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wadlto.WadlToolConstants;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXRSContainerTest extends ProcessorTestBase {

    @Test
    public void testNoTargetNamespace() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/resourceSchemaNoTargetNamespace.xml"));
        context.put(WadlToolConstants.CFG_SCHEMA_PACKAGENAME, "=custom");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());
        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(3, files.size());
        assertTrue(checkContains(files, "application" + ".Resource.class"));
        assertTrue(checkContains(files, "custom" + ".TestCompositeObject.class"));
        assertTrue(checkContains(files, "custom" + ".ObjectFactory.class"));
    }

    @Test
    public void testCodeGenInterfaces() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
        context.put(WadlToolConstants.CFG_MEDIA_TYPE_MAP,
                    "application/xml=javax.xml.transform.Source");
        context.put(WadlToolConstants.CFG_MEDIA_TYPE_MAP,
                    "multipart/form-data=org.apache.cxf.jaxrs.ext.multipart.MultipartBody");
        context.put(WadlToolConstants.CFG_NO_VOID_FOR_EMPTY_RESPONSES, "true");
        context.put(WadlToolConstants.CFG_GENERATE_RESPONSE_IF_HEADERS_SET, "true");
        context.put(WadlToolConstants.CFG_GENERATE_RESPONSE_FOR_METHODS, "getName");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 11, true);
        verifyFiles("class", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 11, true);
    }

    @Test
    public void testInheritParameters() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/test.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");
        context.put(WadlToolConstants.CFG_SCHEMA_TYPE_MAP,
                    "{http://www.w3.org/2001/XMLSchema}anyType="
                    + "java.io.InputStream");
        context.put(WadlToolConstants.CFG_INHERIT_PARAMS, "last");
        context.put(WadlToolConstants.CFG_CREATE_JAVA_DOCS, "true");
        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(1, files.size());
    }

    @Test
    public void testOnewayMethod() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        final String onewayMethod = "deleteRepository";
        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/test.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");
        context.put(WadlToolConstants.CFG_ONEWAY, onewayMethod);
        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        ClassCollector cc = context.get(ClassCollector.class);
        assertEquals(1, cc.getServiceClassNames().size());
        try (URLClassLoader loader = new URLClassLoader(new URL[]{output.toURI().toURL()})) {
            final Class<?> generatedClass = loader.loadClass(cc.getServiceClassNames().values().iterator().next());
            Method m = generatedClass.getMethod(onewayMethod, String.class);
            assertNotNull(m.getAnnotation(Oneway.class));
        }
    }

    @Test
    public void testThrows() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/test.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, Boolean.TRUE);
        context.put(WadlToolConstants.CFG_INTERFACE, Boolean.TRUE);
        context.put(WadlToolConstants.CFG_IMPL, Boolean.TRUE);
        context.put(WadlToolConstants.CFG_CREATE_JAVA_DOCS, Boolean.TRUE);
        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> javaFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".java");
        assertEquals(2, javaFiles.size());
        for (File f : javaFiles) {
            if (!f.getName().endsWith("Impl.java")) {
                assertTrue(
                        Files.readAllLines(f.toPath()).contains("     * @throws IOException if something going wrong"));
            }
        }

        ClassCollector cc = context.get(ClassCollector.class);
        assertEquals(2, cc.getServiceClassNames().size());

        final Map<String, Class<?>[]> methods = new HashMap<>();
        methods.put("listRepositories", new Class<?>[] {});
        methods.put("createRepository", new Class<?>[] {java.io.IOException.class});
        methods.put("deleteRepository",
                new Class<?>[] {jakarta.ws.rs.NotFoundException.class, java.io.IOException.class});
        methods.put("postThename", new Class<?>[] {java.io.IOException.class, java.lang.NoSuchMethodException.class});
        try (URLClassLoader loader = new URLClassLoader(new URL[]{output.toURI().toURL()})) {
            for (String className : cc.getServiceClassNames().values()) {
                final Class<?> generatedClass = loader.loadClass(className);
                for (Map.Entry<String, Class<?>[]> entry : methods.entrySet()) {
                    Method m;
                    try {
                        m = generatedClass.getMethod(entry.getKey(), String.class);
                    } catch (NoSuchMethodException e) {
                        m = generatedClass.getMethod(entry.getKey(), String.class, String.class);
                    }
                    assertArrayEquals(entry.getValue(), m.getExceptionTypes());
                }
            }
        }
    }

    @Test
    public void testCodeGenInterfacesMultipleInXmlReps() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");
        context.put(WadlToolConstants.CFG_MULTIPLE_XML_REPS, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 11, true);
        verifyFiles("class", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 11, true);
    }

    @Test
    public void testCodeGenInterfacesWithBinding() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
        context.put(WadlToolConstants.CFG_BINDING, getLocation("/wadl/jaxbBinding.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 11, true);
        verifyFiles("class", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 11, true);
    }

    @Test
    public void testCodeGenInterfacesWithJaxbClassNameSuffix() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
        context.put(WadlToolConstants.CFG_JAXB_CLASS_NAME_SUFFIX, "DTO");
        context.put(WadlToolConstants.CFG_BINDING, getLocation("/wadl/jaxbSchemaBindings.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());
        List<File> schemafiles = FileUtils.getFilesRecurseUsingSuffix(output, ".java");
        assertEquals(10, schemafiles.size());
        doVerifyTypesWithSuffix(schemafiles, "superbooks", "java");

        List<File> classfiles = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(10, classfiles.size());
        doVerifyTypesWithSuffix(classfiles, "superbooks", "class");
    }

    @Test
    public void testCodeGenWithImportedSchema() throws Exception {
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
    }

    @Test
    public void testCodeGenWithImportedSchemaWithParentRefs() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/sub/bookstoreImport.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        verifyFiles("java", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
        verifyFiles("class", false, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 9);
    }

    @Test
    public void testCodeGenWithMultipleInlinedSchemas() throws Exception {
        doTestInlinedSchemasWithImport("/wadl/bookstoreMultipleSchemas.xml");
    }

    @Test
    public void testCodeGenWithInlinedSchemaAndImport() throws Exception {
        doTestInlinedSchemasWithImport("/wadl/bookstoreInlinedSchemaWithImport.xml");
    }

    private void doTestInlinedSchemasWithImport(String loc) throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation(loc));
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(7, files.size());
        assertTrue(checkContains(files, "org.apache.cxf.jaxrs.model.wadl" + ".BookStore.class"));
        assertTrue(checkContains(files, "superbooks" + ".Book.class"));
        assertTrue(checkContains(files, "superbooks" + ".ObjectFactory.class"));
        assertTrue(checkContains(files, "superbooks" + ".package-info.class"));
        assertTrue(checkContains(files, "superchapters" + ".Chapter.class"));
        assertTrue(checkContains(files, "superchapters" + ".ObjectFactory.class"));
        assertTrue(checkContains(files, "superchapters" + ".package-info.class"));
    }

    @Test
    public void testResourceWithEPR() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/resourceWithEPR.xml"));
        context.put(WadlToolConstants.CFG_SCHEMA_TYPE_MAP,
                    "{http://www.w3.org/2001/XMLSchema}date=javax.xml.datatype.XMLGregorianCalendar");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(4, files.size());
        assertTrue(checkContains(files, "application" + ".BookstoreResource.class"));
        assertTrue(checkContains(files, "superbooks" + ".Book.class"));
        assertTrue(checkContains(files, "superbooks" + ".ObjectFactory.class"));
        assertTrue(checkContains(files, "superbooks" + ".package-info.class"));
    }

    @Test
    public void testResourceWithEPRNoSchemaGen() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/resourceWithEPR.xml"));
        context.put(WadlToolConstants.CFG_SCHEMA_TYPE_MAP,
            "{http://www.w3.org/2005/08/addressing}EndpointReferenceType="
            + "jakarta.xml.ws.wsaddressing.W3CEndpointReference");
        context.put(WadlToolConstants.CFG_NO_ADDRESS_BINDING, "true");
        context.put(WadlToolConstants.CFG_NO_TYPES, "true");

        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(1, files.size());
        assertTrue(checkContains(files, "application" + ".BookstoreResource.class"));
    }

    @Test
    public void testQueryMultipartParam() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/testQueryMultipartParam.wadl"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(2, files.size());
        assertTrue(checkContains(files, "application.Test1.class"));
        assertTrue(checkContains(files, "application.Test2.class"));

        @SuppressWarnings("resource")
        ClassLoader loader = new URLClassLoader(new URL[] {output.toURI().toURL() });

        Class<?> test1 = loader.loadClass("application.Test1");
        Method[] test1Methods = test1.getDeclaredMethods();
        assertEquals(1, test1Methods.length);

        assertEquals(2, test1Methods[0].getAnnotations().length);
        assertNotNull(test1Methods[0].getAnnotation(PUT.class));
        Consumes consumes1 = test1Methods[0].getAnnotation(Consumes.class);
        assertNotNull(consumes1);
        assertEquals(1, consumes1.value().length);
        assertEquals("multipart/mixed", consumes1.value()[0]);

        assertEquals("put", test1Methods[0].getName());
        Class<?>[] paramTypes = test1Methods[0].getParameterTypes();
        assertEquals(3, paramTypes.length);
        Annotation[][] paramAnns = test1Methods[0].getParameterAnnotations();
        assertEquals(Boolean.class, paramTypes[0]);
        assertEquals(1, paramAnns[0].length);
        QueryParam test1QueryParam1 = (QueryParam)paramAnns[0][0];
        assertEquals("standalone", test1QueryParam1.value());
        assertEquals(String.class, paramTypes[1]);
        assertEquals(1, paramAnns[1].length);
        Multipart test1MultipartParam1 = (Multipart)paramAnns[1][0];
        assertEquals("action", test1MultipartParam1.value());
        assertTrue(test1MultipartParam1.required());
        assertEquals(String.class, paramTypes[2]);
        assertEquals(1, paramAnns[2].length);
        Multipart test1MultipartParam2 = (Multipart)paramAnns[2][0];
        assertEquals("sources", test1MultipartParam2.value());
        assertFalse(test1MultipartParam2.required());

        Class<?> test2 = loader.loadClass("application.Test2");
        Method[] test2Methods = test2.getDeclaredMethods();
        assertEquals(1, test2Methods.length);

        assertEquals(2, test2Methods[0].getAnnotations().length);
        assertNotNull(test2Methods[0].getAnnotation(PUT.class));
        Consumes consumes2 = test2Methods[0].getAnnotation(Consumes.class);
        assertNotNull(consumes2);
        assertEquals(1, consumes2.value().length);
        assertEquals("application/json", consumes2.value()[0]);

        assertEquals("put", test2Methods[0].getName());
        Class<?>[] paramTypes2 = test2Methods[0].getParameterTypes();
        assertEquals(2, paramTypes2.length);
        Annotation[][] paramAnns2 = test2Methods[0].getParameterAnnotations();
        assertEquals(boolean.class, paramTypes2[0]);
        assertEquals(1, paramAnns2[0].length);
        QueryParam test2QueryParam1 = (QueryParam)paramAnns2[0][0];
        assertEquals("snapshot", test2QueryParam1.value());
        assertEquals(String.class, paramTypes2[1]);
        assertEquals(0, paramAnns2[1].length);
    }

    @Test
    public void testComplexPath() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/testComplexPath.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(1, files.size());
        assertTrue(checkContains(files, "application.Resource.class"));
        @SuppressWarnings("resource")
        ClassLoader loader = new URLClassLoader(new URL[] {output.toURI().toURL() });

        Class<?> test1 = loader.loadClass("application.Resource");
        Method[] test1Methods = test1.getDeclaredMethods();
        assertEquals(2, test1Methods.length);
        assertEquals(2, test1Methods[0].getAnnotations().length);
        if ("getGetaddmethod2".equals(test1Methods[0].getName())) {
            Method tmp = test1Methods[0];
            test1Methods[0] = test1Methods[1];
            test1Methods[1] = tmp;
        }
        checkComplexPathMethod(test1Methods[0], "");
        checkComplexPathMethod(test1Methods[1], "2");
    }

    private void checkComplexPathMethod(Method m, String suffix) {
        assertNotNull(m.getAnnotation(GET.class));
        Path path = m.getAnnotation(Path.class);
        assertNotNull(path);
        assertEquals("/get-add-method", path.value());
        assertEquals("getGetaddmethod" + suffix, m.getName());
        Class<?>[] paramTypes = m.getParameterTypes();
        assertEquals(1, paramTypes.length);
        Annotation[][] paramAnns = m.getParameterAnnotations();
        assertEquals(String.class, paramTypes[0]);
        assertEquals(1, paramAnns[0].length);
        PathParam methodPathParam1 = (PathParam)paramAnns[0][0];
        assertEquals("id", methodPathParam1.value());
    }

    @Test
    public void testBeanValidation() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/resourceSameTargetNsSchemas.xml"));
        context.put(WadlToolConstants.CFG_BEAN_VALIDATION, "true");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(4, files.size());
        assertTrue(checkContains(files, "application.Resource.class"));
        @SuppressWarnings("resource")
        ClassLoader loader = new URLClassLoader(new URL[] {output.toURI().toURL() });

        Class<?> test1 = loader.loadClass("application.Resource");
        Method[] test1Methods = test1.getDeclaredMethods();
        assertEquals(1, test1Methods.length);
        Method m = test1Methods[0];
        assertEquals(5, m.getAnnotations().length);
        assertNotNull(m.getAnnotation(Valid.class));
        assertNotNull(m.getAnnotation(Path.class));
        assertNotNull(m.getAnnotation(Consumes.class));
        assertNotNull(m.getAnnotation(Produces.class));
        assertNotNull(m.getAnnotation(PUT.class));

        Class<?>[] paramTypes = m.getParameterTypes();
        assertEquals(2, paramTypes.length);
        Annotation[][] paramAnns = m.getParameterAnnotations();
        assertEquals(String.class, paramTypes[0]);
        assertEquals(1, paramAnns[0].length);
        PathParam methodPathParam1 = (PathParam)paramAnns[0][0];
        assertEquals("id", methodPathParam1.value());

        assertEquals(1, paramAnns[1].length);
        assertTrue(paramAnns[1][0] instanceof Valid);
    }

    @Test
    public void testCodeGenWithImportedSchemaAndResourceSet() throws Exception {
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
    }

    @Test
    public void testCodeGenWithImportedSchemaAndBinding() throws Exception {
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
    }

    @Test
    public void testCodeGenWithImportedSchemaAndCatalog() throws Exception {
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
    }

    @Test
    public void testCodeGenNoIds() throws Exception {
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

        List<File> javaFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".java");
        assertEquals(2, javaFiles.size());
        assertTrue(checkContains(javaFiles, "application.CustomResource.java"));
        assertTrue(checkContains(javaFiles, "application.Theid.java"));

        List<File> classFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(2, classFiles.size());
        assertTrue(checkContains(classFiles, "application.CustomResource.class"));
        assertTrue(checkContains(classFiles, "application.Theid.class"));
    }

    @Test
    public void testCodeGenNoIds2() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/multipleResources.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> javaFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".java");
        assertEquals(2, javaFiles.size());
        assertTrue(checkContains(javaFiles, "application.BookstoreResource.java"));
        assertTrue(checkContains(javaFiles, "application.BooksResource.java"));
        List<File> classFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(2, classFiles.size());
        assertTrue(checkContains(classFiles, "application.BookstoreResource.class"));
        assertTrue(checkContains(classFiles, "application.BooksResource.class"));
    }

    @Test
    public void testCodeGenNoIds3() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/resourcesNoId.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");
        context.put(WadlToolConstants.CFG_INHERIT_PARAMS, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> javaFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".java");
        assertEquals(1, javaFiles.size());
        assertTrue(checkContains(javaFiles, "application.TestRsResource.java"));
        List<File> classFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(1, classFiles.size());
        assertTrue(checkContains(classFiles, "application.TestRsResource.class"));
    }

    @Test
    public void testCodeTwoSchemasSameTargetNs() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/resourceSameTargetNsSchemas.xml"));
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        List<File> javaFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".java");
        assertEquals(4, javaFiles.size());
        assertTrue(checkContains(javaFiles, "application.Resource.java"));
        assertTrue(checkContains(javaFiles, "com.example.test.ObjectFactory.java"));
        assertTrue(checkContains(javaFiles, "com.example.test.package-info.java"));
        assertTrue(checkContains(javaFiles, "com.example.test.TestCompositeObject.java"));
        List<File> classFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(4, classFiles.size());
        assertTrue(checkContains(classFiles, "application.Resource.class"));
        assertTrue(checkContains(classFiles, "com.example.test.ObjectFactory.class"));
        assertTrue(checkContains(classFiles, "com.example.test.package-info.class"));
        assertTrue(checkContains(classFiles, "com.example.test.TestCompositeObject.class"));


        assertNotNull(output.list());
    }

    @Test
    public void testCodeGenWithResourceSet() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/singleResourceWithRefs.xml"));
        context.put(WadlToolConstants.CFG_RESOURCENAME, "CustomResource");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> javaFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".java");
        assertEquals(1, javaFiles.size());
        assertTrue(checkContains(javaFiles, "application.CustomResource.java"));

        List<File> classFiles = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(1, classFiles.size());
        assertTrue(checkContains(classFiles, "application.CustomResource.class"));
    }

    @Test
    public void testCodeGenInterfacesCustomPackage() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
        context.put(WadlToolConstants.CFG_PACKAGENAME, "custom.books");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "custom.books", 11, true);
        verifyFiles("class", true, false, "superbooks", "custom.books", 11, true);
    }

    @Test
    public void testCodeGenInterfacesCustomPackageForResourcesAndSchemas() throws Exception {
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

        verifyFiles("java", true, false, "custom.books.schema", "custom.books.service", 11, true);
        verifyFiles("class", true, false, "custom.books.schema", "custom.books.service", 11, true);
    }

    @Test
    public void testCodeGenImpl() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
        context.put(WadlToolConstants.CFG_IMPL, "true");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        verifyFiles("java", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 11, true);
        verifyFiles("class", true, false, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 11, true);
    }

    @Test
    public void testCodeGenInterfaceAndImpl() throws Exception {
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

        verifyFiles("java", true, true, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 14, true);
        verifyFiles("class", true, true, "superbooks", "org.apache.cxf.jaxrs.model.wadl", 14, true);
    }

    @Test
    public void testCodeGenHyphen() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstoreHyphen.xml"));
        context.put(WadlToolConstants.CFG_IMPL, "true");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(3, files.size());
        assertTrue(checkContains(files, "application" + ".BookstoreResource.class"));
        assertTrue(checkContains(files, "generated" + ".TestCompositeObject.class"));
        assertTrue(checkContains(files, "generated" + ".ObjectFactory.class"));
    }
    
    @Test
    public void testCodeGenDigit() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstoreDigit.xml"));
        context.put(WadlToolConstants.CFG_IMPL, "true");
        context.put(WadlToolConstants.CFG_COMPILE, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, ".class");
        assertEquals(4, files.size());
        assertTrue(checkContains(files, "Api1" + ".BookstoreResource.class"));
        assertTrue(checkContains(files, "application" + ".BookstoreResource.class"));
        assertTrue(checkContains(files, "generated" + ".TestCompositeObject.class"));
        assertTrue(checkContains(files, "generated" + ".ObjectFactory.class"));
    }

    @Test
    public void testCodeGenTypesOnly() throws Exception {
        JAXRSContainer container = new JAXRSContainer(null);

        ToolContext context = new ToolContext();
        context.put(WadlToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        context.put(WadlToolConstants.CFG_WADLURL, getLocation("/wadl/bookstore.xml"));
        context.put(WadlToolConstants.CFG_TYPES, "true");

        container.setContext(context);
        container.execute();

        assertNotNull(output.list());

        verifyTypes("superbooks", "java", true);
    }

    private void verifyFiles(String ext, boolean subresourceExpected, boolean interfacesAndImpl,
                             String schemaPackage, String resourcePackage, int expectedCount) {
        verifyFiles(ext, subresourceExpected, interfacesAndImpl, schemaPackage, resourcePackage,
                    expectedCount, false);
    }

    private void verifyFiles(String ext, boolean subresourceExpected, boolean interfacesAndImpl,
                             String schemaPackage, String resourcePackage, int expectedCount,
                             boolean enumTypeExpected) {
        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, "." + ext);
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
        List<File> files = FileUtils.getFilesRecurseUsingSuffix(output, "." + ext);
        assertEquals(enumTypeExpected ? 6 : 5, files.size());
        doVerifyTypes(files, schemaPackage, ext);
    }

    private static void doVerifyTypes(List<File> files, String schemaPackage, String ext) {
        assertTrue(checkContains(files, schemaPackage + ".Book." + ext));
        assertTrue(checkContains(files, schemaPackage + ".TheBook2." + ext));
        assertTrue(checkContains(files, schemaPackage + ".Chapter." + ext));
        assertTrue(checkContains(files, schemaPackage + ".ObjectFactory." + ext));
        assertTrue(checkContains(files, schemaPackage + ".package-info." + ext));
    }

    private static void doVerifyTypesWithSuffix(List<File> files, String schemaPackage, String ext) {
        assertTrue(checkContains(files, schemaPackage + ".BookDTO." + ext));
        assertTrue(checkContains(files, schemaPackage + ".TheBook2DTO." + ext));
        assertTrue(checkContains(files, schemaPackage + ".ChapterDTO." + ext));
        assertTrue(checkContains(files, schemaPackage + ".ObjectFactory." + ext));
        assertTrue(checkContains(files, schemaPackage + ".package-info." + ext));
    }

    private static boolean checkContains(List<File> clsFiles, String name) {
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