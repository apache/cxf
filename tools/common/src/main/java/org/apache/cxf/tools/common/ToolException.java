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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;

/**
 * Exception used for unrecoverable error in a CXF tool.
 */
public class ToolException extends RuntimeException {
    private static final long serialVersionUID = -4418907917249006910L;
    List<Throwable> suppressed = new ArrayList<Throwable>(0);
    boolean hasSuppressed;
    
    public ToolException() {
        super();
    }
    public ToolException(String message, List<ToolErrorListener.ErrorInfo> e) {
        super(message);
        
        if (e.size() > 1) {
            for (ToolErrorListener.ErrorInfo er : e) {
                String ms = createMessage(er);
                if (ms != null
                    && er.cause != null
                    && ms.equals(er.cause.getLocalizedMessage())) {
                    addSuppressedThrowable(er.cause);
                } else if (ms == null && er.cause != null) {
                    addSuppressedThrowable(er.cause);
                } else {
                    addSuppressedThrowable(new ToolException(ms, er.cause));
                }
            }
        } else if (e.size() == 1) {
            initCause(e.get(0).cause);
        }
    }

    
    public ToolException(Message msg) {
        super(msg.toString());
    }
    public ToolException(String msg) {
        super(msg);
    }

    public ToolException(Message msg, Throwable t) {
        super(msg.toString(), t);
    }
    
    public ToolException(String msg, Throwable t) {
        super(msg, t);
    }

    public ToolException(Throwable t) {
        super(t);
    }
    /**
     * Construct message from message property bundle and logger.
     * @param messageId
     * @param logger
     */
    public ToolException(String messageId, Logger logger) {
        this(new Message(messageId, logger));
    }
    public ToolException(String messageId, Logger logger, Object ... args) {
        this(new Message(messageId, logger, args));
    }
    private String createMessage(ToolErrorListener.ErrorInfo e) {
        if (e.file != null) {
            return e.file.getAbsolutePath() + " [" + e.line + "," + e.col + "]: " + e.message; 
        }
        if (e.message == null && e.cause != null) {
            return e.cause.getLocalizedMessage();
        }
        return e.message;
    }
    public void printStackTrace(PrintStream ps) {
        if (!hasSuppressed) {
            super.printStackTrace(ps);
            return;
        }
        printStackTrace(ps, "", "");   
    }
    public void printStackTrace(PrintStream ps, String pfx, String cap) {
        ps.println(pfx + cap + this);
        StackTraceElement[] trace = super.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            ps.println(pfx + "\tat " + traceElement);
        }

        // Print suppressed exceptions, if any
        for (Throwable se : suppressed) {
            printThrowable(se, ps, pfx + "/t", "Suppressed: ");
        }

        // Print cause, if any
        Throwable ourCause = getCause();
        if (ourCause != null &&  ourCause != suppressed.get(0)) {
            printThrowable(ourCause, ps, pfx + "/t", "Caused by: ");
        }
    }    
    private void printThrowable(Throwable t, PrintStream ps, String pfx, String cap) {
        if (t instanceof ToolException) {
            ((ToolException)t).printStackTrace(ps, pfx, cap);
        } else {
            ps.println(pfx + cap + t);
            StackTraceElement[] trace = t.getStackTrace();
            for (StackTraceElement ste : trace) {
                ps.println(pfx + "\tat " + ste);
            }
            if (t.getCause() != null) {
                printThrowable(t.getCause(), ps, pfx + "\t", "Caused by: ");
            }
        }
        
    }

    
    private void addSuppressedThrowable(Throwable t) {
        try {
            this.getClass().getMethod("addSuppressed", Throwable.class).invoke(this, t);
        } catch (Throwable t2) {
            //java < 1.7
            suppressed.add(t2);
            if (getCause() == null) {
                initCause(t);
            }
            hasSuppressed = true;
        }
    }

}

