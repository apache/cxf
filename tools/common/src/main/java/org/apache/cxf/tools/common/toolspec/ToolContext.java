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

package org.apache.cxf.tools.common.toolspec;

import java.io.*;
import org.w3c.dom.Document;
import org.apache.cxf.tools.common.ToolException;
public interface ToolContext {

    /**
     * Request an input stream.
     * @param id the id of the stream in the streams sections of the tool's definition document.
     */
    InputStream getInputStream(String id) throws ToolException;

    /**
     * Request the standard input stream.
     */
    InputStream getInputStream() throws ToolException;

    /**
     * Request a document based on the input stream.
     * This is only returned if the mime type of incoming stream is xml.
     */
    Document getInputDocument(String id) throws ToolException;

    /**
     * Request a document based on the standard input stream.
     * This is only returned if the mime type of incoming stream is xml.
     */
    Document getInputDocument() throws ToolException;

    OutputStream getOutputStream(String id) throws ToolException;

    OutputStream getOutputStream() throws ToolException;

    String getParameter(String name) throws ToolException;

    String[] getParameters(String name) throws ToolException;

    boolean hasParameter(String name) throws ToolException;

    void sendDocument(String id, Document doc);

    void sendDocument(Document doc);

    void executePipeline();

    void setUserObject(String key, Object o);

    Object getUserObject(String key);

}
