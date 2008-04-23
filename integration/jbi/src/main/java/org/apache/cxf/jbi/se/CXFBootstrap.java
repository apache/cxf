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


package org.apache.cxf.jbi.se;

import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.component.InstallationContext;
import javax.management.ObjectName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;

public class CXFBootstrap implements Bootstrap {

    private static final Logger LOG = LogUtils.getL7dLogger(CXFBootstrap.class);

    // Implementation of javax.jbi.component.Bootstrap

      
    public final ObjectName getExtensionMBeanName() {
        return null;
    }

    public final void cleanUp() throws JBIException {
        LOG.info(new Message("BOOTSTRAP.CLEANUP", LOG).toString());
      
    }

    public final void onInstall() throws JBIException {
        LOG.info(new Message("BOOTSTRAP.ONINSTALL", LOG).toString());

    }

    public final void onUninstall() throws JBIException {
        LOG.info(new Message("BOOTSTRAP.ONUNINSTALL", LOG).toString());
    }

    public final void init(final InstallationContext argCtx) throws JBIException {
        LOG.info(new Message("BOOTSTRAP.INIT", LOG).toString());
    }
}
