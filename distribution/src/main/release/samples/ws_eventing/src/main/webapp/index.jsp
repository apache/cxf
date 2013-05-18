<%@ page import="demo.wseventing.ApplicationSingleton" %>
<%@ page import="demo.wseventing.eventapi.CatastrophicEventSinkImpl" %>
<%@ page import="org.apache.cxf.ws.eventing.backend.database.SubscriptionTicket" %>
<%@ page import="demo.wseventing.SingletonSubscriptionManagerContainer" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
</head>
<body>

    <h2>Event sinks</h2>
    <h3>List of registered event sinks</h3>
    <table border="1">
        <tr><td>URL</td><td>Is running?</td><td>Event log</td></tr>
        <%
            for (CatastrophicEventSinkImpl sink : ApplicationSingleton.getInstance().getEventSinks()) {
                %>
                    <tr>
                        <td><a href="<%=sink.getFullURL()%>"><%=sink.getFullURL()%></a></td>
                        <td><%=sink.isRunning()%></td>
                        <td><a href="eventlog.jsp?sink=<%=sink.getShortURL()%>">List events</a></td>
                    </tr>
                <%
            }
        %>
    </table>

    <h2>Subscriptions</h2>
    <a href="subscribe.jsp">Request a new subscription</a>
    <h3>List of existing subscriptions</h3>
    <table border="1">
        <tr><td>UUID</td><td>Target URL</td><td>Filter</td><td>Expires</td></tr>
        <%
            for (SubscriptionTicket ticket : SingletonSubscriptionManagerContainer.getInstance().getTickets()) {
        %>
        <tr>
            <td><%=ticket.getUuid()%></td>
            <td><a href="<%=ticket.getTargetURL()%>"><%=ticket.getTargetURL()%></a></td>
            <td><%=StringEscapeUtils.escapeHtml(ticket.getFilterString())%></td>
            <td><%=ticket.isNonExpiring() ? "never" : ticket.getExpires().toXMLFormat()%></td>
        </tr>
        <%
            }
        %>
    </table>

    <h2>Emit new events</h2>
    <form action="EarthquakeEvent">
        <h3>Earthquake event</h3>
        Location: <input type="text" name="location" value="Russia"/><br/>
        Strength on Richter scale (a float number): <input type="text" name="strength" value="5.1"/><br/>
        <input type="submit" value="Let the ground shake!">
    </form><br/>

    <form action="FireEvent">
        <h3>Fire event</h3>
        Location: <input type="text" name="location" value="Norway"/><br/>
        Severity (an int number): <input type="text" name="strength" value="8"/><br/>
        <input type="submit" value="Burn it down!">
    </form>

    List <a href="services">registered SOAP endpoints</a> in this webapp.<br />


</body>
</html>