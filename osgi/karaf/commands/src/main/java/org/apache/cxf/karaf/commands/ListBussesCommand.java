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
import org.apache.cxf.karaf.commands.internal.CXFController;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.table.ShellTable;

/**
 *
 */
@Command(scope = "cxf", name = "list-busses", description = "Lists all CXF Busses.")
@Service
public class ListBussesCommand extends CXFController implements Action {

    @Reference(optional = true)
    Terminal terminal;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Override
    public Object execute() throws Exception {
        List<Bus> busses = getBusses();

        ShellTable table = new ShellTable();
        if (terminal != null && terminal.getWidth() > 0) {
            table.size(terminal.getWidth());
        }
        table.column("Name");
        table.column("State");

        for (Bus bus : busses) {
            String name = bus.getId();
            String state = bus.getState().toString();
            table.addRow().addContent(name, state);
        }
        table.print(System.out, !noFormat);
        return null;
    }

}
