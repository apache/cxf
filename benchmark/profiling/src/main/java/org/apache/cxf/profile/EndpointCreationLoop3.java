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

package org.apache.cxf.profile;

import java.io.File;

import com.jprofiler.api.agent.Controller;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

/**
 * 
 */
public final class EndpointCreationLoop3 {
    
    
    private EndpointCreationLoop3() {
    }
    
    private void iteration() {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setAddress("http://localhost:9000/test");
        sf.setServiceClass(org.apache.cxf.systest.jaxb.service.TestServiceImpl.class);
        sf.setStart(false);
        
        Server server = sf.create();
        server.start();
        server.stop();
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        Controller.stopAllocRecording();
        Controller.stopCPURecording();
        EndpointCreationLoop3 ecl = new EndpointCreationLoop3();
        int count = Integer.parseInt(args[0]);
        ecl.iteration();
        
        Controller.startCPURecording(true);
        Controller.startAllocRecording(true);
        for (int x = 0; x < count; x++) {
            ecl.iteration();
        }
        Controller.stopAllocRecording();
        Controller.stopCPURecording();
        if (args.length > 1) {
            Controller.saveSnapshot(new File(args[1]));
        }
        System.exit(0);
    }
}
