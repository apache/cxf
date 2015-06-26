<%@ page import="javax.servlet.http.HttpServletRequest, org.apache.cxf.rs.security.oidc.rp.OidcClientTokenContext" %>

<%
    OidcClientTokenContext context = (OidcClientTokenContext) request.getAttribute("oidcclienttokencontext");
%>
<html>
<body>
<div class="padded">
<h2>Welcome, <%= context.getIdToken().getClaim("email") %></h2>
</div>
</body>
</html>
