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
package org.apache.cxf.transport.http_jetty.spring;


import org.w3c.dom.Document;


import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transport.http.spring.HttpConduitBeanDefinitionParser;
import org.apache.cxf.transport.http.spring.HttpDestinationBeanDefinitionParser;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

@org.junit.Ignore("Not valid as the parser shouldn't be generating the beans themselves, thats the" 
                  + "job of the factory later on.  Need to update this test to do that.")
public class BeanDefinitionParsersTest extends Assert {
    @Test
    public void testDest()throws Exception {
        BeanDefinitionBuilder bd = BeanDefinitionBuilder.childBeanDefinition("child");
        
        HttpDestinationBeanDefinitionParser parser = new HttpDestinationBeanDefinitionParser();

        Document d = DOMUtils.readXml(getClass().getResourceAsStream("destination.xml"));
        parser.doParse(d.getDocumentElement(), null, bd);
        
        PropertyValue[] pvs = bd.getRawBeanDefinition().getPropertyValues().getPropertyValues();
        assertEquals(2, pvs.length);
        assertEquals("foobar", ((HTTPServerPolicy) pvs[0].getValue()).getContentEncoding());
        assertEquals("exact", pvs[1].getValue());
    }
    
    @Test
    public void testConduit()throws Exception {
        BeanDefinitionBuilder bd = BeanDefinitionBuilder.childBeanDefinition("child");
        
        HttpConduitBeanDefinitionParser parser = new HttpConduitBeanDefinitionParser();

        Document d = DOMUtils.readXml(getClass().getResourceAsStream("conduit.xml"));
        parser.doParse(d.getDocumentElement(), null, bd);
        
        PropertyValue[] pvs = bd.getRawBeanDefinition().getPropertyValues().getPropertyValues();
        assertEquals(1, pvs.length);
        assertEquals(97, ((HTTPClientPolicy) pvs[0].getValue()).getConnectionTimeout(), 0);
    }
    
    
}
