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

import org.apache.cxf.common.injection.NoJSR250Annotations;

/**
 * This feature may be applied to a Client so as to enable
 * load distribution amongst a set of target endpoints or addresses
 * Note that this feature changes the conduit on the fly and thus makes
 * the Client not thread safe.
 */
@NoJSR250Annotations
public class LoadDistributorFeature extends FailoverFeature {

    public LoadDistributorFeature() {
        super(new Portable());
    }
    public LoadDistributorFeature(String clientBootstrapAddress) {
        super(new FailoverFeature.Portable(clientBootstrapAddress));
    }

    @Override
    public FailoverTargetSelector getTargetSelector() {
        return new LoadDistributorTargetSelector(getClientBootstrapAddress());
    }

    public static class Portable extends FailoverFeature.Portable {
        public Portable() {

        }
        public Portable(String clientBootstrapAddress) {
            super(clientBootstrapAddress);
        }

        @Override
        public FailoverTargetSelector getTargetSelector() {
            return new LoadDistributorTargetSelector(getClientBootstrapAddress());
        }
    }
}
