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
package org.apache.cxf.rs.security.saml.assertion;

public class Subject {
    private String nameFormat;
    private String nameQualifier;
    private String name;
    private String spQualifier;
    private String spId;

    public Subject() {

    }

    public Subject(String name) {
        this.name = name;
    }

    public Subject(String nameFormat, String name) {
        this.nameFormat = nameFormat;
        this.name = name;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }
    public String getNameFormat() {
        return nameFormat;
    }
    public void setNameQualifier(String nameQualifier) {
        this.nameQualifier = nameQualifier;
    }
    public String getNameQualifier() {
        return nameQualifier;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void setSpId(String spId) {
        this.spId = spId;
    }

    public String getSpId() {
        return spId;
    }

    public void setSpQualifier(String spQualifier) {
        this.spQualifier = spQualifier;
    }

    public String getSpQualifier() {
        return spQualifier;
    }
}
