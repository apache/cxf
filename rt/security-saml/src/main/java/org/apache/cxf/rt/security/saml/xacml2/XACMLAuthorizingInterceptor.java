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

package org.apache.cxf.rt.security.saml.xacml2;

import org.apache.cxf.message.Message;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;

/**
 * An interceptor to perform an XACML 2.0 authorization request to a remote PDP using OpenSAML,
 * and make an authorization decision based on the response. It takes the principal and roles
 * from the SecurityContext, and uses the XACMLRequestBuilder to construct an XACML Request
 * statement.
 */
public class XACMLAuthorizingInterceptor extends AbstractXACMLAuthorizingInterceptor {

    private PolicyDecisionPoint pdp;

    public XACMLAuthorizingInterceptor(PolicyDecisionPoint pdp) {
        super();
        this.pdp = pdp;
    }

    @Override
    protected ResponseType performRequest(RequestType request, Message message) throws Exception {
        return this.pdp.evaluate(request);
    }

}
