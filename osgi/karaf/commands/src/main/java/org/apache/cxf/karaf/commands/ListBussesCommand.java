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
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * 
 */
@Command(scope = "cxf", name = "list-busses", description = "Lists all CXF Busses.")
public class ListBussesCommand extends OsgiCommandSupport {
    protected static final int DEFAULT_BUSID_LENGTH = 38;
    protected String headerFormat = "%-40s %-20s";
    protected String outputFormat = "[%-38s] [%-18s]";
   
    private CXFController cxfController;

    public void setController(CXFController controller) {
        this.cxfController = controller;
    }

    protected Object doExecute() throws Exception {
        List<Bus> busses = cxfController.getBusses();
        renderFormat(busses);
        System.out.println(String.format(headerFormat, "Name", "State"));

        for (Bus bus : busses) {
            String state = bus.getState().toString();
            System.out.println(String.format(outputFormat, bus.getId(), state));
        }
        return null;
    }

    private void renderFormat(List<Bus> busses) {
        int longestBusId = DEFAULT_BUSID_LENGTH;
        for (Bus bus : busses) {
            if (bus.getId().length() > longestBusId) {
                longestBusId = bus.getId().length();
            }
        }
        if (longestBusId > DEFAULT_BUSID_LENGTH) {
            headerFormat = "%-" + (longestBusId + 2) + "s %-20s";
            outputFormat = "[%-" + longestBusId + "s] [%-18s]";
        }
    }
}
