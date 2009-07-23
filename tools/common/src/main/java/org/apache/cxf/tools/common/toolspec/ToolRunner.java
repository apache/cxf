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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
public final class ToolRunner {
    private static final Logger LOG = LogUtils.getL7dLogger(ToolRunner.class);
    private ToolRunner() {
        // utility class - never constructed
    }

    public static void runTool(Class<? extends ToolContainer> clz, InputStream toolspecStream,
                               boolean validate, String[] args) throws Exception {
        runTool(clz, toolspecStream, validate, args, true);
    }
    
    public static void runTool(Class<? extends ToolContainer> clz, InputStream toolspecStream,
                               boolean validate, String[] args, OutputStream os) throws Exception {
        runTool(clz, toolspecStream, validate, args, true, null, os);
    }

    public static void runTool(Class<? extends ToolContainer> clz, InputStream toolspecStream,
                               boolean validate, String[] args, ToolContext context) throws Exception {
        runTool(clz, toolspecStream, validate, args, true, context, null);
    }

    public static void runTool(Class<? extends ToolContainer> clz,
                               InputStream toolspecStream,
                               boolean validate,
                               String[] args,
                               boolean exitOnFinish) throws Exception {
        runTool(clz, toolspecStream, validate, args, true, null, null);
    }
    
    public static void runTool(Class<? extends ToolContainer> clz,
                               InputStream toolspecStream,
                               boolean validate,
                               String[] args,
                               boolean exitOnFinish,
                               ToolContext context) throws Exception {
        runTool(clz, toolspecStream, validate, args, exitOnFinish, context, null);
    }
    
    public static void runTool(Class<? extends ToolContainer> clz,
                               InputStream toolspecStream,
                               boolean validate,
                               String[] args,
                               boolean exitOnFinish,
                               ToolContext context,
                               OutputStream os) throws Exception {

        ToolContainer container = null;

        try {
            Constructor<? extends ToolContainer> cons 
                = clz.getConstructor(
                                     new Class[] {
                                         ToolSpec.class
                                     });
            container = cons.newInstance(
                                        new Object[] {
                                            new ToolSpec(toolspecStream, validate)
                                        });
        } catch (Exception ex) {
            Message message = new Message("CLZ_CANNOT_BE_CONSTRUCTED", LOG, clz.getName());
            LOG.log(Level.SEVERE, message.toString());
            throw new ToolException(message, ex);
        }

        try {
            container.setArguments(args);
            if (os != null) {
                container.setErrOutputStream(os);
                container.setOutOutputStream(os);
            }
            container.setContext(context);
            container.execute(exitOnFinish);
        } catch (Exception ex) {
            throw ex;
        }

    }

}
