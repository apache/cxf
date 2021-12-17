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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.validation.ResponseConstraintViolationException;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {
    private static final Logger LOG = LogUtils.getL7dLogger(ValidationExceptionMapper.class);
    private boolean addMessageToResponse;

    @Override
    public Response toResponse(ValidationException exception) {
        Response.Status errorStatus = Response.Status.INTERNAL_SERVER_ERROR;
        if (exception instanceof ConstraintViolationException) {

            StringBuilder responseBody = addMessageToResponse ? new StringBuilder() : null;

            final ConstraintViolationException constraint = (ConstraintViolationException) exception;

            for (final ConstraintViolation< ? > violation: constraint.getConstraintViolations()) {
                String message = buildErrorMessage(violation);
                if (responseBody != null) {
                    responseBody.append(message).append('\n');
                }
                LOG.log(Level.WARNING, message);
            }

            if (!(constraint instanceof ResponseConstraintViolationException)) {
                errorStatus = Response.Status.BAD_REQUEST;
            }
            return buildResponse(errorStatus, responseBody != null ? responseBody.toString() : null);
        }
        return buildResponse(errorStatus, addMessageToResponse ? exception.getMessage() : null);
    }

    protected String buildErrorMessage(ConstraintViolation<?> violation) {
        return "Value "
            + (violation.getInvalidValue() != null ? "'" + violation.getInvalidValue().toString() + "'" : "(null)")
            + " of " + violation.getRootBeanClass().getSimpleName()
            + "." + violation.getPropertyPath()
            + ": " + violation.getMessage();
    }

    protected Response buildResponse(Response.Status errorStatus, String responseText) {
        ResponseBuilder rb = JAXRSUtils.toResponseBuilder(errorStatus);
        if (responseText != null) {
            rb.type(MediaType.TEXT_PLAIN).entity(responseText);
        }
        return rb.build();
    }

    /**
     * Controls whether to add a constraint validation message to Response or not
     * @param addMessageToResponse add a constraint validation message to Response
     */
    public void setAddMessageToResponse(boolean addMessageToResponse) {
        this.addMessageToResponse = addMessageToResponse;
    }

}
