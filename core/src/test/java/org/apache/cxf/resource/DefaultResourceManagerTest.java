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

package org.apache.cxf.resource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultResourceManagerTest {

    private DefaultResourceManager manager;
    private DummyResolver[] resolvers;

    @Before
    public void setUp() {
        AtomicInteger ordering = new AtomicInteger(0);

        resolvers = new DummyResolver[4];
        for (int i = 0; i < resolvers.length; i++) {
            resolvers[i] = new DummyResolver(ordering);
        }

        manager = new DefaultResourceManager(resolvers[resolvers.length - 1]);

        for (int i = resolvers.length - 2; i >= 0; i--) {
            manager.addResourceResolver(resolvers[i]);
        }
    }

    @After
    public void tearDown() {
        manager = null;
        resolvers = null;
    }

    @Test
    public void testResolverOrder() {
        assertArrayEquals(resolvers, getResolvers(manager));

        checkCallOrder(resolvers);
    }

    @Test
    public void testCopiedResolverOrder() {
        ResourceManager newManager = new DefaultResourceManager(manager.getResourceResolvers());

        assertArrayEquals(resolvers, getResolvers(newManager));

        checkCallOrder(resolvers);
    }

    @Test
    public void testAddResolverList() {
        List<ResourceResolver> addedResolvers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            addedResolvers.add(new DummyResolver(resolvers[0].source));
        }

        manager.addResourceResolvers(addedResolvers);

        DummyResolver[] expected = new DummyResolver[addedResolvers.size() + resolvers.length];
        addedResolvers.toArray(expected);
        System.arraycopy(resolvers, 0, expected, addedResolvers.size(), resolvers.length);

        assertArrayEquals(expected, getResolvers(manager));

        checkCallOrder(expected);
    }

    @Test
    public void testAddDuplicateResolver() {
        manager.addResourceResolver(resolvers[1]);

        assertArrayEquals(resolvers, getResolvers(manager));

        checkCallOrder(resolvers);
    }

    @Test
    public void testAddDuplicateResolverList() {
        manager.addResourceResolvers(new ArrayList<>(manager.getResourceResolvers()));

        assertArrayEquals(resolvers, getResolvers(manager));

        checkCallOrder(resolvers);
    }

    @Test
    public void testRemoveResolver() {
        manager.removeResourceResolver(resolvers[1]);

        DummyResolver[] expected = new DummyResolver[resolvers.length - 1];
        expected[0] = resolvers[0];
        System.arraycopy(resolvers, 2, expected, 1, expected.length - 1);

        assertArrayEquals(expected, getResolvers(manager));

        checkCallOrder(expected);

        assertEquals(-1, resolvers[1].order);
    }

    @Test
    public void testLiveResolverList() {
        List<ResourceResolver> currentResolvers = manager.getResourceResolvers();
        DummyResolver newResolver = new DummyResolver(resolvers[0].source);

        assertFalse(currentResolvers.contains(newResolver));
        manager.addResourceResolver(newResolver);
        assertTrue(currentResolvers.contains(newResolver));

        assertTrue(currentResolvers.contains(resolvers[1]));
        manager.removeResourceResolver(resolvers[1]);
        assertFalse(currentResolvers.contains(resolvers[1]));
    }

    private ResourceResolver[] getResolvers(ResourceManager resourceManager) {
        List<ResourceResolver> list = resourceManager.getResourceResolvers();
        ResourceResolver[] actual = new ResourceResolver[list.size()];
        return list.toArray(actual);
    }

    private void checkCallOrder(DummyResolver[] usedResolvers) {
        manager.resolveResource(null, Void.class);

        int[] expected = new int[usedResolvers.length];
        int[] actual = new int[usedResolvers.length];
        for (int i = 0; i < usedResolvers.length; i++) {
            expected[i] = i;
            actual[i] = usedResolvers[i].order;
        }

        assertArrayEquals(expected, actual);
    }

    private static class DummyResolver implements ResourceResolver {
        AtomicInteger source;
        int order = -1;

        DummyResolver(AtomicInteger source) {
            this.source = source;
        }

        @Override
        public <T> T resolve(String resourceName, Class<T> resourceType) {
            order = source.getAndIncrement();
            return null;
        }

        @Override
        public InputStream getAsStream(String name) {
            return null;
        }
    }
}