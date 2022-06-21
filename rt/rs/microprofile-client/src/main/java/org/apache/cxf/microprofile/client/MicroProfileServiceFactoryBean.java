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
package org.apache.cxf.microprofile.client;

import java.util.Collections;

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public class MicroProfileServiceFactoryBean extends JAXRSServiceFactoryBean {

    @Override
    protected ClassResourceInfo createResourceInfo(Class<?> cls, boolean isRoot) {
        return addClassResourceInfo(
            ResourceUtils.createClassResourceInfo(cls, cls, null, isRoot, true, getBus(),
                                                  Collections.singletonList(MediaType.APPLICATION_JSON_TYPE),
                                                  Collections.singletonList(MediaType.APPLICATION_JSON_TYPE)));
    }
}
