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
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.validator.ServiceValidator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;
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
        return checkNameColllision();
    }

    private boolean checkNameColllision() {
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

    private boolean isValidOperation(OperationInfo operation) {
        ToolContext context = service.getProperty(ToolContext.class.getName(), ToolContext.class);
        
        boolean c = context.optionSet(ToolConstants.CFG_AUTORESOLVE);

        boolean valid = false;
        if (operation.getUnwrappedOperation() == null) {
            valid = true;
        }
        
        JAXWSBinding binding = (JAXWSBinding)operation.getExtensor(JAXWSBinding.class);
        if (binding != null && !binding.isEnableWrapperStyle()) {
            valid = true;
        }
        binding = operation.getInterface().getExtensor(JAXWSBinding.class);
        if (binding != null && !binding.isEnableWrapperStyle()) {
            valid = true;
        }
        binding = operation.getInterface().getService()
            .getDescription().getExtensor(JAXWSBinding.class);
        if (binding != null && !binding.isEnableWrapperStyle()) {
            valid = true;
        }
        
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
            Map<QName, QName> names = new HashMap<QName, QName>();
            if (input != null) {
                for (WrapperElement element : ProcessorUtil.getWrappedElement(context, 
                                                                              input.getElementQName())) {
                    if (names.containsKey(element.getElementName())
                        &&  (names.get(element.getElementName()) == element.getSchemaTypeName()
                            || names.get(element.getElementName()).equals(element.getSchemaTypeName()))) {
                        handleErrors(names.get(element.getElementName()), element);
                        return false;
                    } else {
                        names.put(element.getElementName(), element.getSchemaTypeName());
                    }
                }
            }
    
            if (output != null) {
                List<WrapperElement> els = ProcessorUtil.getWrappedElement(context, output.getElementQName());
                if (els.size() > 1) {
                    for (WrapperElement element : els) {
                        if (names.containsKey(element.getElementName())
                            &&  !(names.get(element.getElementName()) == element.getSchemaTypeName()
                                || names.get(element.getElementName()).equals(element.getSchemaTypeName()))) {
                            handleErrors(names.get(element.getElementName()), element);
                            return false;
                        } else {
                            names.put(element.getElementName(), element.getSchemaTypeName());
                        }
                    }
                }
            }
        }
        return true;
    }

    private void handleErrors(QName e1, WrapperElement e2) {
        Message msg = new Message("WRAPPER_STYLE_NAME_COLLISION", LOG, 
                                  e2.getElementName(), e1, e2.getSchemaTypeName());
        addErrorMessage(msg.toString());
    }
}
