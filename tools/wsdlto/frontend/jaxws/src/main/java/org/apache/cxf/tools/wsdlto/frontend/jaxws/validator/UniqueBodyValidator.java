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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.validator.ServiceValidator;

public class UniqueBodyValidator extends ServiceValidator {
    public static final Logger LOG = LogUtils.getL7dLogger(UniqueBodyValidator.class);

    public UniqueBodyValidator() {
    }

    public UniqueBodyValidator(ServiceInfo s) {
        this.service = s;
    }

    @Override
    public boolean isValid() {
        return checkUniqueBody();
    }

    private boolean checkUniqueBody() {
        Collection<EndpointInfo> endpoints = service.getEndpoints();
        if (endpoints != null) {
            for (EndpointInfo endpoint : endpoints) {
                if (!isValidEndpoint(endpoint)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidEndpoint(EndpointInfo endpoint) {
        BindingInfo binding = endpoint.getBinding();
        Map<QName, QName> uniqueNames = new HashMap<>();
        Map<QName, Set<String>> actions = new HashMap<>();

        Collection<BindingOperationInfo> bos = binding.getOperations();
        for (BindingOperationInfo bo : bos) {
            OperationInfo op = binding.getInterface().getOperation(bo.getName());
            if (op.getInput() != null
                && op.getInput().getMessagePartsNumber() == 1) {
                MessagePartInfo part = op.getInput().getFirstMessagePart();
                if (part.getElementQName() == null) {
                    continue;
                }
                QName mName = part.getElementQName();
                String action = getWSAAction(op.getInput());
                QName opName = uniqueNames.get(mName);
                Set<String> opActions = actions.get(mName);
                if (opName != null && opActions != null && !opActions.contains(action)) {
                    opName = null;
                }
                if (opName != null) {
                    Message msg = new Message("NON_UNIQUE_BODY", LOG,
                                              endpoint.getName(), op.getName(), opName, mName);
                    addErrorMessage(msg.toString());
                    return false;
                }
                uniqueNames.put(mName, op.getName());
                if (action != null) {
                    if (opActions == null) {
                        opActions = new HashSet<>();
                        actions.put(mName, opActions);
                    }
                    opActions.add(action);
                }
            }

            for (BindingFaultInfo fault : bo.getFaults()) {
                if (fault.getFaultInfo().getMessagePartsNumber() > 1) {
                    Message msg = new Message("FAULT_WITH_MULTIPLE_PARTS", LOG,
                                              fault.getFaultInfo().getName()
                                                  .getLocalPart());
                    addErrorMessage(msg.toString());
                    return false;
                }
            }
        }
        return true;
    }

    private String getWSAAction(MessageInfo input) {
        if (input.getExtensionAttributes() != null) {
            for (Map.Entry<QName, Object> ent : input.getExtensionAttributes().entrySet()) {
                if ("Action".equals(ent.getKey().getLocalPart())) {
                    return ent.getValue().toString();
                }
            }
        }
        return null;
    }
}
