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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NameBinding;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OperationResourceInfoTest {

    @Produces("text/xml")
    @Consumes("application/xml")
    static class TestClass {
        @Produces("text/plain")
        public void doIt() {
            // empty
        };
        @Consumes("application/atom+xml")
        public void doThat() {
            // empty
        };

    }
    
    @Produces("text/xml")
    @Consumes("application/xml")
    interface TestInteface {
        @Produces("text/plain")
        void doIt();

        @CustomNameBinding
        @Consumes("application/atom+xml")
        void doThat();
    }

    static class TestClass2 implements TestInteface {
        @CustomNameBinding
        @Override
        public void doIt() {
        }

        @Override
        public void doThat() {
        }

    }


    @Test
    public void testConsumeTypes() throws Exception {
        OperationResourceInfo ori1 = new OperationResourceInfo(
                                 TestClass.class.getMethod("doIt", new Class[]{}),
                                 new ClassResourceInfo(TestClass.class));

        List<MediaType> ctypes = ori1.getConsumeTypes();
        assertEquals("Single media type expected", 1, ctypes.size());
        assertEquals("Class resource consume type should be used",
                   "application/xml", ctypes.get(0).toString());

        OperationResourceInfo ori2 = new OperationResourceInfo(
                                 TestClass.class.getMethod("doThat", new Class[]{}),
                                 new ClassResourceInfo(TestClass.class));
        ctypes = ori2.getConsumeTypes();
        assertEquals("Single media type expected", 1, ctypes.size());
        assertEquals("Method consume type should be used",
                   "application/atom+xml", ctypes.get(0).toString());
    }

    @Test
    public void testProduceTypes() throws Exception {

        OperationResourceInfo ori1 = new OperationResourceInfo(
                                       TestClass.class.getMethod("doIt", new Class[]{}),
                                       new ClassResourceInfo(TestClass.class));

        List<MediaType> ctypes = ori1.getProduceTypes();
        assertEquals("Single media type expected", 1, ctypes.size());
        assertEquals("Method produce type should be used",
                   "text/plain", ctypes.get(0).toString());

        OperationResourceInfo ori2 = new OperationResourceInfo(
                                 TestClass.class.getMethod("doThat", new Class[]{}),
                                 new ClassResourceInfo(TestClass.class));
        ctypes = ori2.getProduceTypes();
        assertEquals("Single media type expected", 1, ctypes.size());
        assertEquals("Class resource produce type should be used",
                     "text/xml", ctypes.get(0).toString());
    }

    @Test
    public void testComparator1() throws Exception {
        OperationResourceInfo ori1 = new OperationResourceInfo(
                                                               TestClass.class.getMethod("doIt", new Class[]{}),
                                                               new ClassResourceInfo(TestClass.class));
        ori1.setURITemplate(new URITemplate("/"));

        OperationResourceInfo ori2 = new OperationResourceInfo(
                                                               TestClass.class.getMethod("doThat", new Class[]{}),
                                                               new ClassResourceInfo(TestClass.class));

        ori2.setURITemplate(new URITemplate("/"));

        OperationResourceInfoComparator cmp = new OperationResourceInfoComparator(null, null);


        int result = cmp.compare(ori1,  ori2);
        assertEquals(0, result);
    }

    @Test
    public void testComparator2() throws Exception {
        Message m = createMessage();

        OperationResourceInfo ori1 = new OperationResourceInfo(
                                                               TestClass.class.getMethod("doIt", new Class[]{}),
                                                               new ClassResourceInfo(TestClass.class));
        ori1.setURITemplate(new URITemplate("/"));

        OperationResourceInfo ori2 = new OperationResourceInfo(
                                                               TestClass.class.getMethod("doThat", new Class[]{}),
                                                               new ClassResourceInfo(TestClass.class));

        ori2.setURITemplate(new URITemplate("/"));

        OperationResourceInfoComparator cmp = new OperationResourceInfoComparator(m, "POST", false,
            MediaType.WILDCARD_TYPE, Collections.singletonList(MediaType.WILDCARD_TYPE));


        int result = cmp.compare(ori1,  ori2);
        assertEquals(0, result);
    }

    @Test
    public void testNameBindingsClass() throws NoSuchMethodException, SecurityException {
        final Method method = TestClass2.class.getMethod("doIt", new Class[]{});

        OperationResourceInfo ori = new OperationResourceInfo(
                method,
                AnnotationUtils.getAnnotatedMethod(TestClass2.class,  method),
                new ClassResourceInfo(TestClass2.class));

        final Set<String> names = ori.getNameBindings();
        assertEquals(Collections.singleton(CustomNameBinding.class.getName()), names);
    }

    @Test
    public void testNameBindingsInterface() throws NoSuchMethodException, SecurityException {
        final Method method = TestClass2.class.getMethod("doThat", new Class[]{});

        OperationResourceInfo ori = new OperationResourceInfo(
                method,
                AnnotationUtils.getAnnotatedMethod(TestClass2.class, method),
                new ClassResourceInfo(TestClass2.class));

        Set<String> names = ori.getNameBindings();
        assertEquals(Collections.singleton(CustomNameBinding.class.getName()), names);
    }

    private static Message createMessage() {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.get("org.apache.cxf.jaxrs.comparator")).thenReturn(null);
        e.put(Endpoint.class, endpoint);
        return m;
    }
    
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface CustomNameBinding {
    }
}
