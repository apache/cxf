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
package org.apache.cxf.clustering;

/**
 * Marker interface for {@link FailoverStrategy} implementations that carry
 * per-invocation mutable state (e.g. a retry counter).
 *
 * <p>When {@link FailoverTargetSelector} detects that the configured strategy
 * implements this interface it calls {@link #newStrategy()} at the start of
 * each top-level invocation and uses the returned instance for all failover
 * decisions belonging to that invocation.  The shared bean is therefore never
 * mutated during normal processing and is safe to configure as a singleton.
 */
public interface PerInvocationFailoverStrategy {

    /**
     * Returns a new {@link FailoverStrategy} instance pre-configured with the
     * same settings as {@code this} but with a fresh, zeroed mutable state.
     * Called once per top-level invocation by {@link FailoverTargetSelector}.
     */
    FailoverStrategy newStrategy();
}
