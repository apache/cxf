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

package org.apache.cxf.tools.common.toolspec;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.ToolException;
public class ToolSupport implements Tool {

    private static final Logger LOG = LogUtils.getL7dLogger(ToolSupport.class);
    //private ToolContext ctx;

    public void init() throws ToolException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Initializing " + this);
        }
    }

    public void performFunction() throws ToolException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Performing function");
        }
    }

    public void destroy() throws ToolException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Destroying " + this);
        }
    }
}
