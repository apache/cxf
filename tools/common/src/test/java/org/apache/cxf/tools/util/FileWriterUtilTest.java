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

package org.apache.cxf.tools.util;

import java.io.File;
import java.io.Writer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileWriterUtilTest {

    @Rule
    public TemporaryFolder targetDir = new TemporaryFolder();

    @Test
    public void testGetFile() throws Exception {
        FileWriterUtil fileWriter = new FileWriterUtil(targetDir.getRoot().getAbsolutePath(), null);
        try (Writer w = fileWriter.getWriter("com.iona.test", "A.java")) {
            assertTrue(new File(targetDir.getRoot(), "/com/iona/test/A.java").canWrite());
        }
    }

    @Test
    public void testGetWriter() throws Exception {
        FileWriterUtil fileWriter = new FileWriterUtil(targetDir.getRoot().getAbsolutePath(), null);
        try (Writer w = fileWriter.getWriter("com.iona.test.SAMPLE", "A.java")) {
            assertNotNull(w);
        }
    }

}