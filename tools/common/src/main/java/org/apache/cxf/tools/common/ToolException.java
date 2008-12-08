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
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;

/**
 * Exception used for unrecoverable error in a CXF tool.
 */
public class ToolException extends RuntimeException {

    
    private static final long serialVersionUID = -4418907917249006910L;
    public ToolException() {
        super();
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
}

