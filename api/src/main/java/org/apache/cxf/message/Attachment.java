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

package org.apache.cxf.message;

import java.util.Iterator;

import javax.activation.DataHandler;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public interface Attachment {
    DataHandler getDataHandler();

    /**
     * @return The attachment id.
     */
    String getId();
    
    String getHeader(String name);
    
    Iterator<String> getHeaderNames();
    
    /**
     * Whether or not this is an XOP package. This will affect the 
     * serialization of the attachment. If true, it will be serialized
     * as binary data, and not Base64Binary.
     * 
     * @return true if this attachment is an XOP package
     */
    boolean isXOP();
}
