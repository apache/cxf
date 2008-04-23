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

import org.apache.cxf.message.Message;

/**
 * 
 * The HTTPConduit calls upon this object to establish trust just before a 
 * message within the HTTP Conduit is sent out. This object is based on
 * the implementation of HTTP Conduit using java.net.URL and
 * java.net.URLConnection implementations. 
 * 
 * The HttpURLConnection will be set up and connected, but no data
 * yet sent (at least according to the JDK 1.5 default implemenation), 
 * and in the case of an HttpsURLConnection (again with caveat on
 * particular java.net.HttpsURLConnection implemenation), the TLS handshake 
 * will be completed and certain TLS artifacts will be available.
 * <p>
 * Each MessageTrustDecider has a "logical" name that may be used in logging
 * to help ensure the proper trust decision is being made for particular
 * conduits.
 */
public abstract class MessageTrustDecider {
    
    /**
     * This field contains the "logical" name of this Message Trust Decider.
     * This field is not assigned to be final, since an extension may be
     * Spring initialized as a bean, have an appropriate setLogicalName
     * method, and set this field.
     */
    protected String logicalName;
    
    /**
     * This default constructor sets the "logical" name of the trust
     * decider to be its class name.
     */
    protected MessageTrustDecider() {
        logicalName = this.getClass().getName();
    }

    /**
     * This constructor is used to set the logical name of the 
     * trust decider.
     */
    protected MessageTrustDecider(String name) {
        logicalName = name;
    }
    
    /**
     * This method returns the logical name of this trust decider.
     * The name of the trust decider may be used in logging or auditing
     * to make sure that the proper trust decision is being implemented.
     */
    public String getLogicalName() {
        return logicalName;
    }
    
    /**
     * This method is called when a Message is about to be sent out
     * over an HTTPConduit. Its implementation must throw the specified
     * exception if the URL connection cannot be trusted for the 
     * message.
     * <p>
     * It is important to note that the Message structure at this point
     * may not have any content, so any analysis of message content 
     * may be impossible.
     * <p>
     * This method gets invoked after URL.setRequestProperties() is called
     * on the URL for the selected protocol.
     * <P>
     * The HTTPConduit calls this message on every redirect, however, it is
     * impossible to tell where it has been redirected from.
     * 
     * TODO: What are the exising Message Properties at the point of this call?
     * 
     * @param conduitName    This parameter contains the logical name 
     *                       for the conduit that this trust decider
     *                       is being called from.
     *                       
     * @param connectionInfo This parameter contains information about
     *                       the URL Connection. It may be subclassed depending
     *                       on the protocol used for the URL. For "https",
     *                       this argument will be a HttpsURLConnectionInfo. 
     *                       For "http", this argument will be 
     *                       HttpURLConnectionInfo.
     *                       
     * @param message        This parameter contains the Message structure
     *                       that governs where the message may be going.
     * 
     * @throws UntrustedURLConnectionIOException
     *                     The trust decider throws this exception if 
     *                     trust in the URLConnection cannot be established
     *                     for the particular Message.
     *                     
     * @see HttpURLConnectionInfo
     * @see HttpsURLConnectionInfo
     */

    public abstract void establishTrust(
        String             conduitName,
        URLConnectionInfo  connectionInfo,
        Message            message
    ) throws 
        UntrustedURLConnectionIOException;
}
