<%@ page import="javax.servlet.http.HttpServletRequest, org.apache.cxf.rs.security.oidc.rp.OidcClientTokenContext" %>

<%
    OidcClientTokenContext oidc = (OidcClientTokenContext) request.getAttribute("data");
%>
<html>
<body>
<name><%= oidc.getIdToken().getClaim("name") %></name>
</body>
</html>
