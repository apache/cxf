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

package org.apache.cxf.ext.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables message Logging
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface Logging {
    /**
     * The size limit at which messages are truncated in the log
     */
    int limit() default AbstractLoggingInterceptor.DEFAULT_LIMIT;

    /**
     * Size limit when messages are written to disk.
     * -1 means do not write to disk.
     */
    int inMemThresHold() default AbstractLoggingInterceptor.DEFAULT_THRESHOLD;

    /**
     * For XML content, turn on pretty printing in the logs
     */
    boolean pretty() default false;

    /**
     * Log binary payloads by default
     */
    boolean logBinary() default false;

    /**
     * Log multipart payloads by default
     */
    boolean logMultipart() default true;
}

