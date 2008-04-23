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

package org.apache.cxf.tools.java2wsdl.generator.wsdl11;


import java.util.Collection;
import java.util.HashSet;

import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.model.JavaClass;
import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.RequestWrapper;
import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.ResponseWrapper;

public final class WrapperBeanGenerator extends BeanGenerator {
    protected Collection<JavaClass> generateBeanClasses(final ServiceInfo serviceInfo) {
        Collection<JavaClass> wrapperClasses = new HashSet<JavaClass>();
        
        for (OperationInfo op : serviceInfo.getInterface().getOperations()) {
            if (op.getUnwrappedOperation() != null) {
                if (op.hasInput()) {
                    RequestWrapper requestWrapper = new RequestWrapper();
                    requestWrapper.setOperationInfo(op);
                    JavaClass jClass = requestWrapper.buildWrapperBeanClass();

                    if (requestWrapper.isWrapperBeanClassNotExist()) {
                        wrapperClasses.add(jClass);
                    }
                }
                if (op.hasOutput()) {
                    ResponseWrapper responseWrapper = new ResponseWrapper();
                    responseWrapper.setOperationInfo(op);
                    JavaClass jClass = responseWrapper.buildWrapperBeanClass();

                    if (responseWrapper.isWrapperBeanClassNotExist()) {
                        wrapperClasses.add(jClass);
                    }
                }
            }
        }
        return wrapperClasses;
    }
}
