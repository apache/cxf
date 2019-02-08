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

public final class CachedConstants {

    /**
     * The directory name for storing the temporary files. None is specified by default.
     */
    public static final String OUTPUT_DIRECTORY_SYS_PROP =
        "org.apache.cxf.io.CachedOutputStream.OutputDirectory";

    /**
     * The directory name for storing the temporary files. None is specified by default.
     */
    public static final String OUTPUT_DIRECTORY_BUS_PROP =
        "bus.io.CachedOutputStream.OutputDirectory";

    /**
     * The threshold value in bytes to switch from memory to file caching. The default value is 128K for
     * CachedOutputStream and 64K for CachedWriter.
     */
    public static final String THRESHOLD_SYS_PROP =
        "org.apache.cxf.io.CachedOutputStream.Threshold";

    /**
     * The threshold value in bytes to switch from memory to file caching. The default value is 128K for
     * CachedOutputStream and 64K for CachedWriter.
     */
    public static final String THRESHOLD_BUS_PROP =
        "bus.io.CachedOutputStream.Threshold";

    /**
     * The data size in bytes to limit the maximum data size to be cached. No max size is set by default.
     */
    public static final String MAX_SIZE_SYS_PROP =
        "org.apache.cxf.io.CachedOutputStream.MaxSize";

    /**
     * The data size in bytes to limit the maximum data size to be cached. No max size is set by default.
     */
    public static final String MAX_SIZE_BUS_PROP =
        "bus.io.CachedOutputStream.MaxSize";

    /**
     * The cipher transformation name for encrypting the cached content. None is specified by default.
     */
    public static final String CIPHER_TRANSFORMATION_SYS_PROP =
        "org.apache.cxf.io.CachedOutputStream.CipherTransformation";

    /**
     * The cipher transformation name for encrypting the cached content. None is specified by default.
     */
    public static final String CIPHER_TRANSFORMATION_BUS_PROP =
        "bus.io.CachedOutputStream.CipherTransformation";

    private CachedConstants() {
        // complete
    }
}
