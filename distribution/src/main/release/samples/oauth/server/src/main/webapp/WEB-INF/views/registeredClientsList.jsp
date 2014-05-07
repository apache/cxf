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
<%--@elvariable id="clients" type="java.util.Set<org.apache.cxf.rs.security.oauth.provider.Client>"--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<html>
<head><title>Registered clients</title></head>
<body>
<h3>Registered clients:</h3>
<table>
    <c:forEach var="client" items="${clients}">
        <form:form action="/app/removeClient" commandName="client">
            <input type="hidden" name="consumerKey" value="${client.consumerKey}"/>
            <tr>
                <td>Application Name:</td>
                <td>${client.applicationName}</td>
            </tr>
            <tr>
                <td>Customer Key:</td>
                <td>${client.consumerKey}</td>
            </tr>
            <tr>
                <td>Consumer Secret:</td>
                <td>${client.secretKey}</td>
            </tr>
            <tr>
                <td>Callback URL:</td>
                <td>${client.callbackURI}</td>
            </tr>
            <tr>
                <td colspan="2">
                    <input type="submit" value="Remove Client"/>
                </td>
            </tr>
        </form:form>
        <tr>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
        </tr>
    </c:forEach>
    <tr>
        <form:form action="/app/newClientForm">
            <td colspan="2">
                <input type="submit" value="Register New Client"/>
            </td>
        </form:form>
    </tr>
</table>

</body>
</html>
