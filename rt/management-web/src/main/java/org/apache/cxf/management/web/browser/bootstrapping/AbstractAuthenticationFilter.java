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

package org.apache.cxf.management.web.browser.bootstrapping;

import java.lang.reflect.Method;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

public abstract class AbstractAuthenticationFilter implements RequestHandler {
    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        OperationResourceInfo ori = m.getExchange().get(OperationResourceInfo.class);
        if (ori == null) {
            return null;
        }
        Method method = ori.getMethodToInvoke();
        if (method.getAnnotation(AuthenticationRequired.class) != null
                && !authenticate(m, resourceClass)) {
            return Response.status(UNAUTHORIZED).build();
        }
        return null;
    }

    protected abstract boolean authenticate(Message m, ClassResourceInfo resourceClass);
}
