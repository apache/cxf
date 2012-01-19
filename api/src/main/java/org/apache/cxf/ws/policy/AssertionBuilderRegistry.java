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

import org.apache.neethi.AssertionBuilderFactory;

/**
 * AssertionBuilderRegistry is used to manage AssertionBuilders and
 * create Assertion objects from given xml elements.
 */
public interface AssertionBuilderRegistry extends AssertionBuilderFactory {
    
    /**
     * Indicates if unknown assertions should simply be ignored.
     * If set to false, the policy engine will throw an exception upon
     * encountering an assertion type for which no AssertionBuilder
     * has been registered.
     * @return false if unknown assertions will result in an exception 
     */
    boolean isIgnoreUnknownAssertions();
    
    /**
     * Indicates if unknown assertions should simply be ignored.
     * If set to false, the policy engine will throw an exception upon
     * encountering an assertion type for which no AssertionBuilder
     * has been registered. 
     * @param ignoreUnknownAssertions if unknown assertions should be ignored
     */
    void setIgnoreUnknownAssertions(boolean ignoreUnknownAssertions);
}
