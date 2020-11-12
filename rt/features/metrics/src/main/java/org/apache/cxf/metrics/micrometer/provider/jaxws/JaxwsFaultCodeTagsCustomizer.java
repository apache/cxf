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

package org.apache.cxf.metrics.micrometer.provider.jaxws;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public class JaxwsFaultCodeTagsCustomizer implements TagsCustomizer {

    private final JaxwsTags jaxwsTags;
    private final JaxwsFaultCodeProvider jaxwsFaultCodeProvider;

    public JaxwsFaultCodeTagsCustomizer(JaxwsTags jaxwsTags, JaxwsFaultCodeProvider jaxwsFaultCodeProvider) {
        this.jaxwsTags = jaxwsTags;
        this.jaxwsFaultCodeProvider = jaxwsFaultCodeProvider;
    }

    @Override
    public Iterable<Tag> getAdditionalTags(Exchange ex, boolean client) {
        String faultCode = jaxwsFaultCodeProvider.getFaultCode(ex, client);
        return Tags.of(jaxwsTags.faultCode(faultCode));
    }
}
