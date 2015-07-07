<%@ page import="javax.servlet.http.HttpServletRequest, demo.jaxrs.server.BigQueryStart" %>

<%
    BigQueryStart bq = (BigQueryStart) request.getAttribute("data");
%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Shakespeare Text Search</title>
    <STYLE TYPE="text/css">
	<!--
	  input {font-family:verdana, arial, helvetica, sans-serif;font-size:20px;line-height:40px;}
	  div.padded {  
         padding-left: 5em;  
      } 
	-->
</STYLE>
</head>
<body>
<div class="padded">
<h1><%= bq.getUserName() %>, Welcome to Shakespeare Text Search Service</h1>
<em></em>
<p>
 <table>
     <form action="https://localhost:8080/bigquery/service/search/complete" method="POST">
        <tr>
            <td><big><big><big>Text Word:</big></big></big></td>
            <td>
              <input type="text" name="word" value="brave"/>
            </td>
        </tr>
        <tr>
            <td><big><big><big>Max Results:</big></big></big></td>
            <td>
              <input type="text" name="maxResults" value="10"/>
            </td>
        </tr>
        <tr>
            <td>
                &nbsp;
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <input type="submit" value="        Find Texts       "/>
            </td>
        </tr>
  </form>
 </table> 
</div> 
</body>
</html>
