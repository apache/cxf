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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProviderFactoryExceptionMapperTest {

    private ServerProviderFactory pf;

    @Before
    public void setUp() throws Exception {
        pf = ServerProviderFactory.getInstance();
        pf.registerUserProvider(new RuntimeExceptionMapper2());
        pf.registerUserProvider(new RuntimeExceptionMapper1());
        pf.registerUserProvider(new RuntimeExceptionMapper());
    }

    @Test
    public void testExactMatchInFirstPosition() {
        ExceptionMapper<?> exceptionMapper = pf.createExceptionMapper(RuntimeException2.class,
                                                                      new MessageImpl());
        assertEquals("Wrong mapper found for RuntimeException2", RuntimeExceptionMapper2.class,
                     exceptionMapper.getClass());
    }

    @Test
    public void testExactMatchInSecondPosition() {
        ExceptionMapper<?> exceptionMapper = pf.createExceptionMapper(RuntimeException1.class,
                                                                      new MessageImpl());
        assertEquals("Wrong mapper found for RuntimeException1", RuntimeExceptionMapper1.class,
                     exceptionMapper.getClass());
    }

    @Test
    public void testNearestSuperclassMatch() {
        ExceptionMapper<?> exceptionMapper = pf.createExceptionMapper(NullPointerException.class,
                                                                      new MessageImpl());
        assertEquals("Wrong mapper found for NullPointerException", RuntimeExceptionMapper.class,
                     exceptionMapper.getClass());
    }

    private static final class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {
        @Override
        public Response toResponse(RuntimeException ex) {
            return null;
        }
    }

    private static final class RuntimeExceptionMapper1 implements ExceptionMapper<RuntimeException1> {
        @Override
        public Response toResponse(RuntimeException1 ex) {
            return null;
        }
    }

    private static final class RuntimeExceptionMapper2 implements ExceptionMapper<RuntimeException2> {
        @Override
        public Response toResponse(RuntimeException2 ex) {
            return null;
        }
    }

    @SuppressWarnings("serial")
    private static class RuntimeException1 extends RuntimeException {

    }

    @SuppressWarnings("serial")
    private static final class RuntimeException2 extends RuntimeException1 {

    }
}
