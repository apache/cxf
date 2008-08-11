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
import org.apache.cxf.tools.common.model.JavaMethod;

public class SoapBindingAnnotator implements Annotator {

    public void annotate(JavaAnnotatable ja) {
        JavaMethod method;
        if (ja instanceof JavaMethod) {
            method = (JavaMethod) ja;
        } else {
            throw new RuntimeException("SOAPBindingAnnotator can only annotate JavaMethod");
        }
        if (method.getSoapStyle() == SOAPBinding.Style.DOCUMENT) {
            if (!method.isWrapperStyle()
                && !SOAPBinding.ParameterStyle.BARE.equals(method.getInterface().getSOAPParameterStyle())) {
            
                JAnnotation bindingAnnotation = new JAnnotation(SOAPBinding.class);
                bindingAnnotation.addElement(new JAnnotationElement("parameterStyle", 
                                                                           SOAPBinding.ParameterStyle.BARE));
                method.addAnnotation("SOAPBinding", bindingAnnotation);
            } else if (method.isWrapperStyle()
                && SOAPBinding.ParameterStyle.BARE.equals(method.getInterface().getSOAPParameterStyle())) {
                JAnnotation bindingAnnotation = new JAnnotation(SOAPBinding.class);
                bindingAnnotation.addElement(new JAnnotationElement("parameterStyle", 
                                                                        SOAPBinding.ParameterStyle.WRAPPED));
                method.addAnnotation("SOAPBinding", bindingAnnotation);                
            }
        } else if (!SOAPBinding.Style.RPC.equals(method.getInterface().getSOAPStyle())) {
            JAnnotation bindingAnnotation = new JAnnotation(SOAPBinding.class);
            bindingAnnotation.addElement(new JAnnotationElement("style", 
                                                                       SOAPBinding.Style.RPC));
            method.addAnnotation("SOAPBinding", bindingAnnotation);            
        }
    }
    
}
