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

package org.apache.cxf.io;

import java.io.Closeable;

/**
 * The {@link Bus} extension to clean up unclosed {@link CachedOutputStream} instances (and alike) backed by
 * temporary files (leading to disk fill, see https://issues.apache.org/jira/browse/CXF-7396. 
 */
public interface CachedOutputStreamCleaner {
    /**
     * Run the clean up
     */
    void clean();

    /**
     * Register the stream instance for the clean up
     */
    void unregister(Closeable closeable);

    /**
     * Unregister the stream instance from the clean up (closed properly)
     */
    void register(Closeable closeable);
}
