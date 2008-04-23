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
package org.apache.cxf.jaxws;


import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Factory;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class JAXWSMethodInvokerTest {
    Factory factory = EasyMock.createMock(Factory.class);
    Object target = EasyMock.createMock(Hello.class);
        
    @Test
    public void testFactoryBeans() throws Throwable {
        Exchange ex = EasyMock.createMock(Exchange.class);               
        EasyMock.reset(factory);
        factory.create(ex);
        EasyMock.expectLastCall().andReturn(target);
        EasyMock.replay(factory);
        JAXWSMethodInvoker jaxwsMethodInvoker = new JAXWSMethodInvoker(factory);
        Object object = jaxwsMethodInvoker.getServiceObject(ex);
        Assert.assertEquals("the target object and service object should be equal ", object, target);
        EasyMock.verify(factory);
    }
        

}
