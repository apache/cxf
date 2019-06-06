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

package org.apache.cxf.feature.validation;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * A feature to configure schema validation at the operation level, as an alternative to
 * using the @SchemaValidation annotation.
 */
public class SchemaValidationFeature extends DelegatingFeature<SchemaValidationFeature.Portable> {
    public SchemaValidationFeature(final SchemaValidationTypeProvider provider) {
        super(new Portable(provider));
    }

    public static class Portable implements AbstractPortableFeature {
        private final SchemaValidationTypeProvider provider;

        public Portable(final SchemaValidationTypeProvider provider) {
            this.provider = provider;
        }

        public void initialize(Server server, Bus bus) {
            initialise(server.getEndpoint());
        }

        public void initialize(Client client, Bus bus) {
            initialise(client.getEndpoint());
        }

        @Override
        public void doInitializeProvider(InterceptorProvider interceptorProvider, Bus bus) {
            // no-op
        }

        private void initialise(Endpoint endpoint) {
            for (BindingOperationInfo bop : endpoint.getEndpointInfo().getBinding().getOperations()) {
                SchemaValidationType type = provider.getSchemaValidationType(bop.getOperationInfo());
                if (type != null) {
                    bop.getOperationInfo().setProperty(Message.SCHEMA_VALIDATION_TYPE, type);
                }
            }
        }
    }
}
