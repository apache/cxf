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
package org.apache.cxf.xkms.x509.repo.ldap;

public class LdapSchemaConfig {
    private String certObjectClass = "inetOrgPerson";
    private String attrUID = "uid";
    private String attrIssuerID = "manager";
    private String attrSerialNumber = "employeeNumber";
    private String attrEndpoint = "labeledURI";
    private String attrCrtBinary = "userCertificate;binary";
    private String attrCrlBinary = "certificateRevocationList;binary";
    private String constAttrNamesCSV = "sn";
    private String constAttrValuesCSV = "X509 certificate";
    private String serviceCertRDNTemplate = "cn=%s,ou=services";
    private String serviceCertUIDTemplate = "uid=%s";
    private String trustedAuthorityFilter = "(&(objectClass=inetOrgPerson)(ou:dn:=CAs))";
    private String intermediateFilter = "(objectClass=*)";
    private String crlFilter = "(&(objectClass=inetOrgPerson)(ou:dn:=CAs))";

    public String getCertObjectClass() {
        return certObjectClass;
    }

    public void setCertObjectClass(String crtObjectClass) {
        this.certObjectClass = crtObjectClass;
    }

    public String getAttrUID() {
        return attrUID;
    }

    public void setAttrUID(String attrUID) {
        this.attrUID = attrUID;
    }

    public String getAttrIssuerID() {
        return attrIssuerID;
    }

    public void setAttrIssuerID(String attrIssuerID) {
        this.attrIssuerID = attrIssuerID;
    }

    public String getAttrSerialNumber() {
        return attrSerialNumber;
    }

    public void setAttrSerialNumber(String attrSerialNumber) {
        this.attrSerialNumber = attrSerialNumber;
    }

    public String getAttrCrtBinary() {
        return attrCrtBinary;
    }

    public void setAttrCrtBinary(String attrCrtBinary) {
        this.attrCrtBinary = attrCrtBinary;
    }

    public String getConstAttrNamesCSV() {
        return constAttrNamesCSV;
    }

    public void setConstAttrNamesCSV(String constAttrNamesCSV) {
        this.constAttrNamesCSV = constAttrNamesCSV;
    }

    public String getConstAttrValuesCSV() {
        return constAttrValuesCSV;
    }

    public void setConstAttrValuesCSV(String constAttrValuesCSV) {
        this.constAttrValuesCSV = constAttrValuesCSV;
    }

    public String getServiceCertRDNTemplate() {
        return serviceCertRDNTemplate;
    }

    public void setServiceCertRDNTemplate(String serviceCrtRDNTemplate) {
        this.serviceCertRDNTemplate = serviceCrtRDNTemplate;
    }

    public String getServiceCertUIDTemplate() {
        return serviceCertUIDTemplate;
    }

    public void setServiceCertUIDTemplate(String serviceCrtUIDTemplate) {
        this.serviceCertUIDTemplate = serviceCrtUIDTemplate;
    }

    public String getTrustedAuthorityFilter() {
        return trustedAuthorityFilter;
    }

    public void setTrustedAuthorityFilter(String trustedAuthorityFilter) {
        this.trustedAuthorityFilter = trustedAuthorityFilter;
    }

    public String getIntermediateFilter() {
        return intermediateFilter;
    }

    public void setIntermediateFilter(String intermediateFilter) {
        this.intermediateFilter = intermediateFilter;
    }

    public String getCrlFilter() {
        return crlFilter;
    }

    public void setCrlFilter(String crlFilter) {
        this.crlFilter = crlFilter;
    }

    public String getAttrCrlBinary() {
        return attrCrlBinary;
    }

    public void setAttrCrlBinary(String attrCrlBinary) {
        this.attrCrlBinary = attrCrlBinary;
    }

    public String getAttrEndpoint() {
        return attrEndpoint;
    }

    public void setAttrEndpoint(String attrEndpoint) {
        this.attrEndpoint = attrEndpoint;
    }

}
