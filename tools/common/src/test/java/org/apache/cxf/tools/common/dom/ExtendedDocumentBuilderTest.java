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

package org.apache.cxf.tools.common.dom;

import org.w3c.dom.Document;

import org.junit.Assert;
import org.junit.Test;

public class ExtendedDocumentBuilderTest extends Assert {
    @Test
    public void testMassMethod() throws Exception {
        ExtendedDocumentBuilder builder = new ExtendedDocumentBuilder();
        builder.setValidating(false);
        String tsSource = "/org/apache/cxf/tools/common/toolspec/parser/resources/testtool.xml";
        assertTrue(builder.parse(getClass().getResourceAsStream(tsSource)) != null);
    }

    @Test
    public void testParse() throws Exception {
        ExtendedDocumentBuilder builder = new ExtendedDocumentBuilder();
        String tsSource = "/org/apache/cxf/tools/common/toolspec/parser/resources/testtool1.xml";
        Document doc = builder.parse(getClass().getResourceAsStream(tsSource));
        assertEquals(doc.getXmlVersion(), "1.0");
    }
}
