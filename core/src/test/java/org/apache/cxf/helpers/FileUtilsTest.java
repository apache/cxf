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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileUtilsTest {


    @Test
    public void testTempIODirExists() throws Exception {

        String originaltmpdir = System.getProperty("java.io.tmpdir");
        try {
            System.setProperty("java.io.tmpdir", "dummy");
            FileUtils.createTempFile("foo", "bar");
        } catch (RuntimeException e) {
            assertTrue(e.toString().contains("please set java.io.tempdir to an existing directory"));
        } finally {
            System.setProperty("java.io.tmpdir", originaltmpdir);
        }
    }

    @Test
    public void testReadLines() throws Exception {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        Optional<Path> p =
            Files.find(Paths.get(basedir), 20, (path, attrs) -> path.getFileName().endsWith("FileUtilsTest.java"))
                .findFirst();
        assertTrue(p.isPresent());

        List<String> lines = FileUtils.readLines(p.get().toFile());
        assertFalse(lines.isEmpty());
    }

    @Test
    public void testGetFiles() throws URISyntaxException {
        URL resource = FileUtilsTest.class.getResource("FileUtilsTest.class");
        File directory = Paths.get(resource.toURI()).getParent().toFile();
        assertTrue(directory.exists());

        List<File> foundFiles = FileUtils.getFilesUsingSuffix(directory, ".class");
        assertFalse(foundFiles.isEmpty());

        List<File> foundFiles2 = FileUtils.getFiles(directory, ".*\\.class$");

        assertTrue(foundFiles.containsAll(foundFiles2) && foundFiles2.containsAll(foundFiles));
    }
}
