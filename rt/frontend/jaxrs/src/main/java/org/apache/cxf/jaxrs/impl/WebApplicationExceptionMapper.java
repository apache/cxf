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

package org.apache.cxf.jaxrs.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

/**
 * Default exception mapper for {@link WebApplicationException}.
 * This class interacts with {@link FaultListener}.
 * If {@link FaultListener} is available and has indicated that it handled the exception then
 * no more logging is done, otherwise a message is logged at WARN (default) or FINE level
 * which can be controlled with a printStackTrace property
 */
public class WebApplicationExceptionMapper
    implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOG = LogUtils.getL7dLogger(WebApplicationExceptionMapper.class);
    private static final String ERROR_MESSAGE_START = "WebApplicationException has been caught, status: ";
    private boolean printStackTrace = true;
    private boolean addMessageToResponse;

    public Response toResponse(WebApplicationException ex) {

        Response r = ex.getResponse();
        if (r == null) {
            r = Response.serverError().build();
        }
        boolean doAddMessage = r.getEntity() == null && addMessageToResponse;


        Message msg = PhaseInterceptorChain.getCurrentMessage();
        FaultListener flogger = null;
        if (msg != null) {
            flogger = (FaultListener)PhaseInterceptorChain.getCurrentMessage()
                .getContextualProperty(FaultListener.class.getName());
        }
        String errorMessage = doAddMessage || flogger != null
            ? buildErrorMessage(r, ex) : null;
        if (flogger == null
            || !flogger.faultOccurred(ex, errorMessage, msg)) {
            Level level = printStackTrace ? getStackTraceLogLevel(msg, r) : Level.FINE;
            LOG.log(level, ExceptionUtils.getStackTrace(ex));
        }

        if (doAddMessage) {
            r = JAXRSUtils.copyResponseIfNeeded(r);
            r = buildResponse(r, errorMessage);
        }
        return r;
    }

    protected Level getStackTraceLogLevel(Message msg, Response r) {
        if (r.getStatus() == 404) {
            Level logLevel = JAXRSUtils.getExceptionLogLevel(msg, NotFoundException.class);
            return logLevel == null ? Level.FINE : logLevel;
        }
        return Level.WARNING;
    }

    protected String buildErrorMessage(Response r, WebApplicationException ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ERROR_MESSAGE_START).append(r.getStatus());

        Throwable cause = ex.getCause();
        String message = cause == null ? ex.getMessage() : cause.getMessage();
        if (message == null && cause != null) {
            message = "exception cause class: " + cause.getClass().getName();
        }
        if (message != null) {
            sb.append(", message: ").append(message);
        }
        return sb.toString();
    }

    protected Response buildResponse(Response response, String responseText) {
        Response.ResponseBuilder rb = JAXRSUtils.fromResponse(response);
        if (responseText != null) {
            rb.type(MediaType.TEXT_PLAIN).entity(responseText);
        }
        return rb.build();
    }

    /**
     * Control whether to log at WARN or FINE level.
     * Note this property is ignored if a registered {@link FaultListener}
     * has handled the exception
     * @param printStackTrace if set to true then WARN level is used (default),
     *        otherwise - FINE level.
     */
    public void setPrintStackTrace(boolean printStackTrace) {
        this.printStackTrace = printStackTrace;
    }

    /**
     * Controls whether to add an error message to Response or not,
     * @param addMessageToResponse add a message to Response, ignored
     *        if the captuted WebApplicationException has
     *        a Response with a non-null entity
     */
    public void setAddMessageToResponse(boolean addMessageToResponse) {
        this.addMessageToResponse = addMessageToResponse;
    }


}
