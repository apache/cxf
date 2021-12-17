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
package org.apache.cxf.systest.jms.tx;

import jakarta.jws.WebService;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;

import org.junit.Assert;

/**
 * Service that throws an exception when called by BAD_GUY
 */
@WebService(endpointInterface = "org.apache.hello_world_doc_lit.Greeter")
public class GreeterImplWithTransaction implements Greeter {
    public static final String BAD_GUY = "Bad guy";
    public static final String GOOD_GUY = "Good guy";

    public String greetMe(String name) {
        if (BAD_GUY.equals(name)) {
            throw new RuntimeException("Got a bad guy call for greetMe");
        }
        return "Hello " + name;
    }

    @Override
    public void greetMeOneWay(String name) {
        ConfiguredBeanLocator locator = BusFactory.getDefaultBus().getExtension(ConfiguredBeanLocator.class);
        TransactionManager tm = locator.getBeansOfType(TransactionManager.class).iterator().next();
        try {
            Assert.assertNotNull("We should run inside a transaction", tm.getTransaction());
        } catch (SystemException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (BAD_GUY.equals(name)) {
            throw new RuntimeException("Got a bad guy call for greetMe");
        }
    }

    @Override
    public void pingMe() throws PingMeFault {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public String sayHi() {
        throw new IllegalArgumentException("Not implemented");
    }

}
