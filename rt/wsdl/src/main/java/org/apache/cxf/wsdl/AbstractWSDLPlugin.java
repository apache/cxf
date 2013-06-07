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

package org.apache.cxf.wsdl;

import java.util.Map;
import javax.wsdl.extensions.ExtensionRegistry;

public abstract class AbstractWSDLPlugin implements WSDLExtensibilityPlugin {
    protected ExtensionRegistry registry;

    public void setExtensionRegistry(final ExtensionRegistry r) {
        this.registry = r;
    }

    public ExtensionRegistry getExtenstionRegistry() {
        return this.registry;
    }

    public boolean optionSet(final Map<String, Object> args, final String key) {
        return args.containsKey(key);
    }

    public String getOption(final Map<String, Object> args, final String key) {
        return args.get(key).toString();
    }

    public <T> T getOption(final Map<String, Object> args, final String key, final Class<T> clz) {
        return clz.cast(args.get(key));
    }

    public <T> T getOption(final Map<String, Object> args, final Class<T> clz) {
        return clz.cast(args.get(clz.getName()));
    }

    public String getOption(final Map<String, Object> args, final String key, final String defaultValue) {
        if (!optionSet(args, key)) {
            return defaultValue;
        }
        return getOption(args, key);
    }

}
