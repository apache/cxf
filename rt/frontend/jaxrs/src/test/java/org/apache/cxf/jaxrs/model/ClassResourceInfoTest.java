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

package org.apache.cxf.jaxrs.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.NameBinding;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ClassResourceInfoTest {

    @Path("/bar")
    @Produces("test/bar")
    @Consumes("test/foo")
    public static class TestClass {
        @Context UriInfo u;
        @Context HttpHeaders h;
        @Context HttpServletRequest req;
        @Context HttpServletResponse res;
        @Context ServletContext c;
        int i;

        @GET
        public void getIt() {

        }
    }

    static class TestClass1 extends TestClass {
        @GET
        public void getIt() {

        }
    }

    static class TestClass2 extends TestClass1 {
        @GET
        public void getIt() {

        }

        @Path("/same")
        public TestClass2 getThis() {
            return this;
        }

        @Path("sub")
        public TestClass3 getTestClass3() {
            return new TestClass3();
        }
    }

    @Produces("test/foo")
    static class TestClassWithProduces extends TestClass1 {
        @GET
        public void getIt() {

        }

        @Path("/same")
        public TestClassWithProduces getThis() {
            return this;
        }

        @Path("sub")
        public TestClass3 getTestClass3() {
            return new TestClass3();
        }
    }

    static class TestClass3 {
        @Context HttpServletRequest req;
        @Context HttpServletResponse res;
        @Context ServletContext c;

        @GET
        public void getIt() {

        }

        @HEAD
        public void head() {

        }
    }

    @After
    public void tearDown() {
        AbstractResourceInfo.clearAllMaps();
    }

    @Test
    public void testGetHttpContexts() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class, true);
        List<Field> fields = c.getContextFields();
        Set<Class<?>> clses = new HashSet<>();
        for (Field f : fields) {
            clses.add(f.getType());
        }
        assertEquals("5 http context fields available", 5, clses.size());
        assertTrue("Wrong fields selected",
                   clses.contains(HttpServletRequest.class)
                   && clses.contains(HttpServletResponse.class)
                   && clses.contains(ServletContext.class)
                   && clses.contains(UriInfo.class)
                   && clses.contains(HttpHeaders.class));
    }

    @Test
    public void testGetPath() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("/bar", c.getPath().value());

        c = new ClassResourceInfo(TestClass1.class);
        assertEquals("/bar", c.getPath().value());

        c = new ClassResourceInfo(TestClass2.class);
        assertEquals("/bar", c.getPath().value());
    }

    @Test
    public void testGetProduce() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("test/bar", c.getProduceMime().get(0).toString());

        c = new ClassResourceInfo(TestClass1.class);
        assertEquals("test/bar", c.getProduceMime().get(0).toString());

        c = new ClassResourceInfo(TestClass2.class);
        assertEquals("test/bar", c.getProduceMime().get(0).toString());
    }

    @Test
    public void testGetConsume() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("test/foo", c.getConsumeMime().get(0).toString());

        c = new ClassResourceInfo(TestClass1.class);
        assertEquals("test/foo", c.getConsumeMime().get(0).toString());

        c = new ClassResourceInfo(TestClass2.class);
        assertEquals("test/foo", c.getConsumeMime().get(0).toString());
    }

    @Test
    public void testGetSameSubresource() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("No subresources expected", 0, c.getSubResources().size());
        assertNull(c.findResource(TestClass.class, TestClass.class));
        ClassResourceInfo c1 = c.getSubResource(TestClass.class, TestClass.class);
        assertNotNull(c1);
        assertSame(c1, c.findResource(TestClass.class, TestClass.class));
        assertSame(c1, c.getSubResource(TestClass.class, TestClass.class));
        assertEquals("Single subresources expected", 1, c.getSubResources().size());
    }

    @Test
    public void testGetSubresourceSubclass() {
        ClassResourceInfo c = new ClassResourceInfo(TestClass.class);
        assertEquals("No subresources expected", 0, c.getSubResources().size());
        assertNull(c.findResource(TestClass.class, TestClass1.class));
        ClassResourceInfo c1 = c.getSubResource(TestClass.class, TestClass1.class);
        assertNotNull(c1);
        assertSame(c1, c.findResource(TestClass.class, TestClass1.class));
        assertNull(c.findResource(TestClass.class, TestClass2.class));
        ClassResourceInfo c2 = c.getSubResource(TestClass.class, TestClass2.class);
        assertNotNull(c2);
        assertSame(c2, c.findResource(TestClass.class, TestClass2.class));
        assertSame(c2, c.getSubResource(TestClass.class, TestClass2.class));
        assertNotSame(c1, c2);

    }

    @Test
    public void testAllowedMethods() {
        ClassResourceInfo c = ResourceUtils.createClassResourceInfo(
                                  TestClass3.class, TestClass3.class, false, false);
        Set<String> methods = c.getAllowedMethods();
        assertEquals(2, methods.size());
        assertTrue(methods.contains("HEAD") && methods.contains("GET"));
    }

    @Test
    public void testSubresourceInheritProduces() {
        ClassResourceInfo c = ResourceUtils.createClassResourceInfo(
                                  TestClass2.class, TestClass2.class, true, true);
        assertEquals("test/bar", c.getProduceMime().get(0).toString());
        ClassResourceInfo sub = c.getSubResource(TestClass2.class, TestClass3.class);
        assertNotNull(sub);
        assertEquals("test/bar", sub.getProduceMime().get(0).toString());
        sub = c.getSubResource(TestClass2.class, TestClass2.class);
        assertNotNull(sub);
        assertEquals("test/bar", sub.getProduceMime().get(0).toString());
    }

    @Test
    public void testSubresourceWithProduces() {
        ClassResourceInfo parent = ResourceUtils.createClassResourceInfo(
                                  TestClass2.class, TestClass2.class, true, true);
        ClassResourceInfo c = ResourceUtils.createClassResourceInfo(
                                  TestClassWithProduces.class, TestClassWithProduces.class, true, true);
        c.setParent(parent);
        assertEquals("test/foo", c.getProduceMime().get(0).toString());
    }

    @Test
    public void testNameBindings() {
        Application app = new TestApplication();
        JAXRSServerFactoryBean bean = ResourceUtils.createApplication(app, true, true, false, null);
        ClassResourceInfo cri = bean.getServiceFactory().getClassResourceInfo().get(0);
        Set<String> names = cri.getNameBindings();
        assertEquals(Collections.singleton(CustomNameBinding.class.getName()), names);
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface CustomNameBinding {

    }

    @CustomNameBinding
    public class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            Set<Class<?>> classes = new HashSet<>();
            classes.add(TestClass.class);
            return classes;
        }
    }
}