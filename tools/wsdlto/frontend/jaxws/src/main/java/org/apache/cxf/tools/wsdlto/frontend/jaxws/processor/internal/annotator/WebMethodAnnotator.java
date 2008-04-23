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

import javax.jws.WebMethod;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaMethod;

public class WebMethodAnnotator implements Annotator {

    public void annotate(JavaAnnotatable ja) {
        JavaMethod method = null;
        if (ja instanceof JavaMethod) {
            method = (JavaMethod) ja;
        } else {
            throw new RuntimeException("WebMethod can only annotate JavaMethod");
        }
        String operationName = method.getOperationName();
        JAnnotation methodAnnotation = new JAnnotation(WebMethod.class);
        
        if (!method.getName().equals(operationName)) {
            methodAnnotation.addElement(new JAnnotationElement("operationName", operationName));
        }
        if (!StringUtils.isEmpty(method.getSoapAction())) {
            methodAnnotation.addElement(new JAnnotationElement("action", method.getSoapAction()));
        }
        method.addAnnotation("WebMethod", methodAnnotation);
    }
}
