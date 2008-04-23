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

package org.apache.cxf.tools.validator.internal;

import java.util.Stack;
import org.apache.cxf.common.i18n.Message;

public final class ValidationResult {

    private Stack<String> errors = new Stack<String>();
    private Stack<String> warnings = new Stack<String>();

    public Stack<String> getErrors() {
        return this.errors;
    }

    public Stack<String> getWarnings() {
        return this.warnings;
    }

    public void addError(final Message msg) {
        addError(msg.toString());
    }
    
    public void addError(final String error) {
        this.errors.push(error);
    }

    public void addWarning(final Message msg) {
        addWarning(msg.toString());
    }

    public void addWarning(final String warning) {
        this.warnings.push(warning);
    }

    public boolean hasWarnings() {
        return warnings.size() > 0;
    }
    
    public boolean isSuccessful() {
        return errors.size() == 0 && warnings.size() == 0;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n Summary: ");
        sb.append(" Failures: ");
        sb.append(errors.size());
        sb.append(", Warnings: ");
        sb.append(warnings.size());
        if (errors.size() > 0) {
            sb.append("\n\n <<< ERROR! \n");
            while (!errors.empty()) {
                sb.append(errors.pop());
                sb.append("\n");
            }
        }
        if (warnings.size() > 0) {
            sb.append("\n <<< WARNING! \n");
            while (!warnings.empty()) {
                sb.append(warnings.pop());
                sb.append("\n");                    
            }
        }
        return sb.toString();
    }    
}
