<%@ page import="demo.wseventing.eventapi.CatastrophicEventSinkImpl" %>
<%@ page import="demo.wseventing.SingletonSubscriptionManagerContainer" %>
<%@ page import="demo.wseventing.ApplicationSingleton" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
</head>
<body>

<%
    String sinkURL = request.getParameter("sink");
    CatastrophicEventSinkImpl sink = ApplicationSingleton.getInstance().getEventSinkByURL(sinkURL);
    if (sink == null) {
        throw new RuntimeException("No such event sink: " + sinkURL);
    }
%>

<h2>Event sink with URL: <%= sink.getFullURL() %></h2>
<table border="1">
    <tr><td>Event Class</td><td>toString</td></tr>
    <%
        for (Object o : sink.getReceivedEvents()) {
    %>
    <tr>
        <td><%=o.getClass().getSimpleName()%></td>
        <td><%=o.toString()%></td>
    </tr>
    <%
        }
    %>
</table>

<a href="index.jsp">Back to main page</a>



</body>
</html>