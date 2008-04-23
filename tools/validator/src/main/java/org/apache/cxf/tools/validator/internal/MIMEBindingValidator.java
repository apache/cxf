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
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;

import org.apache.cxf.tools.util.SOAPBindingUtil;

public class MIMEBindingValidator
    extends AbstractDefinitionValidator {

    public MIMEBindingValidator(Definition def) {
        super(def);
    }

    public boolean isValid() {
        Iterator itBinding = def.getBindings().keySet().iterator();
        while (itBinding.hasNext()) {
            Binding binding = (Binding)def.getBindings().get(itBinding.next());
            Iterator itOperation = binding.getBindingOperations().iterator();
            while (itOperation.hasNext()) {
                BindingOperation bindingOperation = (BindingOperation)itOperation.next();
                Iterator itInputExt = bindingOperation.getBindingInput().getExtensibilityElements()
                    .iterator();
                while (itInputExt.hasNext()) {
                    ExtensibilityElement extElement = (ExtensibilityElement)itInputExt.next();
                    if (extElement instanceof MIMEMultipartRelated) {
                        Iterator itMimeParts = ((MIMEMultipartRelated)extElement).getMIMEParts()
                            .iterator();
                        if (!doValidate(itMimeParts, bindingOperation.getName())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean doValidate(Iterator mimeParts, String operationName) {
        boolean gotRootPart = false;        
        while (mimeParts.hasNext()) {
            MIMEPart mPart = (MIMEPart)mimeParts.next();
            List<MIMEContent> mimeContents = new ArrayList<MIMEContent>();
            Iterator extns = mPart.getExtensibilityElements().iterator();
            while (extns.hasNext()) {
                ExtensibilityElement extElement = (ExtensibilityElement)extns.next();
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
            if (!doValidateMimeContentPartNames(mimeContents.iterator(), operationName)) {
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

    private boolean doValidateMimeContentPartNames(Iterator mimeContents, String operationName) {
        // validate mime:content(s) in the mime:part as per R2909
        String partName = null;
        while (mimeContents.hasNext()) {
            MIMEContent mimeContent = (MIMEContent)mimeContents.next();
            String mimeContnetPart = mimeContent.getPart();
            if (mimeContnetPart == null) {
                addErrorMessage("Operation("
                                + operationName
                                + "): Must provide part attribute value for meme:content elements");
                return false;
            } else {
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
        }
        return true;
    }
}
