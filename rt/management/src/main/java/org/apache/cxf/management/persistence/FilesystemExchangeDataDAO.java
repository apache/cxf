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
package org.apache.cxf.management.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

public class FilesystemExchangeDataDAO implements ExchangeDataDAO {

    private static final Logger LOG = LogUtils.getL7dLogger(FilesystemExchangeDataDAO.class);

    private String directory;

    private String extension = "txt";

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void save(ExchangeData exchange) throws Exception {
        File file = null;

        if (this.directory == null) {
            file = File.createTempFile("cxf-management-", "." + this.extension);
        } else {
            file = File.createTempFile("cxf-management-", "." + this.extension, new File(this.directory));
        }

        StringWriter stringWriter = new StringWriter();

        stringWriter.append("Service : ");
        stringWriter.append(exchange.getServiceName());
        stringWriter.append("\n");

        stringWriter.append("Operation : ");
        stringWriter.append(exchange.getOperation());
        stringWriter.append("\n");

        stringWriter.append("Status : ");
        stringWriter.append(exchange.getStatus());
        stringWriter.append("\n");

        stringWriter.append("URI : ");
        stringWriter.append(exchange.getUri());
        stringWriter.append("\n");

        stringWriter.append("User agent : ");
        stringWriter.append(exchange.getUserAgent());
        stringWriter.append("\n");

        stringWriter.append("Encoding : ");
        stringWriter.append(exchange.getEncoding());
        stringWriter.append("\n");

        stringWriter.append("Date in : ");
        stringWriter.append(exchange.getInDate().toString());
        stringWriter.append("\n");

        stringWriter.append("Date out : ");
        stringWriter.append(exchange.getOutDate().toString());
        stringWriter.append("\n");

        stringWriter.append("Request size : ");
        stringWriter.append(String.valueOf(exchange.getRequestSize()));
        stringWriter.append("\n");

        stringWriter.append("Response size : ");
        stringWriter.append(String.valueOf(exchange.getResponseSize()));
        stringWriter.append("\n");

        stringWriter.append("\n\n\nRequest : \n\n\n");
        stringWriter.append(exchange.getRequest());
        stringWriter.append("\n\n\n\n");

        stringWriter.append("\n\n\nResponse : \n\n\n");
        stringWriter.append(exchange.getResponse());
        stringWriter.append("\n\n\n\n");

        if ("ERROR".equals(exchange.getStatus())) {
            stringWriter.append("\n\n\nExcepttion : ");
            stringWriter.append(exchange.getExceptionType());
            stringWriter.append("\nStackTrace : ");
            stringWriter.append(exchange.getStackTrace());
            stringWriter.append("\n\n\n\n");
        }

        stringWriter.append("\n\nProperties : \n");

        if (exchange.getProperties() != null) {
            for (ExchangeDataProperty exchangeProperty : exchange.getProperties()) {
                stringWriter.append(exchangeProperty.getName());
                stringWriter.append(" : ");
                stringWriter.append(exchangeProperty.getValue());
                stringWriter.append("\n");
            }
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(stringWriter.getBuffer().toString().getBytes());
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Exchange data saved in " + file.getAbsolutePath());
        }

    }
}
