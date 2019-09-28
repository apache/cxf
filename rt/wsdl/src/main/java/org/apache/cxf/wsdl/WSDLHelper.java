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

package org.apache.cxf.wsdl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;

public class WSDLHelper {

    public BindingOperation getBindingOperation(Definition def, String operationName) {
        if (operationName == null) {
            return null;
        }
        Iterator<Binding> ite = CastUtils.cast(def.getBindings().values().iterator());
        while (ite.hasNext()) {
            Binding binding = ite.next();
            Iterator<BindingOperation> ite1
                = CastUtils.cast(binding.getBindingOperations().iterator());
            while (ite1.hasNext()) {
                BindingOperation bop = ite1.next();
                if (bop.getName().equals(operationName)) {
                    return bop;
                }
            }
        }
        return null;
    }

    public static String writeQName(Definition def, QName qname) {
        return def.getPrefix(qname.getNamespaceURI()) + ":" + qname.getLocalPart();
    }

    public BindingOperation getBindingOperation(Binding binding, String operationName) {
        if (operationName == null) {
            return null;
        }
        List<BindingOperation> bindingOperations = CastUtils.cast(binding.getBindingOperations());
        for (BindingOperation bindingOperation : bindingOperations) {
            if (operationName.equals(bindingOperation.getName())) {
                return bindingOperation;
            }
        }
        return null;
    }

    public List<PortType> getPortTypes(Definition def) {
        List<PortType> portTypes = new ArrayList<>();
        Collection<PortType> ite = CastUtils.cast(def.getPortTypes().values());
        for (PortType portType : ite) {
            portTypes.add(portType);
        }
        return portTypes;
    }

    public List<Part> getInMessageParts(Operation operation) {
        Input input = operation.getInput();
        List<Part> partsList = new ArrayList<>();
        if (input != null && input.getMessage() != null) {
            Collection<Part> parts = CastUtils.cast(input.getMessage().getParts().values());
            for (Part p : parts) {
                partsList.add(p);
            }
        }
        return partsList;
    }

    public List<Part> getOutMessageParts(Operation operation) {
        Output output = operation.getOutput();
        List<Part> partsList = new ArrayList<>();
        if (output != null && output.getMessage() != null) {
            Collection<Part> parts = CastUtils.cast(output.getMessage().getParts().values());
            for (Part p : parts) {
                partsList.add(p);
            }
        }
        return partsList;
    }

    public Binding getBinding(BindingOperation bop, Definition def) {
        Collection<Binding> ite = CastUtils.cast(def.getBindings().values());
        for (Binding binding : ite) {
            List<BindingOperation> bos = CastUtils.cast(binding.getBindingOperations());
            for (BindingOperation bindingOperation : bos) {
                if (bindingOperation.getName().equals(bop.getName())) {
                    return binding;
                }
            }
        }
        return null;
    }

}
