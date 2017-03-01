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

import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.karaf.commands.completers.BusCompleter;
import org.apache.cxf.karaf.commands.completers.StoppedEndpointCompleter;
import org.apache.cxf.karaf.commands.internal.CXFController;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 *
 */
@Command(scope = "cxf", name = "start-endpoint",
    description = "Starts a CXF Endpoint on a Bus.")
@Service
public class StartEndpointCommand extends CXFController implements Action {

    @Argument(index = 0, name = "bus",
        description = "The CXF bus name where to look for the Endpoint",
        required = true, multiValued = false)
    @Completion(BusCompleter.class)
    String busName;

    @Argument(index = 1, name = "endpoint",
        description = "The Endpoint name to start",
        required = true, multiValued = false)
    @Completion(StoppedEndpointCompleter.class)
    String endpoint;

    @Override
    public Object execute() throws Exception {
        Bus b = getBus(busName);
        ServerRegistry reg = b.getExtension(ServerRegistry.class);
        List<Server> servers = reg.getServers();
        for (Server serv : servers) {
            if (endpoint.equals(serv.getEndpoint().getEndpointInfo().getName().getLocalPart())) {
                serv.start();
            }
        }
        return null;
    }
}
