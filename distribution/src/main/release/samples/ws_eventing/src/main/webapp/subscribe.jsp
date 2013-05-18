<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>New Subscription</title>
</head>
<body>

<h2>New subscription</h2>

<form action="CreateSubscriptionServlet">
    <ul>
        <li>Target URL to notify: <input name="targeturl" type="text" size="50"
                                         value="http://localhost:8080${pageContext.request.contextPath}/services/default"/>
        </li>
        <li>
            XPath filter: <input name="filter" type="text" size="30"
                                 value="//location[text()='Russia']"/><br/>
            <ul>
                <li>You will only receive events which conform to the specified XPath filter</li>
                <li>Right now only XPath 1.0 expressions are supported</li>
                <li>unset <input name="filter-set" type="checkbox" value="false"/>
                    - if you check this, you will receive all events
                </li>
            </ul>
        </li>
        <li>
            Requested expiration : <input id="expires" name="expires" type="text" size="30"
                                          value="2016-06-26T12:23:12.000-01:00"/>
            <ul>
                <li>Specify when you would like the subscription to expire. You can either provide a xs:duration or a xs:dateTime
                    (you can find the correct string formats <a href="http://www.w3schools.com/schema/schema_dtypes_date.asp">here</a>)</li>
                <li>A value of PT0S says that you wish the subscription to never expire</li>
                <li>unset <input name="expires-set" type="checkbox" value="false"/>
                    - if you check this, you let the server decide how long your subscription will last
                    <!-- onselect="Document.getElementById('expires').disabled = true; --></li>
            </ul>
        </li>
    </ul>
    <input type="submit" value="Submit"/>
</form>

<a href="index.jsp">Back to main page</a>

</body>
</html>