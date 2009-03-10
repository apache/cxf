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

package org.apache.cxf.transport.http;

import java.net.URL;

import org.apache.cxf.message.Message;

/**
 * This abstract class is extended by developers who need HTTP Auth
 * functionality on the client side. It supplies Authorization 
 * information to an HTTPConduit.
 * <p>
 * The HTTPConduit will make a call to getPreemptiveAuthorization before
 * an HTTP request is made. The HTTPConduit will call on 
 * getAuthorizationForRealm upon getting a 401 HTTP Response with a
 * "WWW-Authenticate: Basic realm=????" header. 
 * <p>
 * A HTTPConduit keeps a reference to this HttpAuthSupplier for the life
 * of the HTTPConduit, unless changed out by dynamic configuration.
 * Therefore, an implementation of this HttpAuthSupplier may maintain
 * state for subsequent calls. 
 * <p>
 * For instance, an implementation may not provide a Authorization preemptively for 
 * a particular URL and decide to get the realm information from 
 * a 401 response in which the HTTPConduit will call getAuthorizationForReam for
 * that URL. Then this implementation may provide the Authorization for this
 * particular URL preemptively for subsequent calls to getPreemptiveAuthorization.
 */
public abstract class HttpAuthSupplier {
    
    /**
     * This field contains the logical name of this HttpBasicAuthSuppler.
     * This field is not assigned to be final, since an extension may be
     * Spring initialized as a bean, have an appropriate setLogicalName
     * method, and set this field.
     */
    protected String logicalName;
    
    /**
     * The default constructor assigns the class name as the LogicalName.
     *
     */
    protected HttpAuthSupplier() {
        logicalName = this.getClass().getName();
    }
    
    /**
     * This constructor assigns the LogicalName of this HttpBasicAuthSupplier.
     * 
     * @param name The Logical Name.
     */
    protected HttpAuthSupplier(String name) {
        logicalName = name;
    }
    
    /**
     * This method returns the LogicalName of this HttpBasicAuthSupplier.
     */
    public String getLogicalName() {
        return logicalName;
    }
    
    /**
     * If the supplier requires the request to be cached to be resent, return true
     */
    public boolean requiresRequestCaching() {
        return false;
    }
    
    /**
     * The HTTPConduit makes a call to this method before connecting
     * to the server behind a particular URL. If this implementation does not 
     * have a Authorization for this URL, it should return null.
     * 
     * @param conduit     The HTTPConduit making the call.
     * @param currentURL  The URL to which the request is to be made.
     * @param message     The CXF Message.
     * 
     * @return This method returns null if no Authorization is available.
     */
    public abstract String getPreemptiveAuthorization(
            HTTPConduit  conduit,
            URL     currentURL,
            Message message);
            
    /**
     * The HTTPConduit makes a call to this method if it
     * receives a 401 response to a particular URL for
     * a given message. The realm information is taken
     * from the "WWW-Authenticate: ???? realm=?????"
     * header. The current message may be retransmitted
     * if this call returns a Authorization. The current message will
     * fail with a 401 if null is returned. If no Authorization is available
     * for this particular URL, realm, and message, then null
     * should be returned.
     * 
     * @param conduit     The conduit making the call.
     * @param currentURL  The current URL from which the reponse came.
     * @param message     The CXF Message.
     * @param realm       The realm extraced from the basic auth header.
     * @param fullHeader  The full WWW-Authenticate header
     * @return
     */
    public abstract String getAuthorizationForRealm(
            HTTPConduit conduit,
            URL     currentURL,
            Message message,
            String  realm,
            String  fullHeader);
}
