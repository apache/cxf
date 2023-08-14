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

package org.apache.cxf.tracing.micrometer;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 *
 */
enum CxfObservationDocumentation implements ObservationDocumentation {

    /**
     * Observation created when a message is sent out.
     */
    OUT_OBSERVATION {
        @Override
        public Class<? extends ObservationConvention<? extends Context>> getDefaultConvention() {
            return DefaultMessageOutObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeys.values();
        }

    },

    /**
     * Observation created when a message is received.
     */
    IN_OBSERVATION {
        @Override
        public Class<? extends ObservationConvention<? extends Context>> getDefaultConvention() {
            return DefaultMessageInObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeys.values();
        }

    };

    enum LowCardinalityKeys implements KeyName {
        /**
         * A string identifying the remoting system.
         */
        RPC_SYSTEM {
            @Override
            public String asString() {
                return "rpc.system";
            }
        },

        /**
         * The full (logical) name of the service being called, including its package name, if applicable.
         */
        RPC_SERVICE {
            @Override
            public String asString() {
                return "rpc.service";
            }

            @Override
            public boolean isRequired() {
                return false;
            }
        },

        /**
         * The name of the (logical) method being called, must be equal to the $method part in the span name.
         */
        RPC_METHOD {
            @Override
            public String asString() {
                return "rpc.method";
            }

            @Override
            public boolean isRequired() {
                return false;
            }

        },

        /**
         * RPC server host name.
         */
        SERVER_ADDRESS {
            @Override
            public String asString() {
                return "server.address";
            }

            @Override
            public boolean isRequired() {
                return false;
            }

        },

        /**
         * Logical server port number.
         */
        SERVER_PORT {
            @Override
            public String asString() {
                return "server.port";
            }

            @Override
            public boolean isRequired() {
                return false;
            }

        }
    }
}
