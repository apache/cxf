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

package org.apache.cxf.jaxrs.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;
import org.apache.cxf.common.logging.LogUtils;

/**
 * Retry-After  = "Retry-After" ":" ( HTTP-date | delta-seconds )
 * HTTP-date    = IMF-fixdate / obs-date
 *
 * An example of the preferred format is
 *    Sun, 06 Nov 1994 08:49:37 GMT    ; IMF-fixdate (RFC 822)
 *
 * Examples of the two obsolete formats are
 *   Sunday, 06-Nov-94 08:49:37 GMT   ; obsolete RFC 850 format
 *   Sun Nov  6 08:49:37 1994         ; ANSI C's asctime() format
 *
 */
public class RetryAfterHeaderProvider implements HeaderDelegate<Duration> {
    private static final Logger LOG = LogUtils.getL7dLogger(RetryAfterHeaderProvider.class);
    private static final Pattern DELTA_SECONDS_PATTERN = Pattern.compile("^(\\d+)$");
    private static final Pattern RFC_PATTERN = Pattern.compile("^\\s*([^,\\s]+)\\s*[,].+$");
    private final Clock clock;
    
    public RetryAfterHeaderProvider() {
        this(Clock.systemUTC());
    }

    public RetryAfterHeaderProvider(Clock clock) {
        this.clock = clock;
    }
    
    @Override
    public Duration fromString(String value) {
        if (value == null) {
            return Duration.ZERO;
        }
        
        final Matcher matcher = DELTA_SECONDS_PATTERN.matcher(value);
        if (matcher.matches()) {
            return Duration.ofSeconds(Long.parseLong(matcher.group(0)));
        } else {
            final DateFormat formatter = tryFormatter(value);
            if (formatter != null) {
                try {
                    final Date date = formatter.parse(value);
                    final long retryAfter = date.getTime() - clock.millis();
                    if (retryAfter > 0) {
                        return Duration.ofMillis(retryAfter);
                    }
                } catch (ParseException ex) {
                    LOG.fine("The format of '" + value + "' is not recognizable date: " + ex);
                }
            }
        }

        return Duration.ZERO;
    }

    private DateFormat tryFormatter(String value) {
        final Matcher matcher = RFC_PATTERN.matcher(value);
        if (matcher.matches()) {
            final String dayOfWeek = matcher.group(1);
            // RFC-1123 updates RFC-822 changing the year from two digits to four
            if (dayOfWeek.length() == 3) {
                return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            } else {
                // Obsolete RFC-850 format
                return new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", Locale.US);
            } 
        } else {
            // ANSI C's asctime() format
            return new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US);
        }
    }

    @Override
    public String toString(Duration value) {
        if (value == null) {
            return null;
        } else {
            return Long.toString(value.getSeconds());
        }
    }
    
    public static Duration valueOf(String value) {
        return new RetryAfterHeaderProvider().fromString(value);
    }
    
    public static Duration valueOf(Clock clock, String value) {
        return new RetryAfterHeaderProvider(clock).fromString(value);
    }
}
