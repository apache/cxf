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
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.validation.ResponseConstraintViolationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper< ValidationException > {
    private static final Logger LOG = LogUtils.getL7dLogger(ValidationExceptionMapper.class);
    
    private boolean addMessageToResponse;
    
    private Expression messageExpression;
    
    @Override
    public Response toResponse(ValidationException exception) {
        Response.Status errorStatus = Response.Status.INTERNAL_SERVER_ERROR;
        StringBuilder responseBody= new StringBuilder(512);
        if (exception instanceof ConstraintViolationException) { 
            
            final ConstraintViolationException constraint = (ConstraintViolationException) exception;
            
            for (final ConstraintViolation< ? > violation: constraint.getConstraintViolations()) {
                String message=getMessage(violation);

                LOG.log(Level.WARNING, message);
                if(addMessageToResponse){
                    responseBody.append(message).append("\n");
                }
            }
            
            if (!(constraint instanceof ResponseConstraintViolationException)) {
                errorStatus = Response.Status.BAD_REQUEST;
            }
        } 
        Response.ResponseBuilder rb=JAXRSUtils.toResponseBuilder(errorStatus);
        if(addMessageToResponse){
            rb.entity(responseBody.toString());
        }
        return rb.build();
    }
    String getMessage(ConstraintViolation< ?> violation) {
        String message;
        if (messageExpression == null) {
            message = violation.getRootBeanClass().getSimpleName()
                    + "." + violation.getPropertyPath()
                    + ": " + violation.getMessage();
        } else {
            message = messageExpression.getValue(violation, String.class);
        }
        return message;

    }
    /**
     * Controls whether to add an constraint validation message to Response or not,
     * @param addMessageToResponse add a constraint validation message to Respons
     */
    public void setAddMessageToResponse(boolean addMessageToResponse) {
        this.addMessageToResponse = addMessageToResponse;
    }

    public void setMessageTemplate(String messageTemplate) {
        ExpressionParser parser = new SpelExpressionParser();
        this.messageExpression = parser.parseExpression(messageTemplate);
    }
    
    
}
