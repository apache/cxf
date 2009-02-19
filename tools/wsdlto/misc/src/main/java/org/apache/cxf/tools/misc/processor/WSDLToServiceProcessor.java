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

package org.apache.cxf.tools.misc.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import javax.wsdl.Binding;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.misc.processor.address.Address;
import org.apache.cxf.tools.misc.processor.address.AddressFactory;
import org.apache.cxf.wsdl.WSDLExtensibilityPlugin;

public class WSDLToServiceProcessor extends AbstractWSDLToProcessor {

    private static final String NEW_FILE_NAME_MODIFIER = "-service";

    private Map services;
    private Service service;
    private Map ports;
    private Port port;
    private Binding binding;

    public void process() throws ToolException {
        init();
        if (isServicePortExisted()) {
            Message msg = new Message("SERVICE_PORT_EXIST", LOG);
            throw new ToolException(msg);
        }
        if (!isBindingExisted()) {
            Message msg = new Message("BINDING_NOT_EXIST", LOG);
            throw new ToolException(msg);
        }
        doAppendService();
    }

    private boolean isServicePortExisted() {
        return isServiceExisted() && isPortExisted();
    }

    private boolean isServiceExisted() {
        services = wsdlDefinition.getServices();
        if (services == null) {
            return false;
        }
        Iterator it = services.keySet().iterator();
        while (it.hasNext()) {
            QName serviceQName = (QName)it.next();
            String serviceName = serviceQName.getLocalPart();
            if (serviceName.equals(env.get(ToolConstants.CFG_SERVICE))) {
                service = (Service)services.get(serviceQName);
                break;
            }
        }
        return (service == null) ? false : true;
    }

    private boolean isPortExisted() {
        ports = service.getPorts();
        if (ports == null) {
            return false;
        }
        Iterator it = ports.keySet().iterator();
        while (it.hasNext()) {
            String portName = (String)it.next();
            if (portName.equals(env.get(ToolConstants.CFG_PORT))) {
                port = (Port)ports.get(portName);
                break;
            }
        }
        return (port == null) ? false : true;
    }

    private boolean isBindingExisted() {
        Map bindings = wsdlDefinition.getBindings();
        if (bindings == null) {
            return false;
        }
        Iterator it = bindings.keySet().iterator();
        while (it.hasNext()) {
            QName bindingQName = (QName)it.next();
            String bindingName = bindingQName.getLocalPart();
            String attrBinding = (String)env.get(ToolConstants.CFG_BINDING_ATTR);
            if (attrBinding.equals(bindingName)) {
                binding = (Binding)bindings.get(bindingQName);
            }
        }
        return (binding == null) ? false : true;
    }

    protected void init() throws ToolException {
        parseWSDL((String)env.get(ToolConstants.CFG_WSDLURL));
    }

    private void doAppendService() throws ToolException {
        if (service == null) {
            service = wsdlDefinition.createService();
            service
                .setQName(new QName(WSDLConstants.WSDL_PREFIX, (String)env.get(ToolConstants.CFG_SERVICE)));
        }
        if (port == null) {
            port = wsdlDefinition.createPort();
            port.setName((String)env.get(ToolConstants.CFG_PORT));
            port.setBinding(binding);
        }
        setAddrElement();
        service.addPort(port);
        wsdlDefinition.addService(service);

        WSDLWriter wsdlWriter = wsdlFactory.newWSDLWriter();
        Writer outputWriter = getOutputWriter(NEW_FILE_NAME_MODIFIER);
        try {
            wsdlWriter.writeWSDL(wsdlDefinition, outputWriter);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_WRITE_WSDL", LOG);
            throw new ToolException(msg, wse);
        }
        try {
            outputWriter.close();
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_CLOSE_WSDL_FILE", LOG);
            throw new ToolException(msg, ioe);
        }

    }

    private void setAddrElement() throws ToolException {
        String transport = (String)env.get(ToolConstants.CFG_TRANSPORT);
        Address address = AddressFactory.getInstance().getAddresser(transport);

        Map<String, String> ns = address.getNamespaces(env);
        for (String key : ns.keySet()) {
            wsdlDefinition.addNamespace(key, ns.get(key));
        }

        WSDLExtensibilityPlugin plugin = getWSDLPlugin(transport, Port.class);
        try {
            ExtensibilityElement extElement = plugin.createExtension(address.buildAddressArguments(env));
            port.addExtensibilityElement(extElement);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_CREATE_SOAP_ADDRESS", LOG);
            throw new ToolException(msg, wse);
        }
    }
}
