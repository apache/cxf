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
package demo.oauth.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OAuthParams implements Serializable {
    private String temporaryCredentialsEndpoint = "http://localhost:8081/auth/oauth/initiate";
    private String resourceOwnerAuthorizationEndpoint = "http://localhost:8081/auth/oauth/authorize";
    private String tokenRequestEndpoint = "http://localhost:8081/auth/oauth/token";
    private String getResourceURL = "http://localhost:8081/auth/resources/person/get/john";
    private String postResourceURL = "http://localhost:8081/auth/resources/person/modify/john";

    private String callbackURL = "http://localhost:8080/app/callback";

    private String clientID = "12345678";
    private String clientSecret = "secret";
    private String signatureMethod;

    private String oauthToken;
    private String oauthTokenSecret;
    private String oauthVerifier;

    private String errorMessage;
    private String resourceResponse;
    private String header;
    private Integer responseCode;

    private List<SignatureMethod> methods = new ArrayList<SignatureMethod>();

    public OAuthParams() {
        methods.add(new SignatureMethod("HMAC-SHA1"));
    }

    public OAuthParams(String clientSecret, String clientID) {
        super();
        this.clientSecret = clientSecret;
        this.clientID = clientID;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getSignatureMethod() {
        return signatureMethod;
    }

    public void setSignatureMethod(String signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    public String getTemporaryCredentialsEndpoint() {
        return temporaryCredentialsEndpoint;
    }

    public void setTemporaryCredentialsEndpoint(String temporaryCredentialsEndpoint) {
        this.temporaryCredentialsEndpoint = temporaryCredentialsEndpoint;
    }

    public String getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(String oauthToken) {
        this.oauthToken = oauthToken;
    }

    public String getOauthTokenSecret() {
        return oauthTokenSecret;
    }

    public void setOauthTokenSecret(String oauthTokenSecret) {
        this.oauthTokenSecret = oauthTokenSecret;
    }

    public String getResourceOwnerAuthorizationEndpoint() {
        return resourceOwnerAuthorizationEndpoint;
    }

    public void setResourceOwnerAuthorizationEndpoint(String resourceOwnerAuthorizationEndpoint) {
        this.resourceOwnerAuthorizationEndpoint = resourceOwnerAuthorizationEndpoint;
    }

    public String getTokenRequestEndpoint() {
        return tokenRequestEndpoint;
    }

    public void setTokenRequestEndpoint(String tokenRequestEndpoint) {
        this.tokenRequestEndpoint = tokenRequestEndpoint;
    }

    public String getOauthVerifier() {
        return oauthVerifier;
    }

    public void setOauthVerifier(String oauthVerifier) {
        this.oauthVerifier = oauthVerifier;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getGetResourceURL() {
        return getResourceURL;
    }

    public void setGetResourceURL(String getResourceURL) {
        this.getResourceURL = getResourceURL;
    }

    public String getCallbackURL() {
        return callbackURL;
    }

    public void setCallbackURL(String callbackURL) {
        this.callbackURL = callbackURL;
    }

    public String getResourceResponse() {
        return resourceResponse;
    }

    public void setResourceResponse(String resourceResponse) {
        this.resourceResponse = resourceResponse;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public List getMethods() {
        return methods;
    }

    public void setMethods(List<SignatureMethod> methods) {
        this.methods = methods;
    }

    public String getPostResourceURL() {
        return postResourceURL;
    }

    public void setPostResourceURL(String postResourceURL) {
        this.postResourceURL = postResourceURL;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    static class SignatureMethod {
        private String methodName;

        SignatureMethod(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }
    }
}
