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

package org.apache.cxf.xkms.exception;

import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;

public class XKMSException extends RuntimeException {

    private static final long serialVersionUID = 7247415453067157299L;

    private ResultMajorEnum resultMajor;
    private ResultMinorEnum resultMinor;

    public XKMSException(ResultMajorEnum resultMajor, ResultMinorEnum resultMinor) {
        super(String.format("Result major: %s; result minor: %s", resultMajor.toString(),
                            resultMinor.toString()));
        this.resultMajor = resultMajor;
        this.resultMinor = resultMinor;
    }

    public XKMSException(ResultMajorEnum resultMajor, ResultMinorEnum resultMinor, String msg) {
        super((msg != null) ? msg : String.format("Result major: %s; result minor: %s", resultMajor.toString(),
                            resultMinor.toString()));
        this.resultMajor = resultMajor;
        this.resultMinor = resultMinor;
    }

    public XKMSException(ResultMajorEnum resultMajor, ResultMinorEnum resultMinor, String arg0, Throwable arg1) {
        super(arg0, arg1);
        this.resultMajor = resultMajor;
        this.resultMinor = resultMinor;
    }

    public XKMSException(String arg0) {
        super(arg0);
    }

    public XKMSException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public ResultMajorEnum getResultMajor() {
        return resultMajor;
    }

    public ResultMinorEnum getResultMinor() {
        return resultMinor;
    }

}
