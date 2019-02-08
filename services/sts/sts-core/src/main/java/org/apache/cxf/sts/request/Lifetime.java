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
package org.apache.cxf.sts.request;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * This class contains values that have been extracted from a Lifetime element.
 */
public class Lifetime {
    private static final Logger LOG = LogUtils.getL7dLogger(Lifetime.class);

    private String created;
    private String expires;

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Found created value: " + created);
        }
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Found expires value: " + expires);
        }
    }

}
