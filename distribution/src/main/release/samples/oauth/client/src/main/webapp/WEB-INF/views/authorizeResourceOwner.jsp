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
<h3>Step 2. Authorize Resource Owner</h3>

<form:form commandName="oAuthParams" action="/app/authorizeResourceOwner">
    <c:if test="${!empty oAuthParams.errorMessage}">
        <font color="red"><p>Error: ${oAuthParams.errorMessage}</p></font>
    </c:if>
    <table>
        <tr>
            <td>Response:</td>
        </tr>
        <tr>
            <td>OAuth Token:</td>
            <td><form:input size="70" path="oauthToken"/></td>
        </tr>
        <tr>
            <td>OAuth Token Secret:</td>
            <td><form:input size="70" path="oauthTokenSecret"/></td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
        </tr>
        <tr>
            <td>Required OAuth parameters:</td>
        </tr>
        <tr>
            <td>Resource Owner Authorization Endpoint URI:</td>
            <td><form:input size="70" path="resourceOwnerAuthorizationEndpoint"/></td>
        </tr>
        <tr>
            <td colspan="2">
                <input type="submit" value="Authorize Resource Owner"/>
            </td>
        </tr>
    </table>
</form:form>
</body>
</html>