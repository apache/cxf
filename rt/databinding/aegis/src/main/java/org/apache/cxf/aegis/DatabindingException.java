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
package org.apache.cxf.aegis;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.cxf.common.i18n.Message;

/**
 * 
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Feb 14, 2004
 */
public class DatabindingException extends RuntimeException {
    
    private final List<String> extraMessages = new LinkedList<String>();
    

    /**
     * Constructs a new exception with the specified detail
     * message.
     * 
     * @param message the detail message.
     */
    public DatabindingException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail
     * message and cause.
     * 
     * @param message the detail message.
     * @param cause the cause.
     */
    public DatabindingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DatabindingException(Message message) {
        super(message.toString());
    }

    public DatabindingException(Message message, Throwable cause) {
        super(message.toString(), cause);
    }


    /**
     * Return the detail message, including the message from the
     * {@link #getCause() nested exception} if there is one.
     * 
     * @return the detail message.
     */
    public String getMessage() {
        if (getCause() == null || getCause() == this) {
            return getActualMessage();
        } else {
            return getActualMessage() + ". Nested exception is "
                   + getCause().getClass().getName() + ": "
                   + getCause().getMessage();
        }
    }

    public String getActualMessage() {
        if (extraMessages.isEmpty()) {
            return super.getMessage();
        }
        StringBuffer buf = new StringBuffer();
        for (String s : extraMessages) {
            buf.append(s);
        }
        buf.append(" ");
        buf.append(super.getMessage());
        return buf.toString();
    }

    /**
     * Prints this throwable and its backtrace to the specified print stream.
     * 
     * @param s <code>PrintStream</code> to use for output
     */
    @Override
    public void printStackTrace(PrintStream s) {
        if (getCause() == null || getCause() == this) {
            super.printStackTrace(s);
        } else {
            s.println(this);
            getCause().printStackTrace(s);
        }
    }

    /**
     * Prints this throwable and its backtrace to the specified print writer.
     * 
     * @param w <code>PrintWriter</code> to use for output
     */
    @Override
    public void printStackTrace(PrintWriter w) {
        if (getCause() == null || getCause() == this) {
            super.printStackTrace(w);
        } else {
            w.println(this);
            getCause().printStackTrace(w);
        }
    }

    public final void prepend(String m) {
        extraMessages.add(0, m + ": ");
    }

    public void setMessage(String s) {
        extraMessages.clear();
        extraMessages.add(s);
    }
}
