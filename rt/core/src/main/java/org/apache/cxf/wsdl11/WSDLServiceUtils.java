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

package org.apache.cxf.wsdl11;

import javax.wsdl.Binding;
import javax.wsdl.extensions.ExtensibilityElement;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;

public final class WSDLServiceUtils {

    private WSDLServiceUtils() {

    }

    public static BindingFactory getBindingFactory(Binding binding, Bus bus, StringBuffer sb) {
        BindingFactory factory = null;
        for (Object obj : binding.getExtensibilityElements()) {
            if (obj instanceof ExtensibilityElement) {
                ExtensibilityElement ext = (ExtensibilityElement) obj;
                sb.delete(0, sb.length());
                sb.append(ext.getElementType().getNamespaceURI());
                try {
                    factory = bus.getExtension(BindingFactoryManager.class).getBindingFactory(sb.toString());
                } catch (BusException e) {
                    // ignore, we'll use a generic BindingInfo
                }

                if (factory != null) {
                    break;
                }
            }

        }

        return factory;
    }

}
