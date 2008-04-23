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

import javax.jws.Oneway;
import javax.jws.WebResult;
import javax.jws.soap.SOAPBinding;

import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaMethod;

public class WebResultAnnotator implements Annotator {

    public void annotate(JavaAnnotatable ja) {
        JavaMethod method = null;
        if (ja instanceof JavaMethod) {
            method = (JavaMethod) ja;
        } else {
            throw new RuntimeException("WebResult can only annotate JavaMethod");
        }
            
        if (method.isOneWay()) {
            JAnnotation oneWayAnnotation = new JAnnotation(Oneway.class);
            method.addAnnotation("Oneway", oneWayAnnotation);
            return;
        }

        if ("void".equals(method.getReturn().getType())) {
            return;
        }
        JAnnotation resultAnnotation = new JAnnotation(WebResult.class);
        String targetNamespace = method.getReturn().getTargetNamespace();
        String name = "return";

        if (method.getSoapStyle() == SOAPBinding.Style.DOCUMENT && !method.isWrapperStyle()) {
            name = method.getName() + "Response";
        }

        if (method.getSoapStyle() == SOAPBinding.Style.RPC) {
            name = method.getReturn().getName();
            targetNamespace = method.getInterface().getNamespace();
           
        }
        if (method.getSoapStyle() == SOAPBinding.Style.DOCUMENT) {
            if (method.getReturn().getQName() != null) {
                name = method.getReturn().getQName().getLocalPart();
            }
            targetNamespace = method.getReturn().getTargetNamespace();
        }

        
        resultAnnotation.addElement(new JAnnotationElement("name", name));
        if (null != targetNamespace) {
            resultAnnotation.addElement(new JAnnotationElement("targetNamespace", targetNamespace));
        }

        if (method.getSoapStyle() == SOAPBinding.Style.RPC
            || (method.getSoapStyle() == SOAPBinding.Style.DOCUMENT && !method.isWrapperStyle())) {
            resultAnnotation.addElement(new JAnnotationElement("partName", 
                                                                      method.getReturn().getName()));
        }

        method.addAnnotation("WebResult", resultAnnotation);
        method.getInterface().addImport("javax.jws.WebResult");
    }
}
