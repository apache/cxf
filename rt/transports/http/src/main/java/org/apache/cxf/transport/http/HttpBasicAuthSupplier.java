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

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.message.Message;

/**
 * This abstract class is extended by developers who need HTTP Basic Auth
 * functionality on the client side. It supplies userid and password
 * combinations to an HTTPConduit.
 * <p>
 * The HTTPConduit will make a call to getPreemptiveUserPass before
 * an HTTP request is made. The HTTPConduit will call on 
 * getUserPassForRealm upon getting a 401 HTTP Response with a
 * "WWW-Authenticate: Basic realm=????" header. 
 * <p>
 * A HTTPConduit keeps a reference to this HttpBasicAuthSupplier for the life
 * of the HTTPConduit, unless changed out by dynamic configuration.
 * Therefore, an implementation of this HttpBasicAuthSupplier may maintain
 * state for subsequent calls. 
 * <p>
 * For instance, an implemenation may not provide a UserPass preemptively for 
 * a particular URL and decide to get the realm information from 
 * a 401 response in which the HTTPConduit will call getUserPassForReam for
 * that URL. Then this implementation may provide the UserPass for this
 * particular URL preemptively for subsequent calls to getPreemptiveUserPass.
 */
public abstract class HttpBasicAuthSupplier extends HttpAuthSupplier {
    
    /**
     * The default constructor assigns the class name as the LogicalName.
     *
     */
    protected HttpBasicAuthSupplier() {
    }
    
    /**
     * This constructor assigns the LogicalName of this HttpBasicAuthSupplier.
     * 
     * @param name The Logical Name.
     */
    protected HttpBasicAuthSupplier(String name) {
        super(name);
    }
    
    @Override
    public String getAuthorizationForRealm(HTTPConduit conduit, URL currentURL, Message message,
                                           String realm, String fullHeader) {
        
        UserPass up = getUserPassForRealm(conduit.getConduitName(),
                                          currentURL,
                                          message,
                                          realm);
        if (up != null) {
            String key = up.getUserid() + ":" + up.getPassword();
            return "Basic " + Base64Utility.encode(key.getBytes());
        }
        return null;
    }
    @Override
    public String getPreemptiveAuthorization(HTTPConduit conduit, URL currentURL, Message message) {
        UserPass up = getPreemptiveUserPass(conduit.getConduitName(),
                                            currentURL,
                                            message);
        if (up != null) {
            String key = up.getUserid() + ":" + up.getPassword();
            return "Basic " + Base64Utility.encode(key.getBytes());
        }
        return null;
    }

    /**
     * This class is used to return the values of the 
     * userid and password used in the HTTP Authorization
     * Header. 
     */
    public static final class UserPass {
        private final String userid;
        private final String password;
        
        /**
         * This constructor forms the userid and password pair for 
         * the HTTP Authorization header.
         * 
         * @param user The userid that will be returned from getUserid().
         *             This argument must not contain a colon (":"). If
         *             it does, it will throw an IllegalArgumentException.
         *             
         * @param pass The password that will be returned from getPassword().
         */
        UserPass(String user, String pass) {
            if (user.contains(":")) {
                throw new IllegalArgumentException(
                                 "The argument \"user\" cannot contain ':'.");
            }
            userid   = user;
            password = pass;
        }
        /**
         * This method returns the userid.
         */
        public String getUserid() {
            return userid;
        }
        /**
         * This method returns the password.
         */
        public String getPassword() {
            return password;
        }
    }
    
    /**
     * This method is used by extensions of this class to create
     * a UserPass to return.
     * @param userid   The userid that will be returned from getUserid().
     *                 This argument must not contain a colon (":"). If
     *                 it does, it will throw an IllegalArgumentException.
     * @param password The password that will be returned from getPassword().
     * @return
     */
    protected UserPass createUserPass(
        final String userid,
        final String password
    ) {
        return new UserPass(userid, password);
    }
    /**
     * The HTTPConduit makes a call to this method before connecting
     * to the server behind a particular URL. If this implementation does not 
     * have a UserPass for this URL, it should return null.
     * 
     * @param conduitName The HTTPConduit making the call.
     * @param currentURL  The URL to which the request is to be made.
     * @param message     The CXF Message.
     * 
     * @return This method returns null if no UserPass is available.
     */
    public abstract UserPass getPreemptiveUserPass(
            String  conduitName,
            URL     currentURL,
            Message message);
            
    /**
     * The HTTPConduit makes a call to this method if it
     * receives a 401 response to a particular URL for
     * a given message. The realm information is taken
     * from the "WWW-Authenticate: Basic realm=?????"
     * header. The current message may be retransmitted
     * if this call returns a UserPass. The current message will
     * fail with a 401 if null is returned. If no UserPass is available
     * for this particular URL, realm, and message, then null
     * should be returned.
     * 
     * @param conduitName The name of the conduit making the call.
     * @param currentURL  The current URL from which the reponse came.
     * @param message     The CXF Message.
     * @param realm       The realm extraced from the basic auth header.
     * @return
     */
    public abstract UserPass getUserPassForRealm(
            String  conduitName,
            URL     currentURL,
            Message message,
            String  realm);
}
