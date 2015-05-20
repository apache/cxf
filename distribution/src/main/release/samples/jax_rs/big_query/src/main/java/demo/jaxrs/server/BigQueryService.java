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
package demo.jaxrs.server;

import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.rs.security.oidc.rp.OidcClientTokenContext;

@Path("/service")
public class BigQueryService {

    @GET
    @Path("/bigquery/complete")
    @Produces("application/xml,application/json,text/html")
    public Response completeBigQuery(@Context OidcClientTokenContext context) {
        // This IdToken check can be skipped and UserInfo checked for null instead
        // given that UserInfo can only be obtained if IdToken is valid; shown here
        // to demonstrate the properties of OidcClientTokenContext
        IdToken idToken = context.getIdToken();
        if (idToken == null) {
            throw new NotAuthorizedException(Response.Status.UNAUTHORIZED);
        }
        
        UserInfo userInfo = context.getUserInfo();

        ResponseBuilder rb = Response.ok().type("application/json");
        Response r = rb.entity(
                "{\"email\":\"" + userInfo.getEmail() + "\"}")
                .build();
        return r;
    }
}
