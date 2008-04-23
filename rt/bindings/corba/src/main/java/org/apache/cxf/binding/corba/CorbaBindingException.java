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
package org.apache.cxf.binding.corba;

import javax.xml.ws.ProtocolException;

// NOTE: This exception provides basic functionality for throwing exceptions within the binding.
// At the momemnt, we just want to support the ability to throw a message (and accompanying 
// exception) but it may be necessary to break up this functionality into separate exceptions 
// or make this exception a bit more complex.
public class CorbaBindingException extends ProtocolException {

    public static final long serialVersionUID = 8493263228127324876L;

    public CorbaBindingException() {
        super();
    }

    public CorbaBindingException(String msg) {
        super(msg);
    }

    public CorbaBindingException(String msg, Throwable t) {
        super(msg, t);
    }

    public CorbaBindingException(Throwable t) {
        super(t);
    }
}
