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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;

import org.apache.cxf.binding.soap.SOAPBindingUtil;
import org.apache.cxf.helpers.CastUtils;

public class MIMEBindingValidator
    extends AbstractDefinitionValidator {

    public MIMEBindingValidator(Definition def) {
        super(def);
    }

    public boolean isValid() {
        Collection<Binding> bindings = CastUtils.cast(def.getBindings().values());
        for (Binding binding : bindings) {
            Collection<BindingOperation> bindingOps = CastUtils.cast(binding.getBindingOperations());
            for (BindingOperation bindingOperation : bindingOps) {
                if (bindingOperation.getBindingInput() == null) {
                    continue;
                }
                Collection<ExtensibilityElement> exts = CastUtils.cast(bindingOperation
                                                                           .getBindingInput()
                                                                           .getExtensibilityElements());
                for (ExtensibilityElement extElement : exts) {
                    if (extElement instanceof MIMEMultipartRelated
                        && !doValidate((MIMEMultipartRelated)extElement,
                                       bindingOperation.getName())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean doValidate(MIMEMultipartRelated mimeExt, String operationName) {
        boolean gotRootPart = false;
        List<MIMEPart> parts = CastUtils.cast(mimeExt.getMIMEParts());
        for (MIMEPart mPart : parts) {
            List<MIMEContent> mimeContents = new ArrayList<>();
            List<ExtensibilityElement> extns = CastUtils.cast(mPart.getExtensibilityElements());
            for (ExtensibilityElement extElement : extns) {
                if (SOAPBindingUtil.isSOAPBody(extElement)) {
                    if (gotRootPart) {
                        addErrorMessage("Operation("
                                        + operationName
                                        + "): There's more than one soap body mime part"
                                        + " in its binding input");
                        return false;
                    }
                    gotRootPart = true;
                } else if (extElement instanceof MIMEContent) {
                    mimeContents.add((MIMEContent)extElement);
                }
            }
            if (!doValidateMimeContentPartNames(mimeContents, operationName)) {
                return false;
            }
        }
        if (!gotRootPart) {
            addErrorMessage("Operation("
                            + operationName
                            + "): There's no soap body in mime part"
                            + " in its binding input");
            return false;
        }
        return true;
    }

    private boolean doValidateMimeContentPartNames(List<MIMEContent> mimeContents,
                                                   String operationName) {
        // validate mime:content(s) in the mime:part as per R2909
        String partName = null;
        for (MIMEContent mimeContent : mimeContents) {
            String mimeContnetPart = mimeContent.getPart();
            if (mimeContnetPart == null) {
                addErrorMessage("Operation("
                                + operationName
                                + "): Must provide part attribute value for meme:content elements");
                return false;
            }
            if (partName == null) {
                partName = mimeContnetPart;
            } else {
                if (!partName.equals(mimeContnetPart)) {
                    addErrorMessage("Operation("
                                    + operationName
                                    + "): Part attribute value for meme:content "
                                    + "elements are different");
                    return false;
                }
            }
        }
        return true;
    }
}
