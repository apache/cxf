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

package org.apache.cxf.test;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import junit.framework.AssertionFailedError;

import org.apache.cxf.helpers.DOMUtils;
import org.junit.Assert;
import org.junit.Test;

public class XPathAssertTest extends Assert {
    
    @Test
    public void testAssert() throws Exception {
        Document document = DOMUtils.readXml(getClass().getResourceAsStream("test.xml"));

        XPathAssert.assertValid("//a", document, null);
        XPathAssert.assertInvalid("//aasd", document, null);

        try {
            XPathAssert.assertInvalid("//a", document, null);
            fail("Expression is valid!");
        } catch (AssertionFailedError e) {
            // this is correct
        }

        try {
            XPathAssert.assertValid("//aa", document, null);
            fail("Expression is invalid!");
        } catch (AssertionFailedError e) {
            // this is correct
        }

        XPathAssert.assertXPathEquals("//b", "foo", document, null);
    }

    @Test
    public void testAssertNamespace() throws Exception {
        Document document = DOMUtils.readXml(getClass().getResourceAsStream("test2.xml"));

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("a", "urn:foo");
        namespaces.put("z", "urn:z");

        XPathAssert.assertValid("//a:a", document, namespaces);
        XPathAssert.assertValid("//z:b", document, namespaces);
    }
}
