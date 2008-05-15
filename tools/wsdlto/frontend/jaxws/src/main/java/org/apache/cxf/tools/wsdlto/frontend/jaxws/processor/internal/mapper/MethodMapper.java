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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper;

import javax.wsdl.OperationType;

import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;

public final class MethodMapper {

    public JavaMethod map(OperationInfo operation) {
        JavaMethod method = new JavaMethod();
        // set default Document Bare style
        method.setSoapStyle(javax.jws.soap.SOAPBinding.Style.DOCUMENT);

        String operationName = operation.getName().getLocalPart();

        method.setName(ProcessorUtil.mangleNameToVariableName(operationName));
        method.setOperationName(operationName);

        JAXWSBinding opBinding = operation.getExtensor(JAXWSBinding.class);
        if (opBinding != null
            && opBinding.getMethodName() != null) {
            method.setName(opBinding.getMethodName());
        }
        
        if (opBinding != null
            && opBinding.getMethodJavaDoc() != null) {
            method.setJavaDoc(opBinding.getMethodJavaDoc());
        }


        if (operation.isOneWay()) {
            method.setStyle(OperationType.ONE_WAY);
        } else {
            method.setStyle(OperationType.REQUEST_RESPONSE);
        }

        method.setWrapperStyle(operation.isUnwrappedCapable());

        return method;
    }
}
