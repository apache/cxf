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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.aegis.services.SimpleBean;
import org.apache.cxf.aegis.type.AegisType;
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
    
    private interface ListStringInterface {
        List<String> method();
    }
    
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
    
    @Test
    public void testCollectionReadNoXsiType() throws Exception {
        context = new AegisContext();
        Set<java.lang.reflect.Type> roots = new HashSet<java.lang.reflect.Type>();
        java.lang.reflect.Type listStringType 
            = ListStringInterface.class.getMethods()[0].getGenericReturnType();
        roots.add(listStringType);
        context.setRootClasses(roots);
        context.initialize();
        XMLStreamReader streamReader 
            = testUtilities.getResourceAsXMLStreamReader("topLevelList.xml");
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        // until I fix type mapping to use java.lang.reflect.Type instead of 
        // Class, I need to do the following 
        QName magicTypeQName = new QName("urn:org.apache.cxf.aegis.types", "ArrayOfString");
        AegisType aegisRegisteredType = context.getTypeMapping().getType(magicTypeQName);

        Object something = reader.read(streamReader, aegisRegisteredType);
        List<String> correctAnswer = new ArrayList<String>();
        correctAnswer.add("cat");
        correctAnswer.add("dog");
        correctAnswer.add("hailstorm");
        assertEquals(correctAnswer, something);
    }
    
    @Test
    public void testCollectionReadXsiType() throws Exception {
        context = new AegisContext();
        Set<java.lang.reflect.Type> roots = new HashSet<java.lang.reflect.Type>();
        java.lang.reflect.Type listStringType 
            = ListStringInterface.class.getMethods()[0].getGenericReturnType();
        roots.add(listStringType);
        context.setRootClasses(roots);
        context.initialize();
        XMLStreamReader streamReader 
            = testUtilities.getResourceAsXMLStreamReader("topLevelListWithXsiType.xml");
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();

        Object something = reader.read(streamReader);
        List<String> correctAnswer = new ArrayList<String>();
        correctAnswer.add("cat");
        correctAnswer.add("dog");
        correctAnswer.add("hailstorm");
        assertEquals(correctAnswer, something);
    }
    
    
    // test using a .aegis.xml
    @Test
    public void testSimpleBeanRead() throws Exception {
        context = new AegisContext();
        Set<java.lang.reflect.Type> rootClasses = new HashSet<java.lang.reflect.Type>();
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
