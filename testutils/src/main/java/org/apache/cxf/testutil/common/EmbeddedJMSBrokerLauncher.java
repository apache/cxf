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
package org.apache.cxf.testutil.common;

import java.io.File;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;

public class EmbeddedJMSBrokerLauncher extends AbstractBusTestServerBase {
    
    BrokerService broker;
    final String brokerUrl1 = "tcp://localhost:61500";            
            
    public void tearDown() throws Exception {
        if (broker != null) {
            broker.stop();
        }
    }
            
    public void run() {
        try {                
            broker = new BrokerService();
            broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
            broker.setTmpDataDirectory(new File("./target"));
            broker.addConnector(brokerUrl1);
            broker.start();            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            EmbeddedJMSBrokerLauncher s = new EmbeddedJMSBrokerLauncher();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
