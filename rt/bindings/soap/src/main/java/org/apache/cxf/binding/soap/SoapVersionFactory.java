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

package org.apache.cxf.binding.soap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SoapVersionFactory {
    private static SoapVersionFactory factory = new SoapVersionFactory();

    static {
        getInstance().register(Soap11.getInstance());
        getInstance().register(Soap12.getInstance());
    }

    private Map<String, SoapVersion> versions = new HashMap<>();

    public static SoapVersionFactory getInstance() {
        return factory;
    }

    public SoapVersion getSoapVersion(String namespace) {
        return versions.get(namespace);
    }

    public void register(SoapVersion version) {
        versions.put(version.getNamespace(), version);
    }

    public Iterator<SoapVersion> getVersions() {
        return versions.values().iterator();
    }
}
