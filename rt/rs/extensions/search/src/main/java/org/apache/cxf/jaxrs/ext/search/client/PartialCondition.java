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
package org.apache.cxf.jaxrs.ext.search.client;

import java.util.List;

/**
 * Part of fluent interface of {@link SearchConditionBuilder}.
 */
public interface PartialCondition {
    /** Get property of inspected entity type */
    Property is(String property);

    /** Conjunct multiple expressions */
    CompleteCondition and(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn);

    /** Disjunct multiple expressions */
    CompleteCondition or(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn);

    /** Conjunct multiple expressions */
    CompleteCondition and(List<CompleteCondition> conditions);

    /** Disjunct multiple expressions */
    CompleteCondition or(List<CompleteCondition> conditions);
}
