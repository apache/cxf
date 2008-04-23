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

import com.jprofiler.api.agent.Controller;

import java.io.File;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;

import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

/**
 * 
 */
public final class ClientEndpointCreationLoop {
    
    private final QName portName = new QName("http://apache.org/hello_world_soap_http",
                                             "SoapPort");

    private ClientEndpointCreationLoop() {
    }
    
    private void iteration() throws URISyntaxException {    
        SOAPService service = new SOAPService();
        service.getPort(portName, Greeter.class);
    }
    /**
     * @param args
     * @throws URISyntaxException 
     */
    public static void main(String[] args) throws URISyntaxException {
        Controller.stopAllocRecording();
        Controller.stopCPURecording();
        ClientEndpointCreationLoop ecl = new ClientEndpointCreationLoop();
        ecl.iteration();  // warm up.
        Controller.startCPURecording(true);
        Controller.startAllocRecording(true);
        int count = Integer.parseInt(args[0]);
        for (int x = 0; x < count; x++) {
            ecl.iteration();
        }
        Controller.stopAllocRecording();
        Controller.stopCPURecording();
        if (args.length > 1) {
            Controller.saveSnapshot(new File(args[1]));
        }
    }
}
