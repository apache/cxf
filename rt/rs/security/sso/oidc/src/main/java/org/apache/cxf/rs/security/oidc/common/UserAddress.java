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
package org.apache.cxf.rs.security.oidc.common;

import java.util.Map;

import org.apache.cxf.jaxrs.json.basic.JsonMapObject;

public class UserAddress extends JsonMapObject {
    public static final String STREET = "street_address";
    public static final String POSTCODE = "postal_address";
    public static final String LOCALITY = "locality";
    public static final String COUNTRY = "country";
    public static final String REGION = "region";
    public static final String FORMATTED = "formatted";
    private static final long serialVersionUID = -6065703167243134105L;

    public UserAddress() {
    }

    public UserAddress(Map<String, Object> claims) {
        super(claims);
    }

    public void setStreet(String name) {
        setProperty(STREET, name);
    }
    public String getStreet() {
        return (String)getProperty(STREET);
    }
    public void setPostalCode(String name) {
        setProperty(POSTCODE, name);
    }
    public String getPostalCode() {
        return (String)getProperty(POSTCODE);
    }
    public void setLocality(String name) {
        setProperty(LOCALITY, name);
    }
    public String getLocality() {
        return (String)getProperty(LOCALITY);
    }
    public void setCountry(String name) {
        setProperty(COUNTRY, name);
    }
    public String getCountry() {
        return (String)getProperty(COUNTRY);
    }
    public void setRegion(String name) {
        setProperty(REGION, name);
    }
    public String getRegion() {
        return (String)getProperty(REGION);
    }
    public void setFormatted(String name) {
        setProperty(FORMATTED, name);
    }
    public String getFormatted() {
        return (String)getProperty(FORMATTED);
    }
}
