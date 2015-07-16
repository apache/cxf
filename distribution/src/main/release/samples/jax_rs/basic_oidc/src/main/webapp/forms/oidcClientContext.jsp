<%@ page import="javax.servlet.http.HttpServletRequest, org.apache.cxf.rs.security.oidc.rp.OidcClientTokenContext" %>

<%
    OidcClientTokenContext oidc = (OidcClientTokenContext) request.getAttribute("data");
    String basePath = request.getContextPath() + request.getServletPath();
    if (!basePath.endsWith("/")) {
        basePath += "/";
    }
%>
<html xmlns="http://www.w3.org/1999/xhtml">
<div class="padded">
</div>
</body>
</html>
