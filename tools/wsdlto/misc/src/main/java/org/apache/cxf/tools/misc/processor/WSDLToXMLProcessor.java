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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Input;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Port;
import javax.wsdl.PortType;
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

public class WSDLToXMLProcessor extends AbstractWSDLToProcessor {

    private static final String NEW_FILE_NAME_MODIFIER = "-xmlbinding";

    private Map services;
    private Service service;
    private Map ports;
    private Port port;

    private Map portTypes;
    private PortType portType;
    private Binding binding;

    public void process() throws ToolException {
        init();
        if (isBindingExisted()) {
            Message msg = new Message("BINDING_ALREADY_EXIST", LOG);
            throw new ToolException(msg);
        }
        if (!isPortTypeExisted()) {
            Message msg = new Message("PORTTYPE_NOT_EXIST", LOG);
            throw new ToolException(msg);
        }
        if (isServicePortExisted()) {
            Message msg = new Message("SERVICE_PORT_EXIST", LOG);
            throw new ToolException(msg);
        }
        doAppendBinding();
        doAppendService();
        writeToWSDL();
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

    private boolean isPortTypeExisted() {
        portTypes = wsdlDefinition.getPortTypes();
        if (portTypes == null) {
            return false;
        }
        Iterator it = portTypes.keySet().iterator();
        while (it.hasNext()) {
            QName existPortQName = (QName)it.next();
            String existPortName = existPortQName.getLocalPart();
            if (existPortName.equals(env.get(ToolConstants.CFG_PORTTYPE))) {
                portType = (PortType)portTypes.get(existPortQName);
                break;
            }
        }
        return (portType == null) ? false : true;
    }

    private boolean isBindingExisted() {
        Map bindings = wsdlDefinition.getBindings();
        if (bindings == null) {
            return false;
        }
        Iterator it = bindings.keySet().iterator();
        while (it.hasNext()) {
            QName existBindingQName = (QName)it.next();
            String existBindingName = existBindingQName.getLocalPart();
            String bindingName = (String)env.get(ToolConstants.CFG_BINDING);
            if (bindingName.equals(existBindingName)) {
                binding = (Binding)bindings.get(existBindingQName);
            }
        }
        return (binding == null) ? false : true;
    }

    protected void init() throws ToolException {
        parseWSDL((String)env.get(ToolConstants.CFG_WSDLURL));
        if (wsdlDefinition.getNamespace(ToolConstants.XML_FORMAT_PREFIX) == null) {
            wsdlDefinition.addNamespace(ToolConstants.XML_FORMAT_PREFIX, ToolConstants.NS_XML_FORMAT);
        }
        if (wsdlDefinition.getNamespace(ToolConstants.XML_HTTP_PREFIX) == null) {
            wsdlDefinition.addNamespace(ToolConstants.XML_HTTP_PREFIX, ToolConstants.NS_XML_HTTP);
        }
    }

    private void doAppendBinding() throws ToolException {
        if (binding == null) {
            binding = wsdlDefinition.createBinding();
            binding.setQName(new QName(wsdlDefinition.getTargetNamespace(), (String)env
                .get(ToolConstants.CFG_BINDING)));
            binding.setUndefined(false);
            binding.setPortType(portType);
        }
        setXMLBindingExtElement();
        addBindingOperation();
        wsdlDefinition.addBinding(binding);
    }

    private void setXMLBindingExtElement() throws ToolException {
        if (extReg == null) {
            extReg = wsdlFactory.newPopulatedExtensionRegistry();
        }
        try {
            binding.addExtensibilityElement(getWSDLPlugin("xml", Binding.class).createExtension(null));
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_CREATE_XMLBINDING", LOG);
            throw new ToolException(msg);
        }
    }

    @SuppressWarnings("unchecked")
    private void addBindingOperation() throws ToolException {
        List<Operation> ops = portType.getOperations();
        for (Operation op : ops) {
            BindingOperation bindingOperation = wsdlDefinition.createBindingOperation();
            bindingOperation.setName(op.getName());
            if (op.getInput() != null) {
                bindingOperation.setBindingInput(getBindingInput(op.getInput(), op.getName()));
            }
            if (op.getOutput() != null) {
                bindingOperation.setBindingOutput(getBindingOutput(op.getOutput(), op.getName()));
            }
            if (op.getFaults() != null && op.getFaults().size() > 0) {
                addXMLFaults(op, bindingOperation);
            }
            bindingOperation.setOperation(op);
            binding.addBindingOperation(bindingOperation);
        }
    }

    private BindingInput getBindingInput(Input input, String operationName) throws ToolException {
        BindingInput bi = wsdlDefinition.createBindingInput();
        bi.setName(input.getName());
        //This ext element in some scenario is optional, but if provided, won't cause error
        bi.addExtensibilityElement(getXMLBody(BindingInput.class, operationName));
        return bi;
    }

    private BindingOutput getBindingOutput(Output output, String operationName) throws ToolException {
        BindingOutput bo = wsdlDefinition.createBindingOutput();
        bo.setName(output.getName());
        bo.addExtensibilityElement(getXMLBody(BindingOutput.class, operationName));
        return bo;
    }

    private void addXMLFaults(Operation op, BindingOperation bo) {
        // TODO
    }

    private ExtensibilityElement getXMLBody(Class clz, String operationName) throws ToolException {
        if (extReg == null) {
            extReg = wsdlFactory.newPopulatedExtensionRegistry();
        }
        Map<String, Object> args = new HashMap<String, Object>();
        args.put(QName.class.getName(), new QName(wsdlDefinition.getTargetNamespace(), operationName));
        args.put(Class.class.getName(), clz);

        try {
            return getWSDLPlugin("xml", clz).createExtension(args);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_CREATE_XMLBINDING", LOG);
            throw new ToolException(msg);
        }
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
    }

    private void setAddrElement() throws ToolException {
        Address address = AddressFactory.getInstance().getAddresser("xml");

        for (String key : address.getNamespaces(env).keySet()) {
            wsdlDefinition.addNamespace(key, address.getNamespaces(env).get(key));
        }

        WSDLExtensibilityPlugin generator = getWSDLPlugin("xml", Port.class);
        try {
            ExtensibilityElement extElement = generator.createExtension(address.buildAddressArguments(env));

            port.addExtensibilityElement(extElement);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_CREATE_SOAPADDRESS", LOG);
            throw new ToolException(msg);
        }
    }

    private void writeToWSDL() throws ToolException {
        WSDLWriter wsdlWriter = wsdlFactory.newWSDLWriter();
        Writer outputWriter = getOutputWriter(NEW_FILE_NAME_MODIFIER);
        try {
            wsdlWriter.writeWSDL(wsdlDefinition, outputWriter);
        } catch (WSDLException wse) {
            Message msg = new Message("FAIL_TO_WRITE_WSDL", LOG);
            throw new ToolException(msg);
        }
        try {
            outputWriter.close();
        } catch (IOException ioe) {
            Message msg = new Message("FAIL_TO_CLOSE_WSDL_FILE", LOG);
            throw new ToolException(msg);
        }
    }

}
