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

package org.apache.cxf.binding.xml;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.bindings.xformat.XMLBindingMessageFormat;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.validator.ServiceValidator;
import org.apache.cxf.wsdl.WSDLConstants;

public class XMLFormatValidator extends ServiceValidator {

    public XMLFormatValidator() {
    }

    public XMLFormatValidator(ServiceInfo s) {
        this.service = s;
    }

    @Override
    public boolean isValid() {
        return checkXMLBindingFormat();
    }

    private boolean checkXMLBindingFormat() {
        Collection<BindingInfo> bindings = service.getBindings();
        if (bindings != null) {
            for (BindingInfo binding : bindings) {
                if (WSDLConstants.NS_BINDING_XML.equalsIgnoreCase(binding.getBindingId())
                    && !checkXMLFormat(binding)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkXMLFormat(BindingInfo binding) {
        Collection<BindingOperationInfo> bos = binding.getOperations();
        for (BindingOperationInfo bo : bos) {
            OperationInfo op = binding.getInterface().getOperation(bo.getName());
            boolean needRootNode = false;
            final int inputPartsNum = op.getInput().getMessagePartsNumber();
            if (inputPartsNum == 0 || inputPartsNum > 1) {
                needRootNode = true;
            }
            if (needRootNode) {
                String path = "Binding(" + binding.getName().getLocalPart()
                    + "):BindingOperation(" + bo.getName() + ")";
                List<XMLBindingMessageFormat> inExtensors =
                    bo.getInput().getExtensors(XMLBindingMessageFormat.class);
                Iterator<XMLBindingMessageFormat> itIn = null;
                if (inExtensors != null) {
                    itIn = inExtensors.iterator();
                }
                if (!findXMLFormatRootNode(itIn, bo, path + "-input")) {
                    return false;
                }
                // Input check correct, continue to check output binding
                if (op.getOutput() != null) {
                    needRootNode = false;
                    final int outputPartsNum = op.getOutput().getMessagePartsNumber();
                    if (outputPartsNum == 0 || outputPartsNum > 1) {
                        needRootNode = true;
                    }
                    if (needRootNode) {
                        List<XMLBindingMessageFormat> outExtensors =
                            bo.getOutput().getExtensors(XMLBindingMessageFormat.class);
                        Iterator<XMLBindingMessageFormat> itOut = null;
                        if (outExtensors != null) {
                            itOut = outExtensors.iterator();
                        }
                        boolean result = findXMLFormatRootNode(itOut, bo, path + "-Output");
                        if (!result) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean findXMLFormatRootNode(Iterator<XMLBindingMessageFormat> it,
                                          BindingOperationInfo bo,
                                          String errorPath) {
        while (it != null && it.hasNext()) {
            XMLBindingMessageFormat xmlFormat = it.next();
            if (xmlFormat.getRootNode() == null) {
                QName rootNodeName = bo.getName();
                addErrorMessage(errorPath
                                + ": empty value of rootNode attribute, the value should be "
                                + rootNodeName);
                return false;
            }
        }
        return true;
    }
}
