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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.aegis.services.SimpleBean;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class StandaloneReadTest {
    private final AegisContext context = new AegisContext();

    private interface ListStringInterface {
        List<String> method();
    }

    @Test
    public void testBasicTypeRead() throws Exception {
        context.initialize();
        XMLStreamReader streamReader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("stringElement.xml"));
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        Object something = reader.read(streamReader);
        assertEquals("ball-of-yarn", something);
    }

    @Test
    public void testCollectionReadNoXsiType() throws Exception {
        java.lang.reflect.Type listStringType
            = ListStringInterface.class.getMethods()[0].getGenericReturnType();
        context.setRootClasses(Collections.singleton(listStringType));
        context.initialize();
        XMLStreamReader streamReader
            = StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("topLevelList.xml"));
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        // until I fix type mapping to use java.lang.reflect.Type instead of
        // Class, I need to do the following
        QName magicTypeQName = new QName("urn:org.apache.cxf.aegis.types", "ArrayOfString");
        AegisType aegisRegisteredType = context.getTypeMapping().getType(magicTypeQName);

        Object something = reader.read(streamReader, aegisRegisteredType);
        List<String> correctAnswer = Arrays.asList(
            "cat",
            "dog",
            "hailstorm");
        assertEquals(correctAnswer, something);
    }

    @Test
    public void testCollectionReadXsiType() throws Exception {
        java.lang.reflect.Type listStringType
            = ListStringInterface.class.getMethods()[0].getGenericReturnType();
        context.setRootClasses(Collections.singleton(listStringType));
        context.initialize();
        XMLStreamReader streamReader
            = StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("topLevelListWithXsiType.xml"));
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();

        Object something = reader.read(streamReader);
        List<String> correctAnswer = Arrays.asList(
            "cat",
            "dog",
            "hailstorm");
        assertEquals(correctAnswer, something);
    }


    // test using a .aegis.xml
    @Test
    public void testSimpleBeanRead() throws Exception {
        context.setRootClasses(Collections.singleton(SimpleBean.class));
        context.initialize();
        XMLStreamReader streamReader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("simpleBean1.xml"));
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        Object something = reader.read(streamReader);
        assertTrue(something instanceof SimpleBean);
        SimpleBean simpleBean = (SimpleBean) something;
        assertEquals("howdy", simpleBean.getHowdy());
    }

}
