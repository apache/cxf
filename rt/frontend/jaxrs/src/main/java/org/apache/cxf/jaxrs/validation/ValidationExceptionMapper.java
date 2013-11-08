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
package org.apache.cxf.jaxrs.validation;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.validation.ResponseConstraintViolationException;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper< ValidationException > {
    private static final Logger LOG = LogUtils.getL7dLogger(ValidationExceptionMapper.class);
    private boolean reportParameterInfo;
    
    @Override
    public Response toResponse(ValidationException exception) {
        if (exception instanceof ConstraintViolationException) { 
            
            final ConstraintViolationException constraint = (ConstraintViolationException) exception;
            final boolean isResponseException = constraint instanceof ResponseConstraintViolationException;
                        
            for (final ConstraintViolation< ? > violation: constraint.getConstraintViolations()) {
                LOG.log(Level.SEVERE, 
                    violation.getRootBeanClass().getSimpleName() 
                    + "." + getPropertyPathDescription(violation.getPropertyPath(), isResponseException) 
                    + ": " + violation.getMessage());
            }
            
            if (isResponseException) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        
    }
    
    protected String getPropertyPathDescription(Path propertyPath, boolean isResponseException) {
        final String path = propertyPath.toString();
        if (isResponseException || !isReportParameterInfo()) {
            return path;
        }
        int index = path.lastIndexOf(".arg");
        if (index == -1 || index + 4 >= path.length()) {
            return path;
        }
        
        boolean compositePath = false;
        
        int argPos;
        try { 
            String argPath = path.substring(index + 4);
            int infoIndex = argPath.indexOf('[');
            if (infoIndex == -1) {
                infoIndex = argPath.indexOf('.');
            }
            if (infoIndex > 0) {
                argPath = argPath.substring(0, infoIndex);
                compositePath = true;
            }
            argPos = Integer.valueOf(argPath);
        } catch (NumberFormatException ex) {
            return path;
        }
        final OperationResourceInfo ori = JAXRSUtils.getCurrentMessage().getExchange().get(OperationResourceInfo.class);
        if (argPos < 0 || argPos > ori.getParameters().size()) {
            return path;
        }
        Parameter param = ori.getParameters().get(argPos);
        
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (compositePath) {
            sb.append("arg" + argPos + " ");
        }
        sb.append("JAXRS param is " + param.getType().toString());
        if (param.getName() != null) {
            sb.append("(\"" + param.getName() + "\")");
        }
        sb.append(", class: " + ori.getAnnotatedMethod().getParameterTypes()[argPos].getSimpleName());
        sb.append(")");
        return path + sb.toString();
            
    }

    public boolean isReportParameterInfo() {
        return reportParameterInfo;
    }

    public void setReportParameterInfo(boolean reportParameterInfo) {
        this.reportParameterInfo = reportParameterInfo;
    }
}
