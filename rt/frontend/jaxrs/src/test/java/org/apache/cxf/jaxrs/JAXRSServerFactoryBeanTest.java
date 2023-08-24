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
package org.apache.cxf.jaxrs;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.resources.BookStore;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class JAXRSServerFactoryBeanTest {

    @Test
    public void testRegisterProviders() {
        JAXRSServerFactoryBean bean = new JAXRSServerFactoryBean();
        bean.setAddress("http://localhost:8080/rest");
        bean.setStart(false);
        bean.setResourceClasses(BookStore.class);
        List<Object> providers = new ArrayList<>();
        Object provider1 = new CustomExceptionMapper();
        providers.add(provider1);
        Object provider2 = new RuntimeExceptionMapper();
        providers.add(provider2);
        bean.setProviders(providers);
        Server s = bean.create();

        ServerProviderFactory factory =
            (ServerProviderFactory)s.getEndpoint().get(ServerProviderFactory.class.getName());

        ExceptionMapper<Exception> mapper1 =
            factory.createExceptionMapper(Exception.class, new MessageImpl());
        assertNotNull(mapper1);
        ExceptionMapper<RuntimeException> mapper2 =
            factory.createExceptionMapper(RuntimeException.class, new MessageImpl());
        assertNotNull(mapper2);
        assertNotSame(mapper1, mapper2);
        assertSame(provider1, mapper1);
        assertSame(provider2, mapper2);


    }


    private static final class CustomExceptionMapper implements ExceptionMapper<Exception> {

        public Response toResponse(Exception exception) {
            return null;
        }

    }

    private static final class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

        public Response toResponse(RuntimeException exception) {
            return null;
        }

    }
}