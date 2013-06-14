/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package demo.wseventing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.eventing.DeliveryType;
import org.apache.cxf.ws.eventing.ExpirationType;
import org.apache.cxf.ws.eventing.FilterType;
import org.apache.cxf.ws.eventing.ObjectFactory;
import org.apache.cxf.ws.eventing.Subscribe;
import org.apache.cxf.ws.eventing.SubscribeResponse;
import org.apache.cxf.ws.eventing.eventsource.EventSourceEndpoint;

@WebServlet(urlPatterns = "/CreateSubscriptionServlet")
public class CreateSubscriptionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        try {
            resp.getWriter().append("<html><body>");


            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(EventSourceEndpoint.class);
            factory.setAddress("http://localhost:8080/ws_eventing/services/EventSource");
            EventSourceEndpoint requestorClient = (EventSourceEndpoint)factory.create();

            String expires = null;
            if (req.getParameter("expires-set") == null) {
                expires = req.getParameter("expires");
            } else {
                if (!req.getParameter("expires-set").equals("false")) {
                    expires = req.getParameter("expires");
                }
            }

            Subscribe sub = createSubscribeMessage(req.getParameter("targeturl"),
                    req.getParameter("filter-set") == null ? req.getParameter("filter") : null,
                    expires);

            resp.getWriter().append("<h3>Subscription request</h3>");
            resp.getWriter().append(convertJAXBElementToStringAndEscapeHTML(sub));

            SubscribeResponse subscribeResponse = requestorClient.subscribeOp(sub);

            resp.getWriter().append("<h3>Response from Event Source</h3>");
            resp.getWriter().append(convertJAXBElementToStringAndEscapeHTML(subscribeResponse));

            resp.getWriter().append("<br/><a href=\"index.jsp\">Back to main page</a>");
            resp.getWriter().append("</body></html>");
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

    public Subscribe createSubscribeMessage(String targetURL, String filter, String expires)
        throws DatatypeConfigurationException {
        Subscribe sub = new Subscribe();


        // expires
        if (expires != null) {
            sub.setExpires(new ExpirationType());
            sub.getExpires().setValue(expires);
        }

        // delivery
        EndpointReferenceType eventSink = new EndpointReferenceType();
        AttributedURIType eventSinkAddr = new AttributedURIType();
        eventSinkAddr.setValue(targetURL);
        eventSink.setAddress(eventSinkAddr);
        sub.setDelivery(new DeliveryType());
        sub.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSink));

        // filter
        if (filter != null && filter.length() > 0) {
            sub.setFilter(new FilterType());
            sub.getFilter().getContent().add(filter);
        }

        return sub;
    }

    public String convertJAXBElementToStringAndEscapeHTML(Object o) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Subscribe.class.getPackage().getName());
        Marshaller m = jc.createMarshaller();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.marshal(o, baos);
        String unescaped = baos.toString();
        return StringEscapeUtils.escapeHtml(unescaped);
    }

}
