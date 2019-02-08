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
package org.apache.cxf.systest.jaxrs;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;

@Provider
public class BookExceptionMapper implements ContextAware, ExceptionMapper<BookNotFoundFault> {
    private MessageContext mc;
    private boolean toHandle;

    public void setMessageContext(MessageContext context) {
        mc = context;
    }

    public Response toResponse(BookNotFoundFault ex) {
        // status is 200 just to simplify the test client code
        if (toHandle) {
            OperationResourceInfo ori =
                (OperationResourceInfo)mc.getContextualProperty(OperationResourceInfo.class);
            if (ori == null) {
                throw new RuntimeException();
            }
            return Response.status(500).type(MediaType.TEXT_PLAIN_TYPE)
                      .entity("No book found at all : " + ex.getFaultInfo().getId()).build();
        }
        return null;
    }

    public void setToHandle(Boolean toHandle) {
        this.toHandle = toHandle;
    }
}
