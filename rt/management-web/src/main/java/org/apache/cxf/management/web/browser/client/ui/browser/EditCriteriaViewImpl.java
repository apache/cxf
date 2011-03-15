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

import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.cxf.management.web.browser.client.ui.AbstractDialog;
import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserConstans;

@Singleton
public class EditCriteriaViewImpl extends AbstractDialog implements EditCriteriaView {
    @UiTemplate("EditCriteriaView.ui.xml")
    interface EditCriteriaViewUiBinder extends UiBinder<Widget, EditCriteriaViewImpl> { }

    private static final EditCriteriaViewUiBinder UI_BINDER = GWT.create(EditCriteriaViewUiBinder.class);

    @UiField @Nonnull TextBox phraseTextBox;
    @UiField @Nonnull DateBox fromDateBox;
    @UiField @Nonnull DateBox toDateBox;
    @UiField @Nonnull CheckBox debugCheckBox;
    @UiField @Nonnull CheckBox warnCheckBox;
    @UiField @Nonnull CheckBox infoCheckBox;
    @UiField @Nonnull CheckBox errorCheckBox;

    @Nonnull private Presenter presenter;

    @Inject
    public EditCriteriaViewImpl(@Nonnull final LogBrowserConstans constans) {
        init(constans.editCriteriaDialogTitle(), UI_BINDER.createAndBindUi(this));
    }

    @UiHandler("cancelButton")
    void onCancelButtonClicked(@Nonnull final ClickEvent event) {
        hide();
    }

    @UiHandler("saveButton")
    void onSaveButtonClicked(@Nonnull final ClickEvent event) {
        presenter.onSaveButtonClicked();
        hide();
    }

    public HasValue<String> getPhraseValue() {
        return phraseTextBox;
    }

    @Nonnull
    public HasValue<Date> getFromValue() {
        return fromDateBox;
    }

    @Nonnull
    public HasValue<Date> getToValue() {
        return toDateBox;
    }

    @Nonnull
    public HasValue<Boolean> getDebugValue() {
        return debugCheckBox;
    }

    @Nonnull
    public HasValue<Boolean> getInfoValue() {
        return infoCheckBox;
    }

    @Nonnull
    public HasValue<Boolean> getWarnValue() {
        return warnCheckBox;
    }

    @Nonnull
    public HasValue<Boolean> getErrorValue() {
        return errorCheckBox;
    }

    @Nullable
    public Widget asWidget() {
        return null;
    }

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
}