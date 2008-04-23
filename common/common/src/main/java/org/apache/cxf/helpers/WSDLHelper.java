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

package org.apache.cxf.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

public class WSDLHelper {

    public BindingOperation getBindingOperation(Definition def, String operationName) {
        if (operationName == null) {
            return null;
        }
        Iterator ite = def.getBindings().values().iterator();
        while (ite.hasNext()) {
            Binding binding = (Binding)ite.next();
            Iterator ite1 = binding.getBindingOperations().iterator();
            while (ite1.hasNext()) {
                BindingOperation bop = (BindingOperation)ite1.next();
                if (bop.getName().equals(operationName)) {
                    return bop;
                }
            }
        }
        return null;
    }

    public BindingOperation getBindingOperation(Binding binding, String operationName) {
        if (operationName == null) {
            return null;
        }
        List bindingOperations = binding.getBindingOperations();
        for (Iterator iter = bindingOperations.iterator(); iter.hasNext();) {
            BindingOperation bindingOperation = (BindingOperation)iter.next();
            if (operationName.equals(bindingOperation.getName())) {
                return bindingOperation;
            }
        }
        return null;
    }

    public Map getParts(Operation operation, boolean out) {
        Message message = null;
        if (out) {
            Output output = operation.getOutput();
            message = output.getMessage();
        } else {
            Input input = operation.getInput();
            message = input.getMessage();
        }
        return message.getParts() == null ? new HashMap() : message.getParts();
    }

    public List<PortType> getPortTypes(Definition def) {
        List<PortType> portTypes = new ArrayList<PortType>();
        Iterator ite = def.getPortTypes().values().iterator();
        while (ite.hasNext()) {
            PortType portType = (PortType)ite.next();
            portTypes.add(portType);
        }
        return portTypes;
    }

    public List<Part> getInMessageParts(Operation operation) {
        Input input = operation.getInput();
        List<Part> partsList = new ArrayList<Part>();
        if (input != null) {
            Iterator ite = input.getMessage().getParts().values().iterator();
            while (ite.hasNext()) {
                partsList.add((Part)ite.next());
            }
        }
        return partsList;
    }

    public List<Part> getOutMessageParts(Operation operation) {
        Output output = operation.getOutput();
        List<Part> partsList = new ArrayList<Part>();
        if (output != null) {
            Iterator ite = output.getMessage().getParts().values().iterator();
            while (ite.hasNext()) {
                partsList.add((Part)ite.next());
            }
        }
        return partsList;
    }

    public Binding getBinding(BindingOperation bop, Definition def) {
        Iterator ite = def.getBindings().values().iterator();
        while (ite.hasNext()) {
            Binding binding = (Binding)ite.next();
            for (Iterator ite2 = binding.getBindingOperations().iterator(); ite2.hasNext();) {
                BindingOperation bindingOperation = (BindingOperation)ite2.next();
                if (bindingOperation.getName().equals(bop.getName())) {
                    return binding;
                }
            }
        }
        return null;
    }

    public Definition getDefinition(File wsdlFile) throws Exception {
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        WSDLReader reader = wsdlFactory.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        return reader.readWSDL(wsdlFile.toURI().toURL().toString());
    }
}
