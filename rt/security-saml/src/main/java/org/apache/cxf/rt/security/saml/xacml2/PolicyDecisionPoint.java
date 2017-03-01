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

import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;

/**
 * An interface that describes a PolicyDecisionPoint (PDP) that supports XACML 2.0 using OpenSAML
 */
public interface PolicyDecisionPoint {

    /**
     * Evaluate an XACML Request and return a Response
     * @param request an XACML Request as a RequestType
     * @return the XACML Response as a ResponseType
     */
    ResponseType evaluate(RequestType request) throws Exception;

}
