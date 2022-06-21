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
package org.apache.cxf.rs.security.oauth2.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

/**
 * Represents a login name which AuthorizationService
 * may capture after the end user approved a given third party request
 */
@XmlRootElement
@Entity
public class UserSubject implements Serializable {

    private static final long serialVersionUID = -1469694589163385689L;

    private String login;
    private String id;
    private List<String> roles = new LinkedList<>();
    private Map<String, String> properties = new HashMap<>();
    private AuthenticationMethod am;

    public UserSubject() {
        this.id = newId();
    }

    public UserSubject(String login) {
        this();
        this.login = login;
    }

    public UserSubject(String login, List<String> roles) {
        this();
        this.login = login;
        this.roles = roles;
    }

    public UserSubject(String login, String id) {
        this.login = login;
        this.id = id != null ? id : newId();
    }

    public UserSubject(String login, String id, List<String> roles) {
        this.login = login;
        this.id = id != null ? id : newId();
        this.roles = roles;
    }

    public UserSubject(UserSubject sub) {
        this(sub.getLogin(), sub.getId(), sub.getRoles());
        this.properties = sub.getProperties();
        this.am = sub.getAuthenticationMethod();
    }

    private String newId() {
        return Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(16));
    }

    /**
     * Return the user login name
     *
     * @return the login name
     */
    public String getLogin() {
        return login;
    }

    /**
     * Set the user login name
     *
     * @param login the login name
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Return the optional list of user roles which may have
     * been captured during the authentication process
     *
     * @return the list of roles
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Set the optional list of user roles which may have
     * been captured during the authentication process
     *
     * @param roles the list of roles
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    /**
     * Get the list of additional user subject properties
     *
     * @return the list of properties
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "name")
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the list of additional user subject properties
     *
     * @param properties the properties
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Get the user's unique id
     *
     * @return the user's id
     */
    @Id
    public String getId() {
        return this.id;
    }

    /**
     * Set the users unique id
     *
     * @param id the user's id
     */
    public void setId(String id) {
        this.id = id;
    }

    public AuthenticationMethod getAuthenticationMethod() {
        return am;
    }

    public void setAuthenticationMethod(AuthenticationMethod method) {
        this.am = method;
    }

}
