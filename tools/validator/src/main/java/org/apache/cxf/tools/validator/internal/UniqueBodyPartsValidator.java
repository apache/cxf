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

package org.apache.cxf.tools.validator.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;

public class UniqueBodyPartsValidator extends AbstractDefinitionValidator {
    private Map<QName, String> uniqueBodyPartsMap;

    public UniqueBodyPartsValidator(Definition def) {
        super(def);
    }

    public boolean isValid() {
        Collection<Binding> bindings = CastUtils.cast(def.getAllBindings().values());
        for (Binding binding : bindings) {
            uniqueBodyPartsMap = new HashMap<QName, String>();
            List<BindingOperation> ops = CastUtils.cast(binding.getBindingOperations());
            for (BindingOperation op : ops) {
                Operation operation = op.getOperation();
                if (operation.getInput() != null) {
                    Message inMessage = operation.getInput().getMessage();
                    BindingInput bin = op.getBindingInput();
                    Set<String> headers = new HashSet<String>();
                    if (bin != null) {
                        List<ExtensibilityElement> lst = CastUtils.cast(bin.getExtensibilityElements());
                        for (ExtensibilityElement ext : lst) {
                            if (!(ext instanceof SOAPHeader)) {
                                continue;
                            }
                            SOAPHeader header = (SOAPHeader)ext;
                            if (!header.getMessage().equals(inMessage.getQName())) {
                                continue;
                            }
                            headers.add(header.getPart());
                        }
                    }
                    
                    //find the headers as they don't contribute to the body
                    
                    if (inMessage != null && !isUniqueBodyPart(operation.getName(), 
                                                               inMessage,
                                                               headers,
                                                               binding.getQName())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isUniqueBodyPart(String operationName, Message msg,
                                     Collection<String> headers, QName bindingName) {
        List<Part> partList = CastUtils.cast(msg.getOrderedParts(null));
        for (Part part : partList) {
            if (headers.contains(part.getName())) {
                continue;
            }
            if (part.getElementName() == null) {
                return true;
            }
            String opName = getOperationNameWithSamePart(operationName, part);
            if (opName != null) {
                addErrorMessage("Non unique body parts, operation " + "[ " + opName + " ] "
                                + "and  operation [ " + operationName + " ] in binding "
                                + bindingName.toString()
                                + " have the same body block: "
                                + part.getElementName());
                return false;
            }
            //just need to check the first element
            return true;
        }
        return true;
    }

    private String getOperationNameWithSamePart(String operationName, Part part) {
        QName partQN = part.getElementName();
        String opName = uniqueBodyPartsMap.get(partQN);
        if (opName == null) {
            uniqueBodyPartsMap.put(partQN, operationName);
            return null;
        } else {
            return opName;
        }
    }

}
