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

package org.apache.cxf.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * Abstract base class factoring out common Conduit logic,
 * allowing non-decoupled transports to be written without any
 * regard for the decoupled back-channel or partial response logic.
 */
public abstract class AbstractConduit 
    extends AbstractObservable implements Conduit {

    protected final EndpointReferenceType target;

    public AbstractConduit(EndpointReferenceType t) {
        target = t;
    }

    /**
     * @return the reference associated with the target Destination
     */    
    public EndpointReferenceType getTarget() {
        return target;
    }

    /**
     * Retrieve the back-channel Destination.
     * 
     * @return the backchannel Destination (or null if the backchannel is
     * built-in)
     */
    public Destination getBackChannel() {
        return null;
    }

    /**
     * @param message for which content should be closed.
     */    
    public void close(Message msg) throws IOException {
        OutputStream os = msg.getContent(OutputStream.class);
        if (os != null) {
            os.close();
        }
        InputStream in = msg.getContent(InputStream.class);
        if (in != null) {
            in.close();
        }
    }
    
    /**
     * Close the conduit.
     */
    public void close() {
        // nothing to do by default
    }
    
    public String toString() {
        return "conduit: " + this.getClass() + System.identityHashCode(this)
               + "target: "
               +  ((getTarget() != null
                   && getTarget().getAddress() != null)
                   ? getTarget().getAddress().getValue()
                   : "null");
    }
}
