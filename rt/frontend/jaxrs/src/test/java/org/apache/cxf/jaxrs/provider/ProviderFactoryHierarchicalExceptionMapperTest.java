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
package org.apache.cxf.jaxrs.provider;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.apache.cxf.message.MessageImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for CXF-7473
 */
public class ProviderFactoryHierarchicalExceptionMapperTest {
    private ServerProviderFactory pf;

    @Before
    public void setUp() throws Exception {
        pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new IllegalArgumentExceptionMapper());
        pf.registerUserProvider(new IllegalStateExceptionMapper());
    }

    @Test
    public void testExceptionMapperInHierarchy() {
        ExceptionMapper<?> exceptionMapper = pf.createExceptionMapper(IllegalArgumentException.class,
                new MessageImpl());
        Assert.assertNotNull(exceptionMapper);
        Assert.assertEquals("Wrong mapper found for IllegalArgumentException",
                IllegalArgumentExceptionMapper.class, exceptionMapper.getClass());
    }

    @Test
    public void testSimpleExceptionMapperWhenHierarchyPresent() {
        ExceptionMapper<?> exceptionMapper = pf.createExceptionMapper(IllegalStateException.class,
                new MessageImpl());
        Assert.assertNotNull(exceptionMapper);
        Assert.assertEquals("Wrong mapper found for IllegalStateException",
                IllegalStateExceptionMapper.class, exceptionMapper.getClass());
    }

    @Test
    public void testNoMatch() {
        ExceptionMapper<?> exceptionMapper = pf.createExceptionMapper(UnmappedRuntimeException.class,
                new MessageImpl());
        Assert.assertNull(exceptionMapper);
    }

    public abstract class AbstractExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {
        @Override
        public Response toResponse(E exception) {
            return toResponse0(exception);
        }

        protected abstract Response toResponse0(E exception);
    }

    public class IllegalArgumentExceptionMapper extends AbstractExceptionMapper<IllegalArgumentException> {
        @Override
        protected Response toResponse0(IllegalArgumentException exception) {
            return Response
                    .serverError()
                    .entity("Processed by IllegalArgumentExceptionMapper: " + exception.getMessage())
                    .build();
        }
    }

    public class IllegalStateExceptionMapper implements ExceptionMapper<IllegalStateException> {
        @Override
        public Response toResponse(IllegalStateException exception) {
            return Response
                    .serverError()
                    .entity("Processed by IllegalStateExceptionMapper: " + exception.getMessage())
                    .build();
        }
    }

    public class UnmappedRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UnmappedRuntimeException(String message) {
            super(message);
        }
    }
}
