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
package org.apache.cxf.jaxrs.client.spec;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.client.Client;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.provider.PrimitiveTextProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientBuilderImplTest {
    @Test
    public void withCustomInstantiator() {
        final AtomicInteger create = new AtomicInteger();
        final AtomicInteger close = new AtomicInteger();
        final Client build = new ClientBuilderImpl().register(new ConfigurableImpl.Instantiator() {
            @Override
            public <T> Object create(final Class<T> cls) {
                try {
                    create.incrementAndGet();
                    return cls.getDeclaredConstructor().newInstance();
                } catch (final InstantiationException | IllegalAccessException | IllegalArgumentException 
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    fail(e.getMessage());
                }
                return null;
            }

            @Override
            public void release(final Object instance) {
                close.incrementAndGet();
            }
        }).register(PrimitiveTextProvider.class).build();
        assertEquals(1, create.get());
        assertEquals(0, close.get());
        build.close();
        assertEquals(1, create.get());
        assertEquals(1, close.get());
    }
}

