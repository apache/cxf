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

package org.apache.cxf.sts.event.map;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

public class MapEventLogger implements MapEventListener {
    private static final Logger LOG = LogUtils.getL7dLogger(MapEventLogger.class);

    private List<String> fieldOrder = Arrays.asList(
        KEYS.TIME.name(),
        KEYS.STATUS.name(),
        KEYS.DURATION.name(),
        KEYS.REMOTE_HOST.name(),
        KEYS.REMOTE_PORT.name(),
        KEYS.OPERATION.name(),
        KEYS.URL.name(),
        KEYS.REALM.name(),
        KEYS.WS_SEC_PRINCIPAL.name(),
        KEYS.ONBEHALFOF_PRINCIPAL.name(),
        KEYS.ACTAS_PRINCIPAL.name(),
        KEYS.VALIDATE_PRINCIPAL.name(),
        KEYS.CANCEL_PRINCIPAL.name(),
        KEYS.RENEW_PRINCIPAL.name(),
        KEYS.TOKENTYPE.name(),
        KEYS.KEYTYPE.name(),
        KEYS.APPLIESTO.name(),
        KEYS.CLAIMS_PRIMARY.name(),
        KEYS.CLAIMS_SECONDARY.name(),
        KEYS.EXCEPTION.name(),
        KEYS.STACKTRACE.name()
    );
    private boolean logStacktrace;
    private boolean logFieldname;
    private Level logLevel = Level.FINE;
    private String format;

    @Override
    public void onEvent(MapEvent event) {
        Map<String, ?> map = event.getProperties();
        final StringBuilder builder = new StringBuilder();
        for (String key : fieldOrder) {
            if (this.logFieldname) {
                builder.append(key).append('=').append(map.get(key)).append(';');
            } else {
                builder.append(format(map.get(key))).append(';');
            }
        }
        Exception ex = (Exception) map.get(KEYS.EXCEPTION.name());
        if (logStacktrace) {
            LOG.log(this.logLevel, builder.toString(), ex);
        } else {
            LOG.log(this.logLevel, builder.toString());
        }
    }

    private String format(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Date) {
            final DateFormat dateFormat;
            if (format != null) {
                dateFormat = new SimpleDateFormat(format);
            } else {
                dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
            }
            return dateFormat.format(value);
        } else {
            return (value == null) ? "<null>" : value.toString();
        }
    }

    public List<String> getFieldOrder() {
        return fieldOrder;
    }

    public void setFieldOrder(List<String> fieldOrder) {
        this.fieldOrder = fieldOrder;
    }

    public boolean isLogStacktrace() {
        return logStacktrace;
    }

    public void setLogStacktrace(boolean logStacktrace) {
        this.logStacktrace = logStacktrace;
    }

    public boolean isLogFieldname() {
        return logFieldname;
    }

    public void setLogFieldname(boolean logFieldname) {
        this.logFieldname = logFieldname;
    }

    public void setDateFormat(String dateFormat) {
        this.format = dateFormat;
    }

    public String getLogLevel() {
        return logLevel.getName();
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = Level.parse(logLevel);
    }

}
