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

package org.apache.cxf.attachment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ContentDispositionTest {

    @Test
    public void testExtendedFilenameIsDecoded() {
        ContentDisposition cd = new ContentDisposition("attachment;filename*=UTF-8''a%20file.txt");
        assertEquals("attachment", cd.getType());
        assertEquals("a file.txt", cd.getFilename());
    }

    @Test(timeout = 10000)
    public void testMalformedExtendedFilenameDoesNotOverflow() {
        StringBuilder value = new StringBuilder("attachment;filename*=UTF-8''");
        for (int i = 0; i < 2000; i++) {
            value.append("%41");
        }
        value.append(" x");

        ContentDisposition cd = new ContentDisposition(value.toString());

        assertNull(cd.getFilename());
    }
}