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
package demo.oauth.server.controllers;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.common.util.StringUtils;

import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.provider.MD5SequenceGenerator;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.utils.OAuthUtils;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import demo.oauth.server.ClientApp;

@Controller
public class ApplicationController implements ServletContextAware {

    private OAuthDataProvider oauthDataProvider;

    private OAuthClientManager clientManager;

    @RequestMapping("/newClientForm")
    public ModelAndView handleRequest(@ModelAttribute("client") ClientApp clientApp) {
        return new ModelAndView("newClientForm");
    }

    @RequestMapping("/registerClient")
    public ModelAndView registerApp(@ModelAttribute("client") ClientApp clientApp)
        throws Exception {

        if (StringUtils.isEmpty(clientApp.getClientName())) {
            clientApp.setError("Client name field is required!");

            return handleInternalRedirect(clientApp);
        }

        MD5SequenceGenerator tokenGen = new MD5SequenceGenerator();
        Principal principal = SecurityContextHolder.getContext().getAuthentication();
        String consumerKey = clientApp.getConsumerKey();
        if (StringUtils.isEmpty(consumerKey)) {
            consumerKey = tokenGen
                .generate((principal.getName() + clientApp.getClientName()).getBytes(StandardCharsets.UTF_8));
        }

        String secretKey = tokenGen.generate(new SecureRandom().generateSeed(20));

        Client clientInfo = 
            new Client(consumerKey, secretKey, clientApp.getClientName(), null);
        clientInfo.setCallbackURI(clientApp.getCallbackURL());
        clientInfo.setLoginName(principal.getName());

        Client authNInfo = clientManager.registerNewClient(consumerKey, clientInfo);
        if (authNInfo != null) {
            clientApp.setError("Client already exists!");

            return handleInternalRedirect(clientApp);
        }

        ModelAndView modelAndView = new ModelAndView("clientDetails");
        modelAndView.getModel().put("clientInfo", clientInfo);

        return modelAndView;
    }

    @RequestMapping("/listRegisteredClients")
    public ModelAndView listRegisteredClients() {
        Set<Client> apps = clientManager.listRegisteredClients();

        ModelAndView modelAndView = new ModelAndView("registeredClientsList");
        modelAndView.getModelMap().put("clients", apps);
        return modelAndView;
    }

    @RequestMapping("/listAuthorizedClients")
    public ModelAndView listAuthorizedClients() {
        Set<Client> apps = clientManager.listAuthorizedClients();

        ModelAndView modelAndView = new ModelAndView("authorizedClientsList");
        modelAndView.getModelMap().put("clients", apps);
        return modelAndView;
    }

    @RequestMapping("/removeClient")
    public ModelAndView removeClient(HttpServletRequest request) {
        String consumerKey = request.getParameter("consumerKey");

        clientManager.removeRegisteredClient(consumerKey);


        ModelAndView modelAndView = new ModelAndView(new RedirectView("/app/listRegisteredClients"));
        return modelAndView;
    }

    @RequestMapping("/revokeAccess")
    public ModelAndView revokeAccess(HttpServletRequest request) {
        String consumerKey = request.getParameter("consumerKey");
        
        clientManager.removeAllTokens(consumerKey);

        ModelAndView modelAndView = new ModelAndView(new RedirectView("/app/listAuthorizedClients"));
        return modelAndView;
    }

    @RequestMapping("/displayVerifier")
    public ModelAndView displayVerifier() {
        return new ModelAndView("displayVerifier");
    }

    private ModelAndView handleInternalRedirect(ClientApp app) {
        ModelAndView modelAndView = new ModelAndView("newClientForm");
        modelAndView.getModel().put("client", app);
        return modelAndView;
    }

    public void setServletContext(ServletContext servletContext) {
        oauthDataProvider = OAuthUtils.getOAuthDataProvider(null, servletContext);
        clientManager = (OAuthClientManager)oauthDataProvider;
    }
}
