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

package org.apache.cxf.ws.policy.builder.jaxb;

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.test.assertions.foo.FooType;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class JaxbAssertionBuilderTest extends Assert {
    
    @Test
    public void testConstructors() throws Exception {        
        QName qn = new QName("http://cxf.apache.org/test/assertions/foo", "FooType");
        try {
            new JaxbAssertionBuilder("org.apache.cxf.test.assertions.foo.UnknownType", qn);
            fail("Expected ClassNotFoundException not thrown.");
        } catch (ClassNotFoundException ex) {
            // expected
        }
        assertNotNull(new JaxbAssertionBuilder(qn)); 
        assertNotNull(new JaxbAssertionBuilder(FooType.class.getName(), qn));   
        assertNotNull(new JaxbAssertionBuilder<FooType>(FooType.class, qn));
    }
    
    @Test
    public void testGetKnownElements() throws Exception {
        QName qn = new QName("http://cxf.apache.org/test/assertions/foo", "FooType");
        JaxbAssertionBuilder<FooType> ab = new JaxbAssertionBuilder<FooType>(FooType.class, qn);
        assertNotNull(ab);
        assertEquals(1, ab.getKnownElements().size());
        assertSame(qn, ab.getKnownElements().iterator().next());
    }
    
    @Test
    public void testBuild() throws Exception {
        QName qn = new QName("http://cxf.apache.org/test/assertions/foo", "FooType");
        JaxbAssertionBuilder<FooType> ab = new JaxbAssertionBuilder<FooType>(FooType.class, qn);
        assertNotNull(ab);
        InputStream is = JaxbAssertionBuilderTest.class.getResourceAsStream("foo.xml");
        Document doc = DOMUtils.readXml(is);        
        Element elem =  DOMUtils.findAllElementsByTagNameNS((Element)doc.getDocumentElement(), 
                                                          "http://cxf.apache.org/test/assertions/foo", 
                                                          "foo").get(0);
        PolicyAssertion a = ab.build(elem);
        JaxbAssertion<FooType> jba = JaxbAssertion.cast(a, FooType.class);
        FooType foo = jba.getData();
        assertEquals("CXF", foo.getName());
        assertEquals(2, foo.getNumber().intValue());         
    }
}
