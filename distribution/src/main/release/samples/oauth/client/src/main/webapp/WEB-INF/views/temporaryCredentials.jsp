<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
-->
<%--@elvariable id="text" type="java.lang.String"--%>
<%--@elvariable id="oAuthParams" type="org.apache.cxf.auth.oauth.demo.client.model.OAuthParams"--%>
<%--@elvariable id="methods" type="java.util.List"--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>
<head>
    <title>OAuth 1.0a client</title>
</head>
<body>
<table align="center">
    <tr>
        <td><h2>Sample OAuth 1.0a client implementation</h2></td>
    </tr>
</table>

<p>

<h3>Step 1. Get OAuth temporary credentials</h3></p>

<form:form commandName="oAuthParams" action="/app/handleTemporaryCredentials">
    <c:if test="${!empty oAuthParams.errorMessage}">
        <font color="red"><p>Error: ${oAuthParams.errorMessage}</p></font>
    </c:if>
    <table>
        <tr>
            <td>Required OAuth parameters:</td>
        </tr>
        <tr>
            <td>Temporary Credentials Endoint URI:</td>
            <td><form:input size="70" path="temporaryCredentialsEndpoint"/></td>
        </tr>
        <tr>
            <td>Client Identifier:</td>
            <td><form:input size="70" path="clientID"/></td>
        </tr>
        <tr>
            <td>Client Shared-Secret:</td>
            <td><form:input size="70" path="clientSecret"/></td>
        </tr>
        <tr>
            <td>Callback URL:</td>
            <td><form:input size="70" path="callbackURL"/></td>
        </tr>
        <tr>
            <td>Signature Method:</td>
            <td>
                <form:select path="signatureMethod">
                    <form:options items="${oAuthParams.methods}" itemValue="methodName"
                                  itemLabel="methodName"/>
                </form:select>
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <input type="submit" value="Get Temporary Credentials"/>
            </td>
        </tr>
    </table>
</form:form>
</body>
</html>