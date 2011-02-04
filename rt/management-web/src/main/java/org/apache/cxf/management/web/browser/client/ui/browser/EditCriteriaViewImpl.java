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

package org.apache.cxf.management.web.browser.client.ui.browser;

import javax.annotation.Nonnull;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.cxf.management.web.browser.client.ui.AbstractDialog;
import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserConstans;

@Singleton
public class EditCriteriaViewImpl extends AbstractDialog implements EditCriteriaView {

    @Nonnull
    private Form form;

    @Inject
    public EditCriteriaViewImpl(@Nonnull final LogBrowserConstans constans) {
        this.form = new Form();

        init(constans.editCriteriaDialogTitle(), form);
    }

    public Widget asWidget() {
        return null;
    }

    public void setPresenter(Presenter presenter) {
        //TODO implement
    }

    //TODO remove this internal class - information about how to render view should in BindStrategy 
    protected static class Form extends Composite {

        @UiTemplate("EditCriteriaView.ui.xml")
        interface FormViewUiBinder extends UiBinder<Widget, Form> { }

        private static final FormViewUiBinder UI_BINDER = GWT.create(FormViewUiBinder.class);

        public Form() {
            initWidget(UI_BINDER.createAndBindUi(this));
        }
    }
}