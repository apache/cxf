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
package org.apache.cxf.management.jmx;

import javax.management.remote.JMXServiceURL;

import org.junit.Assert;
import org.junit.Test;


public class MBServerConnectorFactoryTest {

    @Test
    public void testGetServerPort() throws Exception {
        Assert.assertEquals(9914, MBServerConnectorFactory.getServerPort(
                "service:jmx:rmi:///jndi/rmi://localhost:9914/jmxrmi"));

        Assert.assertEquals(10002, MBServerConnectorFactory.getServerPort(
                "service:jmx:rmi://localhost:10002/jndi/rmi://localhost:10001/jmxrmi"));
    }

    @Test
    public void testGetRegistryPort() throws Exception {
        Assert.assertEquals(9914, MBServerConnectorFactory.getRegistryPort(
                "service:jmx:rmi:///jndi/rmi://localhost:9914/jmxrmi"));

        Assert.assertEquals(10001, MBServerConnectorFactory.getRegistryPort(
                        "service:jmx:rmi://localhost:10002/jndi/rmi://localhost:10001/jmxrmi"));
    }

    @Test
    public void testGetBindingName() throws Exception {
        Assert.assertEquals("jmxrmi", MBServerConnectorFactory.getBindingName(
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi")));

        Assert.assertEquals("cxf-jmxrmi", MBServerConnectorFactory.getBindingName(
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9913/cxf-jmxrmi")));
    }
}