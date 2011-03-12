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

package org.apache.cxf.management.web.logging;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.logging.LogUtils;

public class Generate extends HttpServlet {
    private static final Logger LOGGER = LogUtils.getL7dLogger(Generate.class);

    private static final String LEVEL = "level";
    private static final String MESSAGE = "message";
    private static final String COPIES = "copies";
    private static final String EXCEPTION_MESSAGE = "exceptionMessage";
    private static final String FORM_URL = "./generate.html";

    private enum Levels {
        DEBUG,
        INFO,
        WARNING,
        ERROR;
    }

    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {

        final String level = request.getParameter(LEVEL) != null
            ? request.getParameter(LEVEL).toLowerCase() : null;
        final String message = request.getParameter(MESSAGE);
        final String exceptionMessage = request.getParameter(EXCEPTION_MESSAGE);

        int copies;
        try {
            copies = Integer.valueOf(request.getParameter(COPIES));
            if (copies < 0) {
                copies = 1;
            }
        } catch (NumberFormatException e) {
            copies = 1;
        }

        assert copies >= 1;
        assert level != null && !"".equals(level);

        if (name(Levels.DEBUG).equals(level)) {
            log(Level.FINE, message, exceptionMessage, copies);
        } else if (name(Levels.INFO).equals(level)) {
            log(Level.INFO, message, exceptionMessage, copies);
        } else if (name(Levels.WARNING).equals(level)) {
            log(Level.WARNING, message, exceptionMessage, copies);
        } else if (name(Levels.ERROR).equals(level)) {
            log(Level.SEVERE, message, exceptionMessage, copies);
        }

        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", FORM_URL);
        response.setContentType("text/html");
    }

    private static void log(final Level level, final String message,
                            final String exceptionMessage, final int copies) {
        if (message != null && !"".equals(message)) {
            for (int i = 0; i < copies; i++) {
                if (exceptionMessage != null && !"".equals(exceptionMessage)) {
                    LOGGER.log(level, message, new Exception(exceptionMessage));
                } else {
                    LOGGER.log(level, message);
                }
            }
        }
    }

    private static String name(final Levels level) {
        return level.name().toLowerCase();
    }
}
