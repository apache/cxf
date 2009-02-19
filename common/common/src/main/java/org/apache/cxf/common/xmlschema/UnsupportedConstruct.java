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
package org.apache.cxf.common.xmlschema;

import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;

/**
 * This exception is thrown when the Javascript client generator hits a schema
 * construct that it cannot handle. 
 */
public class UnsupportedConstruct extends RuntimeException {

    public UnsupportedConstruct() {
    }

    public UnsupportedConstruct(String explanation) {
        super(explanation);
    }
    
    public UnsupportedConstruct(Logger logger, String messageKey, Object...args) {
        super(new Message(messageKey, logger, args).toString());
        
    }

    public UnsupportedConstruct(Throwable cause) {
        super(cause);
    }
    
    public UnsupportedConstruct(Throwable cause, Logger logger, String messageKey, Object...args) {
        super(new Message(messageKey, logger, args).toString(), cause);
    }

    public UnsupportedConstruct(String explanation, Throwable cause) {
        super(explanation, cause);
    }

    public UnsupportedConstruct(Message message) {
        super(message.toString());
    }

}
