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
package org.apache.cxf.sts.request;

import javax.xml.ws.WebServiceContext;


/**
 * This interface controls whether the STS allows an authenticated user to get a token
 * OnBehalfOf or ActAs another token. The tokens should be taken from the TokenRequirements
 * object passed as a parameter.
 */
public interface DelegationHandler {
    
    /**
     * Returns true if delegation is allowed.
     * @param context WebServiceContext
     * @param tokenRequirements The parameters extracted from the request
     * @param appliesToAddress The AppliesTo address (if any)
     * @return true if delegation is allowed.
     */
    boolean isDelegationAllowed(
        WebServiceContext context,
        TokenRequirements tokenRequirements, 
        String appliesToAddress
    );
    
}