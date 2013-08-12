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

package org.apache.cxf.configuration;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.i18n.UncheckedException;

public class ConfigurationException extends UncheckedException {

    private static final long serialVersionUID = 1L;


    /**
     * Constructs a <code>ConfigurationException</code> with the provided detail message.
     */
    public ConfigurationException(Message msg) {
        super(msg);
    }

    /**
     * Constructs a <code>ConfigurationException</code> with the detail message and cause
     * provided.
     */
    public ConfigurationException(Message msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a <code>ConfigurationException</code> with the provided cause.
     */
    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
