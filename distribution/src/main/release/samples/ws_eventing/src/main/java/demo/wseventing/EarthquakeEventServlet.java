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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import demo.wseventing.eventapi.EarthquakeEvent;

@WebServlet(urlPatterns = "/EarthquakeEvent")
public class EarthquakeEventServlet  extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        float strength = Float.parseFloat(req.getParameter("strength"));
        EarthquakeEvent event = new EarthquakeEvent(strength, req.getParameter("location"));
        NotificatorServiceHolder.getInstance().dispatchEvent(event);
        resp.getWriter().append("<html><body>");
        resp.getWriter().append("Event ").append(event.toString()).append(" emitted successfully!");
        resp.getWriter().append("<br/><a href=\"index.jsp\">Back to main page</a>");
        resp.getWriter().append("</body></html>");
    }
}
