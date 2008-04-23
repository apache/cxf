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

package org.apache.cxf.doclitbare;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.test.AbstractCXFTest;
import org.junit.Test;

/**
 * Test motivated by CXF-1504
 */
public class DocLitBareTest extends AbstractCXFTest {
    
    @Test
    public void testNamespaceCrash() {
        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setServiceClass(University.class);
        svrFactory.setAddress("local://dlbTest");
        svrFactory.setServiceBean(new UniversityImpl());
        svrFactory.getServiceFactory().setDataBinding(new AegisDatabinding());
        svrFactory.create(); 

        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
       
        factory.setServiceClass(University.class);
        factory.setAddress("local://dlbTest");
        University client = (University) factory.create();
       
        Teacher tr = client.getTeacher(new Course(40, "Intro to CS", "Introductory Comp Sci"));
        assertNotNull(tr);
        assertEquals(52, tr.getAge());
        assertEquals("Mr. Tom", tr.getName());
    }
}
