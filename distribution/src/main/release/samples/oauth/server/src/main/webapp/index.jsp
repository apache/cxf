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
<%--@elvariable id="oauthauthorizationdata" type="org.apache.cxf.rs.security.oauth.provider.OAuthAuthorizationData"--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<html>
<head><title>OAuth 1.0a CXF server</title></head>
<body>
<table align="center">
    <tr align="center">
        <td><h2>Sample CXF-OAuth 1.0a server implementation</h2></td>
    </tr>

    <tr align="center">
        <td><h3>OAuth protected resources at path: </h3></td>
    </tr>
    <tr align="center">
        <td><input size="70" value="/auth/resources/person/{name}"/><br/><br/>
            You can access this resources by using OAuth client hosted at: <a
                    href="http://www.oauthclient.appspot.com/">OAuth client</a></td>
    </tr>
</table>
<br/><br/>
<table align="center">
    <tr align="center">
        <td><h3>Login with Username and Password to register OAuth client</h3></td>
    </tr>

    <tr>
        <td>User: user1</td>
    </tr>
    <tr>
        <td>Password: 1111</td>
    </tr>
    <tr align="center">
        <td>
            <form name="f" action="/j_spring_security_check" method="POST">
                <c:if test="${not empty param.login_error}">
                    <font color="red">
                        Your login attempt was not successful, try again.<br/><br/>
                        Reason: <c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/>.
                    </font>
                </c:if>
                <label for="login">User</label>
                <input type="text" id="login" name='j_username'
                       value='<c:if test="${not empty param.login_error}"><c:out value="${SPRING_SECURITY_LAST_USERNAME}"/></c:if>'/>

                <div class="clear"></div>
                <label for="password">Password</label>
                <input type="password" id="password" name="j_password"/>
                <br>
                <input type="submit" class="button" name="commit" value="Log in"/>
            </form>
        </td>
    </tr>
</table>

</body>
</html>
