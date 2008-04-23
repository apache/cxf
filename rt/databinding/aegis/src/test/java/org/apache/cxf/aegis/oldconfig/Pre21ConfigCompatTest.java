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

package org.apache.cxf.aegis.oldconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.inheritance.ws1.WS1;
import org.apache.cxf.aegis.proxy.Hello;
import org.apache.cxf.aegis.proxy.MyHello;
import org.apache.cxf.aegis.services.SimpleBean;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.junit.Test;

/**
 * 
 */
public class Pre21ConfigCompatTest extends AbstractAegisTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testCompat() throws Exception {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AegisDatabinding.WRITE_XSI_TYPE_KEY, "true");
        props.put(AegisDatabinding.READ_XSI_TYPE_KEY, "false");
        List<String> l = new ArrayList<String>();
        l.add(SimpleBean.class.getName());
        props.put(AegisDatabinding.OVERRIDE_TYPES_KEY, l);
        props.put(Hello.class.getName() + ".implementation", MyHello.class.getName());
        
        ClientProxyFactoryBean pf = new ClientProxyFactoryBean();
        setupAegis(pf.getClientFactoryBean());
        pf.setServiceClass(WS1.class);
        pf.getServiceFactory().setProperties(props);
        pf.setAddress("local://WS1");
        pf.setProperties(props);
        ClientFactoryBean cfb = pf.getClientFactoryBean();
        cfb.create();

        AegisDatabinding db = (AegisDatabinding)cfb.getServiceFactory().getDataBinding();
        AegisContext context = db.getAegisContext();
        assertTrue(context.isWriteXsiTypes());
        assertFalse(context.isReadXsiTypes());
        Set<String> classes = context.getRootClassNames();
        assertTrue(classes.contains(SimpleBean.class.getName()));
        assertEquals(MyHello.class.getName(), context.getBeanImplementationMap().get(Hello.class)); 
    }
}
