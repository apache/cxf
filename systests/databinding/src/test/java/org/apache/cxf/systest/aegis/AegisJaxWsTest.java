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

package org.apache.cxf.systest.aegis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.systest.aegis.bean.Item;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * 
 */
@ContextConfiguration(locations = { "classpath:aegisJaxWsBeans.xml" })
public class AegisJaxWsTest extends AbstractJUnit4SpringContextTests {
    
    private AegisJaxWs client;
    
    public AegisJaxWsTest() {
    }
    
    private void setupForTest(boolean sec) throws Exception {
        
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(AegisJaxWs.class);
        if (sec) {
            factory.setAddress("http://localhost:9167/aegisJaxWsUN");
            WSS4JOutInterceptor wss4jOut = new WSS4JOutInterceptor();
            wss4jOut.setProperty("action", "UsernameToken");
            wss4jOut.setProperty("user", "alice");
            wss4jOut.setProperty("password", "pass");
            
            factory.setProperties(new HashMap<String, Object>());
            factory.getProperties().put("password", "pass");
            factory.getOutInterceptors().add(wss4jOut);
        } else {
            factory.setAddress("http://localhost:9167/aegisJaxWs");            
        }
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());

        client = (AegisJaxWs)factory.create();
    }
    
    @Test
    public void testGetItemSecure() throws Exception {
        setupForTest(true);
        Item item = client.getItemByKey("   jack&jill   ", "b");
        Assert.assertEquals(33, item.getKey().intValue());
        Assert.assertEquals("   jack&jill   :b", item.getData());
    }
    
    @Test
    public void testGetItem() throws Exception {
        setupForTest(false);
        Item item = client.getItemByKey(" a ", "b");
        Assert.assertEquals(33, item.getKey().intValue());
        Assert.assertEquals(" a :b", item.getData());
    }
    @Test 
    public void testMapSpecified() throws Exception {
        setupForTest(false);
        Item item = new Item();
        item.setKey(new Integer(42));
        item.setData("Godzilla");
        client.addItem(item);
        
        Map<Integer, Item> items = client.getItemsMapSpecified();
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Map.Entry<Integer, Item> entry = items.entrySet().iterator().next();
        Assert.assertNotNull(entry);
        Item item2 = entry.getValue();
        Integer key2 = entry.getKey();
        Assert.assertEquals(42, key2.intValue());
        Assert.assertEquals("Godzilla", item2.getData());
    }
    @Test
    public void testGetStringList() throws Exception {
        setupForTest(false);

        Integer soucet = client.getSimpleValue(5, "aa");
        //this one fail, when comment org.apache.cxf.systest.aegis.AegisJaxWs.getStringList test pass
        Assert.assertEquals(new Integer(5), soucet);

        List<String> item = client.getStringList();
        Assert.assertEquals(Arrays.asList("a", "b", "c"), item);
    }
}
