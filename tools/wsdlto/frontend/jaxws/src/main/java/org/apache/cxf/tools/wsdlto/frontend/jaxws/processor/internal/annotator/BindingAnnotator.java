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

import javax.jws.soap.SOAPBinding;

import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;

public final class BindingAnnotator implements Annotator {
    
    public void annotate(JavaAnnotatable ja) {
        JavaInterface intf;
        if (ja instanceof JavaInterface) {
            intf = (JavaInterface) ja;
        } else {
            throw new RuntimeException("BindingAnnotator can only annotate JavaInterface");
        }
        
        if (processBinding(intf)) {
            JAnnotation bindingAnnotation = new JAnnotation(SOAPBinding.class);
            if (!SOAPBinding.Style.DOCUMENT.equals(intf.getSOAPStyle())) {
                bindingAnnotation.addElement(new JAnnotationElement("style", intf.getSOAPStyle()));
            }
            if (!SOAPBinding.Use.LITERAL.equals(intf.getSOAPUse())) {
                bindingAnnotation.addElement(new JAnnotationElement("use", intf.getSOAPUse()));
            }            
            if (intf.getSOAPStyle() == SOAPBinding.Style.DOCUMENT) {
                bindingAnnotation.addElement(new JAnnotationElement("parameterStyle", 
                                                                           intf.getSOAPParameterStyle()));
            }
            intf.addAnnotation(bindingAnnotation);
        }
    }
    
    private boolean processBinding(JavaInterface intf) {
        SOAPBinding.Style soapStyle = intf.getSOAPStyle();
        SOAPBinding.Use soapUse = intf.getSOAPUse();
        boolean isWrapped = true;
        int count = 0;
        for (JavaMethod method : intf.getMethods()) {
            if (!method.isWrapperStyle()) {
                isWrapped = false;
                count++;
            }
            if (soapStyle == null
                && method.getSoapStyle() != null) {
                soapStyle = method.getSoapStyle();
            }
            if (soapUse == null
                && method.getSoapUse() != null) {
                soapUse = method.getSoapUse();
            }
        }

        if (soapStyle == SOAPBinding.Style.DOCUMENT) {
            intf.setSOAPStyle(SOAPBinding.Style.DOCUMENT);
            if (isWrapped) {
                intf.setSOAPParameterStyle(SOAPBinding.ParameterStyle.WRAPPED);
            } else {
                intf.setSOAPParameterStyle(SOAPBinding.ParameterStyle.BARE);
            }
        } else if (soapStyle == null) {
            intf.setSOAPStyle(SOAPBinding.Style.DOCUMENT);
            if (isWrapped) {
                intf.setSOAPParameterStyle(SOAPBinding.ParameterStyle.WRAPPED);
            } else {
                intf.setSOAPParameterStyle(SOAPBinding.ParameterStyle.BARE);
            }
            
        } else {
            intf.setSOAPStyle(SOAPBinding.Style.RPC);
        }
        
        if (soapUse == SOAPBinding.Use.LITERAL) {
            intf.setSOAPUse(SOAPBinding.Use.LITERAL);
        } else if (soapUse == null) {
            intf.setSOAPUse(SOAPBinding.Use.LITERAL);
        } else {
            intf.setSOAPUse(SOAPBinding.Use.ENCODED);
        }

        if (intf.getSOAPStyle() == SOAPBinding.Style.DOCUMENT
            && count != 0
            && count != intf.getMethods().size()) {
            return false;
        }

        if (intf.getSOAPStyle() == SOAPBinding.Style.DOCUMENT
            && intf.getSOAPUse() == SOAPBinding.Use.LITERAL
            && intf.getSOAPParameterStyle() == SOAPBinding.ParameterStyle.WRAPPED) {
            return false;
        }
        return true;
    }
}

