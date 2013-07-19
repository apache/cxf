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

package org.apache.cxf.xkms.cache;

import java.io.Closeable;
import java.io.IOException;
import java.security.cert.X509Certificate;

public interface XKMSClientCache extends Closeable {

    /**
     * Store an X509Certificate in the Cache
     */
    void put(String key, X509Certificate certificate);

    /**
     * Get an X509Certificate from the cache matching the given key. Returns null if there
     * is no such certificate in the cache.
     */
    X509Certificate get(String key);
    
    void close() throws IOException;
}