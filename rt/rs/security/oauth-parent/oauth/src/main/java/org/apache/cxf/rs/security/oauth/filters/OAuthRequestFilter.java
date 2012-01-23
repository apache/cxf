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
package org.apache.cxf.rs.security.oauth.filters;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import net.oauth.OAuthProblemException;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth.data.OAuthContext;
import org.apache.cxf.security.SecurityContext;

/**
 * JAX-RS OAuth filter which can be used to protect end user endpoints
 */
@Provider
public class OAuthRequestFilter extends AbstractAuthFilter implements RequestHandler {
    @Context
    private MessageContext mc;
   
    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        try {
            
            OAuthInfo info = handleOAuthRequest(mc.getHttpServletRequest());
            setSecurityContext(m, info);
            
        } catch (OAuthProblemException e) {
            return Response.status(401).header("WWW-Authenticate", "OAuth").build();
        } catch (Exception e) {
            return Response.status(401).header("WWW-Authenticate", "OAuth").build();
        }
        return null;
    }

    private void setSecurityContext(Message m, OAuthInfo info) {
        
        SecurityContext sc = createSecurityContext(mc.getHttpServletRequest(), info);
        m.setContent(SecurityContext.class, sc);
        m.setContent(OAuthContext.class, createOAuthContext(info));
        
    }
}
