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
package org.apache.cxf.jaxrs.validation;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;


public final class ValidationUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(ValidationUtils.class);
    private ValidationUtils() {
    }
    public static Object getResourceInstance(Message message) {
        final OperationResourceInfo ori = message.getExchange().get(OperationResourceInfo.class);
        if (ori == null) {
            return null;
        }
        if (!ori.getClassResourceInfo().isRoot()) {
            return message.getExchange().get("org.apache.cxf.service.object.last");
        }
        final ResourceProvider resourceProvider = ori.getClassResourceInfo().getResourceProvider();
        if (!resourceProvider.isSingleton()) {
            String error = "Service object is not a singleton, use a custom invoker to validate";
            LOG.warning(error);
            return null;
        }
        return resourceProvider.getInstance(message);
    }


}
