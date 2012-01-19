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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.validator.ServiceValidator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSParameter;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.WrapperElement;

public class WrapperStyleNameCollisionValidator extends ServiceValidator {
    public static final Logger LOG = LogUtils.getL7dLogger(WrapperStyleNameCollisionValidator.class);

    public WrapperStyleNameCollisionValidator() {
    }

    public WrapperStyleNameCollisionValidator(ServiceInfo s) {
        this.service = s;
    }

    @Override
    public boolean isValid() {
        return checkNameCollision();
    }

    private boolean checkNameCollision() {
        InterfaceInfo interfaceInfo = service.getInterface();
        if (interfaceInfo != null) {
            for (OperationInfo operation : interfaceInfo.getOperations()) {
                if (!isValidOperation(operation)) {
                    return false;
                }
            }
        }
        return true;
    }
    private boolean checkArray(String[] ar, String n) {
        if (ar != null) {
            if (ar.length == 0) {
                return true;
            }
            for (String s : ar) {
                if (s.equals(n)) {
                    return true;
                }
            }
        }
        return false;
    }
    private boolean checkBare(ToolContext context, String opName) {
        String o[] = context.getArray(ToolConstants.CFG_BAREMETHODS);
        if (checkArray(o, opName)) {
            return true;
        }
        return false;
    }
    private boolean isValidOperation(OperationInfo operation) {
        ToolContext context = service.getProperty(ToolContext.class.getName(), ToolContext.class);
        
        boolean c = context.optionSet(ToolConstants.CFG_AUTORESOLVE);

        boolean valid = false;
        if (operation.getUnwrappedOperation() == null) {
            valid = true;
        }

        String operationName = operation.getName().getLocalPart();
        operationName = ProcessorUtil.mangleNameToVariableName(operationName);

        
        JAXWSBinding binding = (JAXWSBinding)operation.getExtensor(JAXWSBinding.class);
        if (binding != null) {
            if (!binding.isEnableWrapperStyle()) {
                valid = true;
            } else if (binding.getMethodName() != null) {
                operationName = binding.getMethodName();
            }
        }
        binding = operation.getInterface().getExtensor(JAXWSBinding.class);
        if (binding != null) {
            if (!binding.isEnableWrapperStyle()) {
                valid = true;
            } else if (binding.getMethodName() != null) {
                operationName = binding.getMethodName();
            }
        }
        binding = operation.getInterface().getService()
            .getDescription().getExtensor(JAXWSBinding.class);
        if (binding != null) {
            if (!binding.isEnableWrapperStyle()) {
                valid = true;
            } else if (binding.getMethodName() != null) {
                operationName = binding.getMethodName();
            }
        }
        
        valid |= checkBare(context, operationName);
        
        if (valid) {
            return true;
        }

        MessagePartInfo input = null;
        MessagePartInfo output = null;
        if (operation.getInput() != null
            && operation.getInput().getMessageParts().size() == 1) {
            input = operation.getInput().getMessageParts().iterator().next();
        }

        if (operation.getOutput() != null
            && operation.getOutput().getMessageParts().size() == 1) {
            output = operation.getOutput().getMessageParts().iterator().next();
        }
        if (!c) {
            Map<String, QName> names = new HashMap<String, QName>();
            if (input != null) {
                for (WrapperElement element : ProcessorUtil.getWrappedElement(context, 
                                                                              input.getElementQName())) {
                    
                    String mappedName = mapElementName(operation,
                                                      operation.getUnwrappedOperation().getInput(),
                                                      element);
                    if (names.containsKey(mappedName)
                        &&  (names.get(mappedName) == element.getSchemaTypeName()
                            || names.get(mappedName).equals(element.getSchemaTypeName()))) {
                        handleErrors(names.get(mappedName), element);
                        return false;
                    } else {
                        names.put(mappedName, element.getSchemaTypeName());
                    }
                }
            }
    
            if (output != null) {
                List<WrapperElement> els = ProcessorUtil.getWrappedElement(context, output.getElementQName());
                if (els.size() > 1) {
                    for (WrapperElement element : els) {
                        String mappedName = mapElementName(operation,
                                                           operation.getUnwrappedOperation().getOutput(),
                                                           element);
                        if (names.containsKey(mappedName)
                            &&  !(names.get(mappedName) == element.getSchemaTypeName()
                                || names.get(mappedName).equals(element.getSchemaTypeName()))) {
                            handleErrors(names.get(mappedName), element);
                            return false;
                        } else {
                            names.put(mappedName, element.getSchemaTypeName());
                        }
                    }
                }
            }
        }
        return true;
    }

    private String mapElementName(OperationInfo op, MessageInfo mi, WrapperElement element) {
        MessagePartInfo mpi = mi.getMessagePart(element.getElementName());
        JAXWSBinding bind = op.getExtensor(JAXWSBinding.class);
        if (bind != null && bind.getJaxwsParas() != null) {
            for (JAXWSParameter par : bind.getJaxwsParas()) {
                if (mi.getName().getLocalPart().equals(par.getMessageName())
                    && mpi.getName().getLocalPart().equals(par.getElementName().getLocalPart())) {
                    return par.getName();
                }
            }
        }
        return mpi.getElementQName().getLocalPart();
    }

    private void handleErrors(QName e1, WrapperElement e2) {
        Message msg = new Message("WRAPPER_STYLE_NAME_COLLISION", LOG, 
                                  e2.getElementName(), e1, e2.getSchemaTypeName());
        addErrorMessage(msg.toString());
    }
}
