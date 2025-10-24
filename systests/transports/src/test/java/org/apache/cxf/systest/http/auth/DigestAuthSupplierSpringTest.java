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
package org.apache.cxf.systest.http.auth;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DigestAuthSupplier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {
        DigestAuthSupplierSpringTest.SecurityConfig.class,
        DigestAuthSupplierSpringTest.Controller.class
    },
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@SpringBootApplication
public class DigestAuthSupplierSpringTest {

    private static final String USER = "alice";
    private static final String PWD = "ecila";

    @LocalServerPort
    private int port;

    @Test
    public void test() {
        WebClient client = WebClient.create("http://localhost:" + port, (String) null);

        assertThrows(NotAuthorizedException.class, () -> client.get(String.class));

        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.setAuthSupplier(new DigestAuthSupplier());
        conduit.getAuthorization().setUserName(USER);
        conduit.getAuthorization().setPassword(PWD);

        assertEquals(Controller.RESPONSE, client.get(String.class));
    }

    @RestController
    static class Controller {

        static final String RESPONSE = "Hi!";

        @GetMapping(produces = MediaType.TEXT_PLAIN, value = "/")
        public String get() {
            return "Hi!";
        }

    }

    @EnableWebSecurity
    static class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            DigestAuthenticationEntryPoint authenticationEntryPoint = digestAuthenticationEntryPoint();
            return http
                .authorizeHttpRequests(c -> c.anyRequest().authenticated())
                .exceptionHandling(c -> c.authenticationEntryPoint(authenticationEntryPoint))
                .addFilter(digestAuthenticationFilter(authenticationEntryPoint))
                .build();
        }

        private DigestAuthenticationFilter digestAuthenticationFilter(
            DigestAuthenticationEntryPoint authenticationEntryPoint) {
            DigestAuthenticationFilter digestAuthenticationFilter = new DigestAuthenticationFilter();
            digestAuthenticationFilter.setUserDetailsService(userDetailsService());
            digestAuthenticationFilter.setAuthenticationEntryPoint(authenticationEntryPoint);
            digestAuthenticationFilter.setCreateAuthenticatedToken(true);
            return digestAuthenticationFilter;
        }

        private static DigestAuthenticationEntryPoint digestAuthenticationEntryPoint() {
            DigestAuthenticationEntryPoint digestAuthenticationEntryPoint = new DigestAuthenticationEntryPoint();
            digestAuthenticationEntryPoint.setKey("acegi");
            digestAuthenticationEntryPoint.setRealmName("Digest Realm");
            return digestAuthenticationEntryPoint;
        }

        @Bean
        public InMemoryUserDetailsManager userDetailsService() {
            final UserDetails user = User
                .builder()
                .username(USER)
                .password(PWD)
                .roles("")
                .build();
            return new InMemoryUserDetailsManager(user);
        }

        @Bean
        public static PasswordEncoder passwordEncoder() {
            return new PasswordEncoder() {
                @Override
                public String encode(CharSequence rawPassword) {
                    return rawPassword.toString();
                }
                @Override
                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return rawPassword.toString().equals(encodedPassword);
                }
            };
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(DigestAuthSupplierSpringTest.class, args);
    }

}
