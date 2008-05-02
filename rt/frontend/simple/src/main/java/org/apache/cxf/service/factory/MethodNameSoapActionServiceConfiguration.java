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

package org.apache.cxf.service.factory;

import java.lang.reflect.Method;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.OperationInfo;

/**
 * Provide a SOAPAction header for the benefit of dusty toolkits
 * which still require them (e.g. Oracle OC4J).
 */
public class MethodNameSoapActionServiceConfiguration extends AbstractServiceConfiguration {
    /** {@inheritDoc}*/
    @Override
    public String getAction(OperationInfo op, Method method) {
        String action = op.getName().getLocalPart();
        if (StringUtils.isEmpty(action)) {
            action = method.getName();
        }
        return action; 
    }
}
