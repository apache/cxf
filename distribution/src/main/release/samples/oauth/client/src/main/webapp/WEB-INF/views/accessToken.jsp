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
<%--@elvariable id="oAuthParams" type="org.apache.cxf.auth.oauth.demo.client.model.OAuthParams"--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>
<head>
    <title>OAuth Client</title>
</head>
<body>
<table align="center">
    <tr>
        <td><h2>Sample OAuth 1.0a client implementation</h2></td>
    </tr>
</table>
<h3>Step 4. Get Protected Resource</h3>

<form:form commandName="oAuthParams" action="/app/getProtectedResource">
    <table>
        <tr>
            <td>OAuth Token:</td>
            <td><form:input size="70" path="oauthToken"/></td>
        </tr>
        <tr>
            <td>OAuth Secret:</td>
            <td><form:input size="70" path="oauthTokenSecret"/></td>
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
            <td>GET Protected Resource, need scope: 'read_info'</td>
            <td><form:input size="70" path="getResourceURL"/></td>
        </tr>
        <tr>
            <td>POST Protected Resource, need scope: 'modify_info'</td>
            <td><form:input size="70" path="postResourceURL"/></td>
        </tr>
        <tr>
            <td>Signature Method:</td>
            <td><form:select path="signatureMethod">
                <form:options items="${oAuthParams.methods}" itemValue="methodName"
                              itemLabel="methodName"/>
            </form:select></td>
        </tr>
        <tr>
            <td colspan="2">
                <input type="submit" name="op" value="GET"/>
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <input type="submit" name="op" value="POST"/>
            </td>
        </tr>
    </table>
</form:form>

<c:if test="${!empty oAuthParams.resourceResponse}">
    <p><b>Response:</b> ${oAuthParams.resourceResponse}</p>
</c:if>
<c:if test="${!empty oAuthParams.header}">
    <p><b>Header:</b>${oAuthParams.header}</p>
</c:if>
<c:if test="${!empty oAuthParams.responseCode}">
    <p><b>Response Status:</b>${oAuthParams.responseCode}</p>
</c:if>
</body>
</html>