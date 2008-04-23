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

package org.apache.cxf.aegis.standalone;

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.aegis.services.SimpleBean;
import org.apache.cxf.test.TestUtilities;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 
 */
public class StandaloneReadTest {
    private AegisContext context;
    private TestUtilities testUtilities;
    
    @Before
    public void before() {
        testUtilities = new TestUtilities(getClass());
    }
    
    @Test
    public void testBasicTypeRead() throws Exception {
        context = new AegisContext();
        context.initialize();
        XMLStreamReader streamReader = testUtilities.getResourceAsXMLStreamReader("stringElement.xml");
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        Object something = reader.read(streamReader);
        assertTrue("ball-of-yarn".equals(something));
    }
    
    // test using a .aegis.xml
    @Test
    public void testSimpleBeanRead() throws Exception {
        context = new AegisContext();
        Set<Class<?>> rootClasses = new HashSet<Class<?>>();
        rootClasses.add(SimpleBean.class);
        context.setRootClasses(rootClasses);
        context.initialize();
        XMLStreamReader streamReader = 
            testUtilities.getResourceAsXMLStreamReader("simpleBean1.xml");
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        Object something = reader.read(streamReader);
        assertTrue(something instanceof SimpleBean);
        SimpleBean simpleBean = (SimpleBean) something;
        assertEquals("howdy", simpleBean.getHowdy());
    }
}
