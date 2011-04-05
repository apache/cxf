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

package org.apache.cxf.binding.soap.blueprint;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapVersion;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

public class SoapVersionTypeConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Object sourceObject, ReifiedType targetType) {
        return SoapVersion.class.isAssignableFrom(targetType.getRawClass());
    }

    /**
     * {@inheritDoc}
     */
    public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
        if ("1.2".equals(sourceObject)) {
            return Soap12.getInstance();
        } else if ("1.1".equals(sourceObject)) {
            return Soap11.getInstance();
        }
        throw new IllegalArgumentException("Unimplemented SOAP version requested.");
    }
}
