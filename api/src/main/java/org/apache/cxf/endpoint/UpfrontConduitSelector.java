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
 
package org.apache.cxf.endpoint;

import java.util.logging.Logger;

//import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;


/**
 * Strategy for eager upfront retreival of a Conduit to mediate an 
 * outbound message.
 */
public class UpfrontConduitSelector extends AbstractConduitSelector {

    private static final Logger LOG =
        LogUtils.getL7dLogger(UpfrontConduitSelector.class);
    
    /**
     * Normal constructor.
     */
    public UpfrontConduitSelector() {
        this(null);
    }
    
    /**
     * Constructor, allowing a specific conduit to override normal selection.
     * 
     * @param c specific conduit
     */
    public UpfrontConduitSelector(Conduit c) {
        super(c);
    }

    /**
     * Called prior to the interceptor chain being traversed.
     * 
     * @param message the current Message
     */
    public void prepare(Message message) {
        getSelectedConduit(message);
    }
    
    /**
     * Called when a Conduit is actually required.
     * 
     * @param message
     * @return the Conduit to use for mediation of the message
     */
    public Conduit selectConduit(Message message) {
        return getSelectedConduit(message);
    }
    
    /**
     * @return the logger to use
     */
    protected Logger getLogger() {
        return LOG;
    }
}
