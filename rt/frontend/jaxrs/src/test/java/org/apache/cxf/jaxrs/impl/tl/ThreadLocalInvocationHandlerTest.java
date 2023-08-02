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

package org.apache.cxf.jaxrs.impl.tl;

import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Proxy;
import java.net.SocketException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThreadLocalInvocationHandlerTest {

    private static final String CHECKED_EXCEPTION_MSG = "Throwing a checked exception.";
    private static final String UNCHECKED_EXCEPTION_MSG = "Throwing an unchecked exception.";
    private static final String ERROR_MSG = "Throwing an error.";
    private static final String THROWABLE_MSG = "Throwing a throwable.";

    private TestIface testIface;

    private ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException getExpectedExceptionRule() {
        return expectedException;
    }

    @Before
    public void setUp() throws Exception {
        ThreadLocalInvocationHandler<TestClass> subject = new ThreadLocalInvocationHandler<>();
        subject.set(new TestClass());

        testIface = (TestIface) Proxy.newProxyInstance(ThreadLocalInvocationHandler.class.getClassLoader(),
                new Class[] {TestIface.class}, subject);
    }

    @Test
    public void testCheckedExceptionPropagation() throws Exception {
        expectedException.expect(SocketException.class);
        expectedException.expectMessage(CHECKED_EXCEPTION_MSG);

        testIface.throwCheckedException();
    }

    @Test
    public void testUncheckedExceptionPropagation() {
        expectedException.expect(IndexOutOfBoundsException.class);
        expectedException.expectMessage(UNCHECKED_EXCEPTION_MSG);

        testIface.throwUncheckedException();
    }

    @Test
    public void testErrorPropagation() {
        expectedException.expect(AnnotationFormatError.class);
        expectedException.expectMessage(ERROR_MSG);

        testIface.throwError();
    }

    @Test
    public void testThrowablePropagation() throws Throwable {
        expectedException.expect(Throwable.class);
        expectedException.expectMessage(THROWABLE_MSG);

        testIface.throwThrowable();
    }

    private interface TestIface {
        void throwCheckedException() throws Exception;

        void throwUncheckedException();

        void throwError();

        void throwThrowable() throws Throwable;
    }

    private final class TestClass implements TestIface {

        @Override
        public void throwCheckedException() throws Exception {
            throw new SocketException(CHECKED_EXCEPTION_MSG);
        }

        @Override
        public void throwUncheckedException() {
            throw new IndexOutOfBoundsException(UNCHECKED_EXCEPTION_MSG);
        }

        @Override
        public void throwError() {
            throw new AnnotationFormatError(ERROR_MSG);
        }

        @Override
        public void throwThrowable() throws Throwable {
            throw new Throwable(THROWABLE_MSG);
        }
    }
}
