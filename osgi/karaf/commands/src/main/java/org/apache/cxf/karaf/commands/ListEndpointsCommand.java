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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.karaf.commands.completers.BusCompleter;
import org.apache.cxf.karaf.commands.internal.CXFController;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 *
 */
@Command(scope = "cxf", name = "list-endpoints",
    description = "Lists all CXF Endpoints on a Bus.")
@Service
public class ListEndpointsCommand extends CXFController implements Action {
    protected static final String HEADER_FORMAT = "%-25s %-10s %-60s %-40s";
    protected static final String OUTPUT_FORMAT = "[%-23s] [%-8s] [%-58s] [%-38s]";

    @Argument(index = 0, name = "bus",
        description = "The CXF bus name where to look for the Endpoints",
        required = false, multiValued = false)
    @Completion(BusCompleter.class)
    String name;

    @Option(name = "-f", aliases = {"--fulladdress" },
        description = "Display full address of an endpoint ", required = false, multiValued = false)
    boolean fullAddress;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Reference(optional = true)
    Terminal terminal;

    @Override
    public Object execute() throws Exception {
        List<Bus> busses;
        if (name == null) {
            busses = getBusses();
        } else {
            Bus b = getBus(name);
            if (b != null) {
                busses = Collections.singletonList(getBus(name));
            } else {
                busses = Collections.emptyList();
            }
        }

        ShellTable table = new ShellTable();
        if (terminal != null && terminal.getWidth() > 0) {
            table.size(terminal.getWidth());
        }
        table.column("Name");
        table.column("State");
        table.column("Address");
        table.column("BusID");
        for (Bus b : busses) {
            ServerRegistry reg = b.getExtension(ServerRegistry.class);
            List<Server> servers = reg.getServers();
            for (Server serv : servers) {
                String qname = serv.getEndpoint().getEndpointInfo().getName().getLocalPart();
                String started = serv.isStarted() ? "Started" : "Stopped";
                String address = serv.getEndpoint().getEndpointInfo().getAddress();
                if (fullAddress) {
                    address = toFullAddress(address);
                }
                String busId = b.getId();
                table.addRow().addContent(qname, started, address, busId);
            }
        }
        table.print(System.out, !noFormat);
        return null;
    }

    private String toFullAddress(String address) throws IOException, InvalidSyntaxException {
        ConfigurationAdmin configAdmin = getConfigAdmin();
        if (address.startsWith("/") && configAdmin != null) {
            String httpPort = extractConfigProperty(configAdmin, "org.ops4j.pax.web", "org.osgi.service.http.port");
            String cxfContext =
                extractConfigProperty(configAdmin, "org.apache.cxf.osgi", "org.apache.cxf.servlet.context");
            if (StringUtils.isEmpty(cxfContext)) {
                cxfContext = getCXFOSGiServletContext();
            }
            if (StringUtils.isEmpty(httpPort)) {
                httpPort = getHttpOSGiServicePort();
            }
            if (!StringUtils.isEmpty(httpPort) && !StringUtils.isEmpty(cxfContext)) {
                address = "http://localhost:" + httpPort + cxfContext + address;
            }
        }
        return address;
    }

    private String extractConfigProperty(ConfigurationAdmin configAdmin,
                                         String pid, String propertyName) throws IOException,
        InvalidSyntaxException {
        String ret = null;
        Configuration[] configs = configAdmin.listConfigurations("(service.pid=" + pid + ")");
        if (configs != null && configs.length > 0) {
            Configuration configuration = configs[0];
            if (configuration != null) {
                ret = (String)configuration.getProperties().get(propertyName);
            }
        }
        return ret;
    }

    private String getCXFOSGiServletContext() throws InvalidSyntaxException {
        String ret = null;
        String filter = "(&(" + "objectclass=" + "jakarta.servlet.Servlet"
            + ")(servlet-name=cxf-osgi-transport-servlet))";

        ServiceReference<?> ref = getBundleContext().getServiceReferences((String)null, filter)[0];

        if (ref != null) {
            ret = (String)ref.getProperty("alias");
        }

        return ret;

    }

    private String getHttpOSGiServicePort() throws InvalidSyntaxException {
        String ret = null;
        String filter = "(&(" + "objectclass=" + "org.osgi.service.http.HttpService"
                + "))";

        ServiceReference<?> ref = getBundleContext().getServiceReferences((String)null, filter)[0];

        if (ref != null) {
            ret = (String) ref.getProperty("org.osgi.service.http.port");
        }

        return ret;

    }

}
