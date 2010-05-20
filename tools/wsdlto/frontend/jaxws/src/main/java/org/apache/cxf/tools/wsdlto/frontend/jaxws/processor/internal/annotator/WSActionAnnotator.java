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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Action;
import javax.xml.ws.FaultAction;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.AbstractMessageContainer;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;

public final class WSActionAnnotator implements Annotator {
    private static final QName WSAW_ACTION_QNAME = new QName("http://www.w3.org/2006/05/addressing/wsdl", 
                                                             "Action");    
    private static final QName WSAM_ACTION_QNAME = new QName("http://www.w3.org/2007/05/addressing/metadata", 
                                                             "Action");    
    private static final QName WSAW_OLD_ACTION_QNAME 
        = new QName("http://www.w3.org/2005/02/addressing/wsdl", "Action");    
    private OperationInfo operation;

    public WSActionAnnotator(final OperationInfo op) {
        this.operation = op;
    }
    
    private String getAction(AbstractMessageContainer mi) {
        QName action = (QName)mi.getExtensionAttribute(WSAW_ACTION_QNAME);
        if (action == null) {
            action = (QName)mi.getExtensionAttribute(WSAM_ACTION_QNAME);
        }
        if (action == null) {
            action = (QName)mi.getExtensionAttribute(WSAW_OLD_ACTION_QNAME);
        }
        if (action != null) {
            String s = action.getLocalPart();
            if (!StringUtils.isEmpty(s)) {
                return s;
            }
        } 
        return null;
    }
    
    public void annotate(JavaAnnotatable ja) {
        JavaMethod method;
        if (ja instanceof JavaMethod) {
            method = (JavaMethod) ja;
        } else {
            throw new RuntimeException("Action can only annotate JavaMethod");
        }

        boolean required = false;


        JavaInterface intf = method.getInterface();
        MessageInfo inputMessage = operation.getInput();
        MessageInfo outputMessage = operation.getOutput();

        JAnnotation actionAnnotation = new JAnnotation(Action.class);
        if (inputMessage.getExtensionAttributes() != null) {
            String inputAction = getAction(inputMessage);
            if (inputAction != null) {
                actionAnnotation.addElement(new JAnnotationElement("input", 
                                                                   inputAction));
                required = true;
            }
        }

        if (outputMessage != null && outputMessage.getExtensionAttributes() != null) {
            String outputAction = getAction(outputMessage);
            if (outputAction != null) {
                actionAnnotation.addElement(new JAnnotationElement("output", 
                                                                   outputAction));
                required = true;
            }
        }
        if (operation.hasFaults()) {
            List<JAnnotation> faultAnnotations = new ArrayList<JAnnotation>();
            for (FaultInfo faultInfo : operation.getFaults()) {
                if (faultInfo.getExtensionAttributes() != null) {
                    String faultAction = getAction(faultInfo);
                    if (faultAction == null) {
                        continue;
                    }

                    JavaException exceptionClass = getExceptionClass(method, faultInfo);                    
                    if (!StringUtils.isEmpty(exceptionClass.getPackageName())
                        && !exceptionClass.getPackageName().equals(intf.getPackageName())) {
                        intf.addImport(exceptionClass.getClassName());
                    }                    

                    JAnnotation faultAnnotation = new JAnnotation(FaultAction.class);
                    faultAnnotation.addElement(new JAnnotationElement("className", exceptionClass));
                    faultAnnotation.addElement(new JAnnotationElement("value", 
                                                                      faultAction));
                    faultAnnotations.add(faultAnnotation);
                    required = true;
                }
            }
            actionAnnotation.addElement(new JAnnotationElement("fault", faultAnnotations));
        }

        if (required) {
            method.addAnnotation("Action", actionAnnotation);
        }
    }

    private JavaException getExceptionClass(JavaMethod method, FaultInfo faultInfo) {
        for (JavaException exception : method.getExceptions()) {
            QName faultName = faultInfo.getName();
            if (exception.getTargetNamespace().equals(faultName.getNamespaceURI()) 
                && exception.getName().toLowerCase().startsWith(faultName.getLocalPart().toLowerCase())) {
                return exception;
            }
        }
        return null;
    }
}
