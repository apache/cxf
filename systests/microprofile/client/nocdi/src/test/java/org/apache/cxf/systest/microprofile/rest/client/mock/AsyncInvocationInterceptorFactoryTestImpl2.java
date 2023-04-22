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

package org.apache.cxf.systest.microprofile.rest.client.mock;

import java.util.List;

import jakarta.annotation.Priority;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory;

@Priority(3700)
public class AsyncInvocationInterceptorFactoryTestImpl2 implements AsyncInvocationInterceptorFactory {

    static class AsyncInvocationInterceptorTestImpl2 implements AsyncInvocationInterceptor {

        /** {@inheritDoc}*/
        @Override
        public void prepareContext() {
            List<String> list = AsyncInvocationInterceptorFactoryTestImpl.OUTBOUND.get();
            list.add(AsyncInvocationInterceptorFactoryTestImpl2.class.getSimpleName());
            list.add(Thread.currentThread().getName());
        }

        /** {@inheritDoc}*/
        @Override
        public void applyContext() {
            List<String> list = AsyncInvocationInterceptorFactoryTestImpl.INBOUND.get();
            list.add(Thread.currentThread().getName());
            list.add(AsyncInvocationInterceptorFactoryTestImpl2.class.getSimpleName());
        }

        /** {@inheritDoc}*/
        @Override
        public void removeContext() {
            List<String> list = AsyncInvocationInterceptorFactoryTestImpl.INBOUND.get();
            list.add("REMOVE-" + Thread.currentThread().getName());
            list.add("REMOVE-" + AsyncInvocationInterceptorFactoryTestImpl.class.getSimpleName());
        }
    }

    /** {@inheritDoc}*/
    @Override
    public AsyncInvocationInterceptor newInterceptor() {
        return new AsyncInvocationInterceptorTestImpl2();
    }

}
