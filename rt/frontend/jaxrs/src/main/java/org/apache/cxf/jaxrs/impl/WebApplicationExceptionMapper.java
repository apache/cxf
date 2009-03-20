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

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;

public class WebApplicationExceptionMapper 
    implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOG = LogUtils.getL7dLogger(WebApplicationExceptionMapper.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(WebApplicationExceptionMapper.class);
    
    public Response toResponse(WebApplicationException ex) {
        if (LOG.isLoggable(Level.WARNING)) {
            String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
            if (message == null) {
                if (ex.getCause() != null) {
                    message = "cause is " + ex.getCause().getClass().getName();
                } else {
                    message = "no cause is available";
                }
            }
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("WEB_APP_EXCEPTION", BUNDLE, message);
            LOG.warning(errorMsg.toString());
        }
        Response r = ex.getResponse(); 
        if (r == null) {
            String message = null;
            if (ex.getCause() == null) {
                message = new org.apache.cxf.common.i18n.Message("DEFAULT_EXCEPTION_MESSAGE", 
                                                                 BUNDLE).toString();
            } else {
                message = ex.getCause().getMessage();
                if (message == null) {
                    message = ex.getCause().getClass().getName();
                }
            }
            r = Response.status(500).type(MediaType.TEXT_PLAIN).entity(message).build();
        }
        return r;
    }

}
