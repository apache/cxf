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

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        final Path file;

        if (this.directory == null) {
            file = Files.createTempFile("cxf-management-", "." + this.extension);
        } else {
            file = Files.createTempFile(Paths.get(this.directory), "cxf-management-", "." + this.extension);
        }

        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            bw.append("Service : ").append(exchange.getServiceName()).append('\n');

            bw.append("Operation : ").append(exchange.getOperation()).append('\n');
            bw.append("Status : ").append(exchange.getStatus()).append('\n');
            bw.append("URI : ").append(exchange.getUri()).append('\n');
            bw.append("User agent : ").append(exchange.getUserAgent()).append('\n');
            bw.append("Encoding : ").append(exchange.getEncoding()).append('\n');
            bw.append("Date in : ").append(String.valueOf(exchange.getInDate())).append('\n');
            bw.append("Date out : ").append(String.valueOf(exchange.getOutDate())).append('\n');
            bw.append("Request size : ").append(String.valueOf(exchange.getRequestSize())).append('\n');
            bw.append("Response size : ").append(String.valueOf(exchange.getResponseSize())).append('\n');

            bw.append("\n\n\nRequest : \n\n\n").append(exchange.getRequest()).append("\n\n\n\n");
            bw.append("\n\n\nResponse : \n\n\n").append(exchange.getResponse()).append("\n\n\n\n");

            if ("ERROR".equals(exchange.getStatus())) {
                bw.append("\n\n\nExcepttion : ").append(exchange.getExceptionType());
                bw.append("\nStackTrace : ").append(exchange.getStackTrace());
                bw.append("\n\n\n\n");
            }

            bw.append("\n\nProperties : \n");

            if (exchange.getProperties() != null) {
                for (ExchangeDataProperty edp : exchange.getProperties()) {
                    bw.append(edp.getName()).append(" : ").append(edp.getValue()).append('\n');
                }
            }
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Exchange data saved in " + file);
        }

    }

}
