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

package org.apache.cxf.jibx.tools;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.jibx.schema.validation.ProblemHandler;
import org.jibx.schema.validation.ValidationProblem;

public class JibxToolingProblemHandler implements ProblemHandler {

    // We are using JiBXToolingDataBinding logger
    private static final Logger LOG = LogUtils.getLogger(JibxToolingDataBinding.class);

    public void handleError(ValidationProblem prob) {
        LOG.log(Level.SEVERE, prob.getDescription());
    }

    public void handleFatal(ValidationProblem prob) {
        LOG.log(Level.SEVERE, prob.getDescription());
    }

    public void handleUnimplemented(ValidationProblem prob) {
        LOG.log(Level.INFO, "Unimplemented feature - " + prob.getDescription());
    }

    public void handleWarning(ValidationProblem prob) {
        LOG.log(Level.WARNING, prob.getDescription());
    }

    public void report(String msg) {
        LOG.log(Level.INFO, msg);
    }

    public void terminate(String msg) {
        LOG.log(Level.SEVERE, msg);

    }
    
    public void terminate(String msg, Throwable thrown) {
        LOG.log(Level.SEVERE, msg, thrown);
    }
    
    public void handleSevere(String msg, Throwable thrown) {
        LOG.log(Level.SEVERE, msg, thrown);
    }


}
