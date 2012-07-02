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

package org.apache.cxf.tools.common;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
public class ToolErrorListener {
    private static final Logger LOG = LogUtils.getL7dLogger(ToolErrorListener.class);
    
    class ErrorInfo {
        File file;
        int line;
        int col;
        Throwable cause;
        String message;
        
        ErrorInfo(File f, int l, int c, String m, Throwable t) {
            file = f;
            line = l;
            col = c;
            message = m;
            cause = t;
        }
    }
    List<ErrorInfo> errors = new LinkedList<ErrorInfo>();
    
    public void addError(File file, int line, int column, String message) {
        addError(file, line, column, null);
    }
    public void addError(File file, int line, int column, String message, Throwable t) {
        errors.add(new ErrorInfo(file, line, column, message, t));
    }

    public void addWarning(File file, int line, int column, String message) {
        addWarning(file, line, column, null);
    }
    public void addWarning(File file, int line, int column, String message, Throwable t) {
        if (file != null) {
            message = file.getAbsolutePath() + " [" + line + "," + column + "]: " + message; 
        }
        LOG.warning(message);
    }

    public int getErrorCount() {
        return errors.size();
    }
    private StringBuilder createMessage(StringBuilder b, ToolErrorListener.ErrorInfo e) {
        if (e.file != null) {
            b.append(e.file.getAbsolutePath())
                .append(" [").append(e.line).append(',').append(e.col).append("]: ").append(e.message);
        } else if (e.message == null && e.cause != null) {
            b.append(e.cause.getLocalizedMessage());
        } else {
            b.append(e.message);
        }
        return b;
    }
    public void throwToolException() {
        StringBuilder b = new StringBuilder();
        for (ErrorInfo e : errors) {
            createMessage(b, e).append("\n");
        }
        throw new ToolException(b.toString(), errors);
    }
}
