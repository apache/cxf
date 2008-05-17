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

package org.apache.cxf.systest.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/*
 * This interceptor will issue 401s
 *    No Authorization Header  --> 401 Realm=Cronus
 *    Username Mary            --> 401 Realm=Andromeda
 *    Username Edward          --> 401 Realm=Zorantius
 *    Username George          --> 401 Realm=Cronus
 *    If the password is not "password" a 401 is issued without 
 *    realm.
 */
public class PushBack401 extends AbstractPhaseInterceptor {
    
    PushBack401() {
        super(Phase.RECEIVE);
    }
    
    /**
     * This function extracts the user:pass token from 
     * the Authorization:Basic header. It returns a two element
     * String array, the first being the userid, the second
     * being the password. It returns null, if it cannot parse.
     */
    private String[] extractUserPass(String token) {
        try {
            byte[] userpass = Base64Utility.decode(token);
            String up = IOUtils.newStringFromBytes(userpass);
            String user = up.substring(0, up.indexOf(':'));
            String pass = up.substring(up.indexOf(':') + 1);
            return new String[] {user, pass};
        } catch (Exception e) {
            return null;
        }
        
    }
    
    /**
     * This function returns the realm which depends on 
     * the user name, as follows:
     * <pre>
     *    Username Mary            --> Andromeda
     *    Username Edward          --> Zorantius
     *    Username George          --> Cronus
     * </pre>
     * However, if the password is not "password" this function 
     * throws an exception, regardless.
     */
    private String checkUserPass(
        String user,
        String pass
    ) throws Exception {
        //System.out.println("Got user: " + user + " pass: " + pass);
        if (!"password".equals(pass)) {
            throw new Exception("bad password");
        }
        if ("Mary".equals(user)) {
            return "Andromeda";
        }
        if ("Edward".equals(user)) {
            return "Zorantius";
        }
        if ("George".equals(user)) {
            return "Cronus";
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public void handleMessage(Message message) throws Fault {
        
        Map<String, List<String>> headers =
            (Map<String, List<String>>) 
                message.get(Message.PROTOCOL_HEADERS);
        
        List<String> auth = headers.get("Authorization");
        if (auth == null) {
            // No Auth Header, respond with 401 Realm=Cronus
            replyUnauthorized(message, "Cronus");
            return;
        } else {
            for (String a : auth) {
                if (a.startsWith("Basic ")) {
                    String[] userpass = 
                        extractUserPass(a.substring("Basic ".length()));
                    if (userpass != null) {
                        try {
                            String realm = 
                                checkUserPass(userpass[0], userpass[1]);
                            if (realm != null) {
                                replyUnauthorized(message, realm);
                                return;
                            } else {
                                // Password is good and no realm
                                // We just return for successful fall thru.
                                return;
                            }
                        } catch (Exception e) {
                            // Bad Password
                            replyUnauthorized(message, null);
                            return;
                        }
                    }
                }
            }
            // No Authorization: Basic
            replyUnauthorized(message, null);
            return;
        }
    }
    
    /**
     * This function issues a 401 response back down the conduit.
     * If the realm is not null, a WWW-Authenticate: Basic realm=
     * header is sent. The interceptor chain is aborted stopping
     * the Message from going to the servant.
     */
    private void replyUnauthorized(Message message, String realm) {
        Message outMessage = getOutMessage(message);
        outMessage.put(Message.RESPONSE_CODE, 
                HttpURLConnection.HTTP_UNAUTHORIZED);
        
        if (realm != null) {
            setHeader(outMessage, 
                      "WWW-Authenticate", "Basic realm=" + realm);
        }
        message.getInterceptorChain().abort();
        try {
            getConduit(message).prepare(outMessage);
            close(outMessage);
        } catch (IOException e) {
            //System.out.println("Prepare of message not working." + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Retrieves/creates the corresponding Outbound Message.
     */
    private Message getOutMessage(Message message) {
        Exchange exchange = message.getExchange();
        Message outMessage = exchange.getOutMessage();
        if (outMessage == null) {
            Endpoint endpoint = exchange.get(Endpoint.class);
            outMessage = endpoint.getBinding().createMessage();
            exchange.setOutMessage(outMessage);
        }
        outMessage.putAll(message);
        return outMessage;
    }
    
    /**
     * This function sets the header in the PROTOCO_HEADERS of
     * the message.
     */
    @SuppressWarnings("unchecked")
    private void setHeader(Message message, String key, String value) {
        Map<String, List<String>> responseHeaders =
            (Map<String, List<String>>) 
                message.get(Message.PROTOCOL_HEADERS);
        if (responseHeaders != null) {
            responseHeaders.put(key, Arrays.asList(new String[] {value}));
        }
    }
    
    /**
     * This method retrieves/creates the conduit for the response
     * message.
     */
    private Conduit getConduit(Message message) throws IOException {
        Exchange exchange = message.getExchange();
        EndpointReferenceType target = 
            exchange.get(EndpointReferenceType.class);
        Conduit conduit =
            exchange.getDestination().getBackChannel(message, null, target);
        exchange.setConduit(conduit);
        return conduit;
    }
    
    /**
     * This method closes the output stream associated with the
     * message.
     */
    private void close(Message message) throws IOException {
        OutputStream os = message.getContent(OutputStream.class);
        os.flush();
        os.close();
    }

}
