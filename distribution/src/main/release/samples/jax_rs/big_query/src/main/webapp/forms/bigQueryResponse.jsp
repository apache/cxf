<%@ page import="javax.servlet.http.HttpServletRequest, demo.jaxrs.server.BigQueryResponse, demo.jaxrs.server.ShakespeareText" %>

<%
    BigQueryResponse bgResponse = (BigQueryResponse) request.getAttribute("data");
    String basePath = request.getContextPath() + request.getServletPath();
    if (!basePath.endsWith("/")) {
        basePath += "/";
    }
%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Shakespeare texts</title>
    <STYLE TYPE="text/css">
	<!--
	  div.padded {  
         padding-left: 15em;  
      }   
	-->
</STYLE>
</head>
<body>
<div class="padded">
<h2>Texts containing a word "<%= bgResponse.getSearchWord() %>"</h2>
<em></em>
<p>
<big><big>
<%= bgResponse.getUserName() %>, here is a list of texts:
</big></big>
</p>
<table border="1">
    <tr><th><big><big>Text</big></big></th><th><big><big>Date</big></big></th></tr> 
    <%
       for (ShakespeareText entry : bgResponse.getTexts()) {
    %>
       <tr>
           <td><big><big><%= entry.getText() %></big></big></td>
           <td><big><big><%= entry.getDate() %></big></big></td>
       </tr>
    <%   
       }
    %> 
</table>

<br/>
<p>
Back to <a href="<%= basePath %>service/search/start">Search Service</a>.
</p>
</big></big>
</div>
</body>
</html>
