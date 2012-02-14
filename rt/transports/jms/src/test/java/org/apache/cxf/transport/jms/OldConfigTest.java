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

package org.apache.cxf.transport.jms;

import junit.framework.Assert;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.junit.BeforeClass;
import org.junit.Test;


public class OldConfigTest extends AbstractJMSTester {

    @BeforeClass
    public static void createAndStartBroker() throws Exception {
        startBroker(new JMSBrokerSetup("tcp://localhost:" + JMS_PORT));
    }

    @Test
    public void testUsernameAndPassword() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                "HelloWorldService", "HelloWorldPort");
        JMSOldConfigHolder holder = new JMSOldConfigHolder();
        JMSEndpoint endpoint = holder.getExtensorsAndConfig(bus, endpointInfo, target, false);
        holder.configureEndpoint(false, endpoint);
        Assert.assertEquals("User name does not match." , "testUser", endpoint.getUsername());
        Assert.assertEquals("Password does not match." , "testPassword", endpoint.getPassword());
    }
}
