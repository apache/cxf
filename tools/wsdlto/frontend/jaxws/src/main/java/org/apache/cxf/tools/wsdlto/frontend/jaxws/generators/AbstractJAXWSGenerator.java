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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.generators;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.core.AbstractGenerator;

public abstract class AbstractJAXWSGenerator extends AbstractGenerator {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractJAXWSGenerator.class);
    protected static final String TEMPLATE_BASE = "org/apache/cxf/tools/wsdlto/frontend/jaxws/template";

    public abstract boolean passthrough();
    
    public abstract void generate(ToolContext penv) throws ToolException;

    public void register(final ClassCollector collector, String packageName, String fileName) {
        // do nothing
    }
}
