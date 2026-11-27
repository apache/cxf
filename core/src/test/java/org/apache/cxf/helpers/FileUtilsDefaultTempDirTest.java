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
package org.apache.cxf.helpers;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the temp directory CXF creates itself when the TempDirectory property is not set.
 */
public class FileUtilsDefaultTempDirTest {

    private static final String TEMP_DIR_PROPERTY = FileUtils.class.getName() + ".TempDirectory";

    private Object originalTempDir;
    private Object originalShutdownHook;
    private String originalProperty;

    @Before
    public void setUp() throws Exception {
        // FileUtils keeps static state, so save it and start each test from a clean state
        originalTempDir = getField("defaultTempDir");
        originalShutdownHook = getField("shutdownHook");
        originalProperty = System.getProperty(TEMP_DIR_PROPERTY);
        System.clearProperty(TEMP_DIR_PROPERTY);
        setField("defaultTempDir", null);
        setField("shutdownHook", null);
    }

    @After
    public void tearDown() throws Exception {
        if (originalProperty != null) {
            System.setProperty(TEMP_DIR_PROPERTY, originalProperty);
        }
        Thread hook = (Thread)getField("shutdownHook");
        if (hook != null) {
            Runtime.getRuntime().removeShutdownHook(hook);
            hook.run();
        }
        setField("defaultTempDir", originalTempDir);
        setField("shutdownHook", originalShutdownHook);
    }

    @Test
    public void testDeletedTempDirIsReplaced() throws Exception {
        File first = FileUtils.getDefaultTempDir();
        assertTrue(first.isDirectory());
        Thread firstHook = (Thread)getField("shutdownHook");

        // simulate a tmp cleaner removing the directory while the JVM is running
        FileUtils.removeDir(first);
        assertFalse(first.exists());

        File second = FileUtils.getDefaultTempDir();
        assertTrue(second.isDirectory());
        assertNotEquals(first, second);
        // the old hook is replaced by one for the new directory
        assertNotNull(getField("shutdownHook"));
        assertFalse(Runtime.getRuntime().removeShutdownHook(firstHook));
    }

    @Test
    public void testTempFileCanBeCreatedAfterTempDirIsDeleted() throws Exception {
        File first = FileUtils.getDefaultTempDir();
        FileUtils.removeDir(first);

        File tempFile = FileUtils.createTempFile("cos", "tmp");
        assertTrue(tempFile.isFile());
        assertNotEquals(first, tempFile.getParentFile());
    }

    private static Object getField(String name) throws Exception {
        Field f = FileUtils.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    private static void setField(String name, Object value) throws Exception {
        Field f = FileUtils.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }
}
