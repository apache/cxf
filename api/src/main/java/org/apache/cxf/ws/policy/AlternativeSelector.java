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

package org.apache.cxf.ws.policy;

import java.util.Collection;
import java.util.List;

import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

/**
 * Used by the Policy engine to select the Policy alternative to use.
 * 
 * By default, the Policy engine uses a "Minimal" policy alternative selector
 * that finds the alternative with the smallest Collection of Assertions to
 * assert.
 */
public interface AlternativeSelector {
 
    /**
     * 
     * @param policy The full policy to consider 
     * @param engine The policy engine calling the selector
     * @param assertor Additional asserter (such as the transport) that may be 
     *                 able to handle some of the assertions
     * @param request On the server out bound side, this will contain the alternatives
     *                from the request that were successfully met by the request.  The
     *                selector should use these to help narrow down the alternative to
     *                use.
     * @return
     */
    Collection<Assertion> selectAlternative(Policy policy, 
                                            PolicyEngine engine, 
                                            Assertor assertor, 
                                            List<List<Assertion>> request);
    
}
