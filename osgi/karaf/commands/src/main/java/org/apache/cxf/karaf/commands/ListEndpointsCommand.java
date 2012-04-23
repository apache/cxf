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

package org.apache.cxf.karaf.commands;

import java.util.Collections;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * 
 */
@Command(scope = "cxf", name = "list-endpoints", 
    description = "Lists all CXF Endpoints on a Bus.")
public class ListEndpointsCommand extends OsgiCommandSupport {
    protected static final String HEADER_FORMAT = "%-25s %-10s %-60s %-40s";
    protected static final String OUTPUT_FORMAT = "[%-23s] [%-8s] [%-58s] [%-38s]";
    
    @Argument(index = 0, name = "bus", 
        description = "The CXF bus name where to look for the Endpoints", 
        required = false, multiValued = false)
    String name;
    
    private CXFController cxfController;

    public void setController(CXFController controller) {
        this.cxfController = controller;
    }

    protected Object doExecute() throws Exception {
        List<Bus> busses;
        if (name == null) {
            busses = cxfController.getBusses();
        } else {
            Bus b = cxfController.getBus(name);
            if (b != null) {
                busses = Collections.singletonList(cxfController.getBus(name));
            } else {
                busses = Collections.emptyList();
            }
        }
        for (Bus b : busses) {
            ServerRegistry reg = b.getExtension(ServerRegistry.class);
            List<Server> servers = reg.getServers();
            System.out.println(String.format(HEADER_FORMAT, 
                                             "Name", "State", "Address", "BusID"));
            for (Server serv : servers) {
                String qname = serv.getEndpoint().getEndpointInfo().getName().getLocalPart();
                String started = serv.isStarted() ? "Started" : "Stopped";
                String address = serv.getEndpoint().getEndpointInfo().getAddress();
                String busId = b.getId();
                System.out.println(String.format(OUTPUT_FORMAT, qname, started, address, busId));
            }
        }
        return null;
    } 
}
