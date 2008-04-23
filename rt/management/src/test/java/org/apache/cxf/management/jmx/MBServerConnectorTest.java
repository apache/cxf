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



import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.junit.Assert;
import org.junit.Test;



public class MBServerConnectorTest extends Assert {
    
    @Test
    public void testMBServerConnector() {
        MBServerConnectorFactory mcf;    
        MBeanServer mbs;        
        mbs = MBeanServerFactory.createMBeanServer("test");            
        mcf = MBServerConnectorFactory.getInstance();
        mcf.setMBeanServer(mbs);
        mcf.setThreaded(true);
        mcf.setDaemon(true);
        mcf.setServiceUrl("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi");
        try {
            mcf.createConnector(); 
            Thread.sleep(1000);           
            mcf.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
            assertFalse("Some Exception happen to MBServerConnectorTest", true);
        }
    }

}
