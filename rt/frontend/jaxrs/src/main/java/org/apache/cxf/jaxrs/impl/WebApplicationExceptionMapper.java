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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

/**
 * Default exception mapper for {@link WebApplicationException}.
 * This class interacts with {@link FaultListener}. If the service has a {@link FaultListener},
 * then this mapper calls it to determine whether to log the exception. In theory, {@link FaultListener}
 * objects could take other actions, but since they cannot produce a {@link Response}, they
 * are practically limited to controlling logging.
 */
public class WebApplicationExceptionMapper 
    implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOG = LogUtils.getL7dLogger(WebApplicationExceptionMapper.class);
    private static final String ERROR_MESSAGE_START = "WebApplicationException has been caught, status: ";
    private boolean printStackTrace;
    
    public Response toResponse(WebApplicationException ex) {
        
        Response r = ex.getResponse();
        if (r == null) {
            r = Response.serverError().build();
        }
        
        Message msg = PhaseInterceptorChain.getCurrentMessage();
        FaultListener flogger = null;
        if (msg != null) {
            flogger = (FaultListener)PhaseInterceptorChain.getCurrentMessage()
                .getContextualProperty(FaultListener.class.getName());
        }
        if (flogger != null || LOG.isLoggable(Level.FINE)) {
            String errorMessage = buildErrorMessage(r, ex);

            if (flogger != null) {
                if (flogger.faultOccurred(ex, errorMessage, msg)) {
                    LOG.log(Level.INFO, errorMessage, ex);
                }
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, errorMessage, ex);
                    /*
                     * only print a stack trace if we are logging FINE.
                     * If there is a listener, let it print the stack trace if
                     * wants one.
                     */
                    if (printStackTrace) {
                        LOG.fine(getStackTrace(ex));
                    }
                }
            }
        }

        return r;
    }

    private String buildErrorMessage(Response r, WebApplicationException ex) {
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
    
    private static String getStackTrace(Exception ex) { 
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Control whether this mapper logs backtraces. If there is no {@link FaultListener},
     * and this is <tt>true</tt>, this mapper will log the stack trace at FINE.
     * @param printStackTrace whether to log stack trace.
     */
    public void setPrintStackTrace(boolean printStackTrace) {
        this.printStackTrace = printStackTrace;
    }

    
}
