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
<c:choose>
    <c:when test="${!empty oauthauthorizationdata.oauthToken}">
        <table align="center">
            <tr align="center">
                <td>
                    <form name="f" action="/j_spring_security_check" method="POST">
                        <input type="hidden" name="oauth_token"
                               value="${oauthauthorizationdata.oauthToken}"/>
                        <input type="hidden"
                               name="<%=org.apache.cxf.rs.security.oauth.utils.OAuthConstants
                                   .AUTHENTICITY_TOKEN%>"
                               value="${oauthauthorizationdata.authenticityToken}"/>
                        <input type="hidden"
                               name="<%=org.apache.cxf.rs.security.oauth.utils.OAuthConstants
                                   .X_OAUTH_SCOPE%>"
                               value="<%=request.getParameter("x_oauth_scope")%>"/>

                        <p>The application <b>${oauthauthorizationdata.applicationName}</b> would like
                            the
                            ability to access and update your data on Sample OAuth CXF server:
                            <br/></p>
                        <br/>
                        <b>Permissions:</b>

                        <c:forEach items="${oauthauthorizationdata.permissions}" var="permission">
                            <li>${permission.description}</li>
                            URIs:
                            <c:forEach items="${permission.uris}" var="uri">
                               <li>${uri}</li>
                            </c:forEach>
                        </c:forEach>
                        <br/>
                        Please ensure that you trust this website with your information before
                        proceeding!
                        <c:if test="${not empty param.login_error}">
                            <font color="red">
                                Your login attempt was not successful, try again.<br/><br/>
                                Reason: <c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/>.
                            </font>
                        </c:if>
                        <br>
                        User: user2
                        <br>
                        Password: 2222
                        <br>
                        <label for="login">User</label>
                        <input type="text" id="login" name='j_username'
                               value='<c:if test="${not empty param.login_error}"><c:out
                                   value="${SPRING_SECURITY_LAST_USERNAME}"/></c:if>'/>

                        <div class="clear"></div>
                        <label for="password">Password</label>
                        <input type="password" id="password" name="j_password"/>
                        <br>
                        <button name="<%=org.apache.cxf.rs.security.oauth.utils.OAuthConstants
                            .AUTHORIZATION_DECISION_KEY%>"
                                type="submit"
                                value="<%=org.apache.cxf.rs.security.oauth.utils.OAuthConstants
                                    .AUTHORIZATION_DECISION_DENY%>">
                            Deny
                        </button>
                        <button name="<%=org.apache.cxf.rs.security.oauth.utils.OAuthConstants
                            .AUTHORIZATION_DECISION_KEY%>"
                                type="submit"
                                value="<%=org.apache.cxf.rs.security.oauth.utils.OAuthConstants
                                    .AUTHORIZATION_DECISION_ALLOW%>">
                            Allow
                        </button>
                    </form>
                </td>
            </tr>
        </table>
    </c:when>
    <c:otherwise>
        <h3>Invalid request</h3>
    </c:otherwise>
</c:choose>
</body>
</html>
