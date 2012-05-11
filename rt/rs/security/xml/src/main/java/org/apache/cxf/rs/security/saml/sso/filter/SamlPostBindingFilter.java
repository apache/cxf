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
package org.apache.cxf.rs.security.saml.sso.filter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;

public class SamlPostBindingFilter extends AbstractServiceProviderFilter {
    
    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        if (checkSecurityContext(m)) {
            return null;
        } else {
            try {
                SamlRequestInfo info = createSamlRequestInfo(m);
                info.setIdpServiceAddress(getIdpServiceAddress());
                // This depends on RequestDispatcherProvider linking
                // SamlResponseInfo with the jsp page which will fill
                // in the XHTML form using SamlResponseInfo
                // in principle we could've built the XHTML form right here
                // but it will be cleaner to get that done in JSP
                
                // Note the view handler will also need to set a RelayState 
                // cookie
                
                return Response.ok(info)
                               .type("text/html")
                               .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                               .header("Pragma", "no-cache") 
                               .build();
                
            } catch (Exception ex) {
                throw new WebApplicationException(ex);
            }
        }
    }
    
    
}
