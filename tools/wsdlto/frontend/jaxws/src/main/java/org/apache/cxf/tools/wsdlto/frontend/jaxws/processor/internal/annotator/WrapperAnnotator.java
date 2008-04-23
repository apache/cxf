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

import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;

public class WrapperAnnotator implements Annotator {
    JavaParameter wrapperRequest;
    JavaParameter wrapperResponse;

    public WrapperAnnotator(JavaParameter request, JavaParameter response) {
        wrapperRequest = request;
        wrapperResponse = response;
    }
    
    public void annotate(JavaAnnotatable ja) {
        JavaMethod method;
        if (ja instanceof JavaMethod) {
            method = (JavaMethod) ja;
        } else {
            throw new RuntimeException("RequestWrapper and ResponseWrapper can only annotate JavaMethod");
        }
        if (wrapperRequest != null) {
            JAnnotation requestAnnotation = new JAnnotation(RequestWrapper.class);
            requestAnnotation.addElement(new JAnnotationElement("localName",
                                                                       wrapperRequest.getType()));
            requestAnnotation.addElement(new JAnnotationElement("targetNamespace",
                                                                       wrapperRequest.getTargetNamespace()));
            requestAnnotation.addElement(new JAnnotationElement("className", 
                                                                       wrapperRequest.getClassName()));

            method.addAnnotation("RequestWrapper", requestAnnotation);
            method.getInterface().addImports(requestAnnotation.getImports());
        }
        if (wrapperResponse != null) {
            List<JAnnotationElement> elements = new ArrayList<JAnnotationElement>();
            elements.add(new JAnnotationElement("localName", wrapperResponse.getType()));
            elements.add(new JAnnotationElement("targetNamespace", wrapperResponse.getTargetNamespace()));
            elements.add(new JAnnotationElement("className", wrapperResponse.getClassName()));

            JAnnotation responseAnnotation = new JAnnotation(ResponseWrapper.class);
            responseAnnotation.getElements().addAll(elements);
            method.addAnnotation("ResponseWrapper", responseAnnotation);
            method.getInterface().addImports(responseAnnotation.getImports());
        }
    }
}
