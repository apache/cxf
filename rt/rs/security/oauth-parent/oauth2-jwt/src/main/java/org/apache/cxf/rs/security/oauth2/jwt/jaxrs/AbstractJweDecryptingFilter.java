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
package org.apache.cxf.rs.security.oauth2.jwt.jaxrs;

import java.io.IOException;
import java.io.InputStream;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptor;
import org.apache.cxf.rs.security.oauth2.jwe.JweHeaders;

public class AbstractJweDecryptingFilter {
    private JweDecryptor decryptor;
    protected byte[] decrypt(InputStream is) throws IOException {
        JweDecryptionOutput out = decryptor.decrypt(new String(IOUtils.readBytesFromStream(is), "UTF-8"));
        validateHeaders(out.getHeaders());
        return out.getContent();
    }

    protected void validateHeaders(JweHeaders headers) {
        // complete
    }
    public void setDecryptor(JweDecryptor decryptor) {
        this.decryptor = decryptor;
    }

}
