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
package org.apache.cxf.jca.core.resourceadapter;

import java.util.*;
import java.util.logging.Logger;
import org.apache.cxf.common.logging.LogUtils;

public class UriHandlerInit {
    private static final String PACKAGE_PREFIX = "org.apache.cxf.jca.core";
    private static final Logger LOG = LogUtils.getL7dLogger(UriHandlerInit.class);
  

    public UriHandlerInit() {
        initUriHandlers(PACKAGE_PREFIX);
    }

    public UriHandlerInit(String prefix) {
        initUriHandlers(prefix);
    }

    protected final void initUriHandlers(String prefix) {
        Properties properties = System.getProperties();
        String s = properties.getProperty("java.protocol.handler.pkgs");

        if (s == null) {
            s = prefix;
        } else {
            if (s.indexOf(prefix) == -1) {
                s = prefix + "|" + s;
            }
        }

        System.setProperty("java.protocol.handler.pkgs", s);
        properties.put("java.protocol.handler.pkgs", s);

        LOG.fine("java.protocol.handler.pkgs=" + s);
    }
}
