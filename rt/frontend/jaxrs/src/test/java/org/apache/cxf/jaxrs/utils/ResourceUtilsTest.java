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
package org.apache.cxf.jaxrs.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.Customer;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.BookInterface;
import org.apache.cxf.jaxrs.resources.Chapter;
import org.apache.cxf.jaxrs.resources.SuperBook;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResourceUtilsTest {

    @Test
    public void testFindResourceConstructor() {
        Constructor<?> c = ResourceUtils.findResourceConstructor(Customer.class, true);
        assertNotNull(c);
        assertEquals(2, c.getParameterTypes().length);
        assertEquals(UriInfo.class, c.getParameterTypes()[0]);
        assertEquals(String.class, c.getParameterTypes()[1]);
    }

    @Test
    public void testClassResourceInfoUserResource() throws Exception {
        UserResource ur = new UserResource();
        ur.setName(HashMap.class.getName());
        ur.setPath("/hashmap");
        UserOperation op = new UserOperation();
        op.setPath("/key/{id}");
        op.setName("get");
        op.setVerb("POST");
        op.setParameters(Collections.singletonList(new Parameter(ParameterType.PATH, "id")));
        ur.setOperations(Collections.singletonList(op));

        Map<String, UserResource> resources = new HashMap<>();
        resources.put(ur.getName(), ur);
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(resources, ur, null, true, true, BusFactory.getDefaultBus());
        assertNotNull(cri);
        assertEquals("/hashmap", cri.getURITemplate().getValue());
        Method method =
            HashMap.class.getMethod("get", new Class[]{Object.class});
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(method);
        assertNotNull(ori);
        assertEquals("/key/{id}", ori.getURITemplate().getValue());
        List<Parameter> params = ori.getParameters();
        assertNotNull(params);
        Parameter p = params.get(0);
        assertEquals("id", p.getName());
    }

    @Test
    public void testUserResourceFromFile() throws Exception {
        List<UserResource> list =
            ResourceUtils.getUserResources("classpath:/resources.xml");
        assertNotNull(list);
        assertEquals(1, list.size());
        UserResource resource = list.get(0);
        assertEquals("java.util.Map", resource.getName());
        assertEquals("map", resource.getPath());
        assertEquals("application/xml", resource.getProduces());
        assertEquals("application/json", resource.getConsumes());
        UserOperation oper = resource.getOperations().get(0);
        assertEquals("putAll", oper.getName());
        assertEquals("/putAll", oper.getPath());
        assertEquals("PUT", oper.getVerb());
        assertEquals("application/json", oper.getProduces());
        assertEquals("application/xml", oper.getConsumes());

        Parameter p = oper.getParameters().get(0);
        assertEquals("map", p.getName());
        assertEquals("emptyMap", p.getDefaultValue());
        assertTrue(p.isEncoded());
        assertEquals("REQUEST_BODY", p.getType().toString());
    }

    @Test
    public void testGetAllJaxbClasses() {
        ClassResourceInfo cri1 =
            ResourceUtils.createClassResourceInfo(BookInterface.class, BookInterface.class, true, true);
        Map<Class<?>, Type> types =
            ResourceUtils.getAllRequestResponseTypes(Collections.singletonList(cri1), true)
                .getAllTypes();
        assertEquals(2, types.size());
        assertTrue(types.containsKey(Book.class));
        assertTrue(types.containsKey(Chapter.class));
    }
    @Test
    public void testGetAllJaxbClasses2() {
        ClassResourceInfo cri1 =
            ResourceUtils.createClassResourceInfo(IProductResource.class, IProductResource.class, true, true);
        Map<Class<?>, Type> types =
            ResourceUtils.getAllRequestResponseTypes(Collections.singletonList(cri1), true)
                .getAllTypes();
        assertEquals(2, types.size());
        assertTrue(types.containsKey(Book.class));
        assertTrue(types.containsKey(Chapter.class));
    }

    @Test
    public void testGetAllJaxbClassesComplexGenericType() {
        ClassResourceInfo cri1 =
            ResourceUtils.createClassResourceInfo(OrderResource.class,
                                                  OrderResource.class, true, true);
        Map<Class<?>, Type> types =
            ResourceUtils.getAllRequestResponseTypes(Collections.singletonList(cri1), true)
                .getAllTypes();
        assertEquals(2, types.size());
        assertTrue(types.containsKey(OrderItemsDTO.class));
        assertTrue(types.containsKey(OrderItemDTO.class));
    }

    @Test
    public void testClassResourceInfoWithOverride() throws Exception {
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(ExampleImpl.class, ExampleImpl.class, true, true);
        assertNotNull(cri);
        Method m = ExampleImpl.class.getMethod("get");
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        assertNotNull(ori);
        assertEquals("GET", ori.getHttpMethod());
    }
    
    @Test
    public void testClassResourceInfoWithBridgeMethod() throws Exception {
        ClassResourceInfo cri =
            ResourceUtils.createClassResourceInfo(ExampleBridgeImpl.class, ExampleBridgeImpl.class, true, true);
        assertNotNull(cri);
        assertEquals(1, cri.getMethodDispatcher().getOperationResourceInfos().size());
        
        Method m = ExampleBridgeImpl.class.getMethod("get");
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        assertNotNull(ori);
        assertEquals("GET", ori.getHttpMethod());
        
        m = Arrays
            .stream(ExampleBridgeImpl.class.getMethods())
            .filter(method -> method.getName().equals("get"))
            .filter(Method::isBridge)
            .findAny()
            .orElse(null);
        
        ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        assertNotNull(ori);
        assertEquals("GET", ori.getHttpMethod());
    }
    
    @Test
    public void testGenericClassResourceInfoWithBridgeMethod() throws Exception {
        ClassResourceInfo cri = ResourceUtils.createClassResourceInfo(GenericExampleBridgeImpl.class, 
            GenericExampleBridgeImpl.class, true, true);
        assertNotNull(cri);
        assertEquals(1, cri.getMethodDispatcher().getOperationResourceInfos().size());
        
        Method m = GenericExampleBridgeImpl.class.getMethod("get");
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        assertNotNull(ori);
        assertEquals("GET", ori.getHttpMethod());
        
        m = Arrays
            .stream(GenericExampleBridgeImpl.class.getMethods())
            .filter(method -> method.getName().equals("get"))
            .filter(Method::isBridge)
            .findAny()
            .orElse(null);
        
        ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        assertNotNull(ori);
        assertEquals("GET", ori.getHttpMethod());
    }
    
    @Test
    public void testGenericClassResourceInfo() throws Exception {
        ClassResourceInfo cri = ResourceUtils.createClassResourceInfo(GenericExampleImpl.class, 
                GenericExampleImpl.class, true, true);
        assertNotNull(cri);
        assertEquals(1, cri.getMethodDispatcher().getOperationResourceInfos().size());
        
        Method m = GenericExampleImpl.class.getMethod("get");
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        assertNotNull(ori);
        assertEquals("GET", ori.getHttpMethod());
        
        m = Arrays
            .stream(GenericExampleImpl.class.getMethods())
            .filter(method -> method.getName().equals("get"))
            .filter(Method::isBridge)
            .findAny()
            .orElse(null);
        
        assertNull(m);
    }

    @Path("/synth-hello")
    protected interface SyntheticHelloInterface<T> {
        @GET
        @Path("/{name}")
        T getById(@PathParam("name") T name);
    }

    protected abstract static class AbstractSyntheticHello implements SyntheticHelloInterface<String> {
        public abstract String getById(String name);
    }

    public static class SyntheticHelloInterfaceImpl
            extends AbstractSyntheticHello
            implements SyntheticHelloInterface<String> {
        @Override
        public String getById(String name) {
            return "Hello " + name + "!";
        }
    }

    @Test
    public void testClassResourceInfoWithSyntheticMethod() throws Exception {
        ClassResourceInfo cri =
                ResourceUtils.createClassResourceInfo(
                        SyntheticHelloInterfaceImpl.class,
                        SyntheticHelloInterfaceImpl.class,
                        true,
                        true);

        Method synthetic = SyntheticHelloInterfaceImpl.class.getMethod("getById", new Class[]{Object.class});
        assertTrue(synthetic.isSynthetic());

        assertNotNull(cri);
        Method notSynthetic = SyntheticHelloInterfaceImpl.class.getMethod("getById", new Class[]{String.class});
        assertFalse(notSynthetic.isSynthetic());

        cri.hasSubResources();
        assertEquals("there must be only one method, which is the getById(String)",
                1,
                cri.getMethodDispatcher().getOperationResourceInfos().size());
    }

    protected interface OverriddenInterface<T> {
        @GET
        @Path("/{key}")
        T read(@PathParam("key") String key);
    }

    @Path("overridden-string")
    protected interface OverriddenInterfaceString extends OverriddenInterface<String> {
        @NotNull @Override
        String read(String key);

        @NotNull
        Set<String> read(String key, String type);
    }

    public static class OverriddenInterfaceImpl implements OverriddenInterfaceString {
        @Override
        public String read(String key) {
            return key;
        }

        @Override
        public Set<String> read(String key, String type) {
            return Collections.singleton(key);
        }
    }

    @Test
    public void testClassResourceInfoWithOverriddenMethods() throws Exception {
        ClassResourceInfo cri =
                ResourceUtils.createClassResourceInfo(
                        OverriddenInterfaceImpl.class,
                        OverriddenInterfaceImpl.class,
                        true,
                        true);

        assertNotNull(cri);
        Method notSynthetic = OverriddenInterfaceImpl.class.getMethod("read", new Class[]{String.class});
        assertFalse(notSynthetic.isSynthetic());

        cri.hasSubResources();
        final Set<OperationResourceInfo> oris = cri.getMethodDispatcher().getOperationResourceInfos();
        assertEquals("there must be one read method", 1, oris.size());
        assertEquals(notSynthetic, oris.iterator().next().getMethodToInvoke());
    }

    @Test
    public void shouldCreateApplicationWhichInheritsApplicationPath() throws Exception {
        JAXRSServerFactoryBean application = ResourceUtils.createApplication(
                                                 new SuperApplication(), false, false, false, null);
        assertEquals("/base", application.getAddress());
    }

    @Test
    public void shouldCreateApplicationWhichOverridesApplicationPath() throws Exception {
        JAXRSServerFactoryBean application = ResourceUtils.createApplication(
                                                 new CustomApplication(), false, false, false, null);
        assertEquals("/custom", application.getAddress());
    }

    public interface IProductResource {
        @Path("/parts")
        IPartsResource getParts();
    }

    public interface IPartsResource {
        @Path("/{i}/")
        IPartsResource2 elementAt(@PathParam("i") String i);
        @Path("/parts")
        IPartsResource getParts();
        @Path("/products")
        IProductResource getProducts();
        @Path("chapter")
        Chapter get();
    }

    public interface IPartsResource2 {
        @Path("/{i}/")
        IPartsResource elementAt(@PathParam("i") String i);
        @Path("/products")
        IProductResource getProducts();
        @GET
        Book get();
    }

    @Path("example")
    public interface Example {

        @GET
        Book get();
    }
    
    @Path("example")
    public interface GenericExample<T extends Book> {

        @GET
        T get();
    }

    public static class ExampleImpl implements Example {

        @Override
        public Book get() {
            return null;
        }
    }
    
    public static class ExampleBridgeImpl implements Example {

        @Override
        public SuperBook get() {
            return null;
        }
    }
    
    public static class GenericExampleImpl implements GenericExample<Book> {

        @Override
        public Book get() {
            return null;
        }
    }
    
    public static class GenericExampleBridgeImpl implements GenericExample<Book> {

        @Override
        public SuperBook get() {
            return null;
        }
    }

    @XmlRootElement
    public static class OrderItem {

    }
    @XmlRootElement
    public static class OrderItemDTO<T> {

    }
    @XmlRootElement
    public static class OrderItemsDTO<E> {

    }

    public static class OrderResource {
        @GET
        public OrderItemsDTO<? extends OrderItemDTO<? extends OrderItem>> getOrders() {
            return null;
        }
    }

    @ApplicationPath("/base")
    private static class BaseApplication extends Application {

    }

    private static final class SuperApplication extends BaseApplication {

    }

    @ApplicationPath("/custom")
    private static final class CustomApplication extends BaseApplication {

    }
}