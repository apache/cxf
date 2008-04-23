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

package org.apache.cxf.tools.wsdlto;

import org.junit.Assert;
import org.junit.Test;


public class WSDLToJavaTest extends Assert {

    @Test
    public void testGetFrontEndName() throws Exception {
        WSDLToJava w2j = new WSDLToJava();
        assertEquals("jaxws", w2j.getFrontEndName(new String[]{"-frontend", "jaxws"}));
        assertEquals("jaxws", w2j.getFrontEndName(new String[]{"-fe", "jaxws"}));
        assertNull(w2j.getFrontEndName(new String[]{"-frontend"}));
        assertNull(w2j.getFrontEndName(new String[]{"-fe"}));
        assertNull(w2j.getFrontEndName(new String[]{"nothing"}));
        assertNull(w2j.getFrontEndName(null));
    }

    @Test
    public void testGetDataBindingName() throws Exception {
        WSDLToJava w2j = new WSDLToJava();
        assertEquals("jaxb", w2j.getDataBindingName(new String[]{"-databinding", "jaxb"}));
        assertEquals("jaxb", w2j.getDataBindingName(new String[]{"-db", "jaxb"}));
        assertNull(w2j.getDataBindingName(new String[]{"-databinding"}));
        assertNull(w2j.getDataBindingName(new String[]{"-db"}));
        assertNull(w2j.getDataBindingName(new String[]{"nothing"}));
        assertNull(w2j.getDataBindingName(null));
        assertNull(w2j.getDataBindingName(new String[]{"-frontend", "jaxws"}));
    }

    @Test
    public void testIsVerbose() {
        WSDLToJava w2j = new WSDLToJava();
        w2j.setArguments(new String[]{"-V"});
        assertTrue(w2j.isVerbose());
        w2j = new WSDLToJava();
        w2j.setArguments(new String[]{"-verbose"});
        assertTrue(w2j.isVerbose());
        w2j = new WSDLToJava();
        w2j.setArguments(new String[]{"none"});
        assertFalse(w2j.isVerbose());
    }
}
