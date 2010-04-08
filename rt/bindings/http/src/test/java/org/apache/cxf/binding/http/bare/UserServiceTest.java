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
package org.apache.cxf.binding.http.bare;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.http.AbstractRestTest;
import org.apache.cxf.binding.http.HttpBindingFactory;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.Test;

public class UserServiceTest extends AbstractRestTest {
    
    @Test
    public void testInterfaceImplCombo() throws Exception {
        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
        HttpBindingFactory factory = new HttpBindingFactory();
        factory.setBus(getBus());
        bfm.registerBindingFactory(HttpBindingFactory.HTTP_BINDING_ID, factory);
        
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:9001/foo/");
        sf.setServiceBean(new UserServiceImpl());
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("contextMatchStrategy", "stem");
        sf.setProperties(props);
        
        ServerImpl svr = (ServerImpl) sf.create();
        
        Document res = get("http://localhost:9001/foo/login/bleh/bar");
        assertNotNull(res);

        addNamespace("c", "http://bare.http.binding.cxf.apache.org/");
        assertValid("/c:loginResponse/return", res);

        svr.destroy();
    }

}
