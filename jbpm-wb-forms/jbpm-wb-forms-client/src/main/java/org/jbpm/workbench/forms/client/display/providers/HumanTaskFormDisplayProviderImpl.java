/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.workbench.forms.client.display.providers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.container.SyncBeanDef;
import org.jboss.errai.ioc.client.container.SyncBeanManager;
import org.jbpm.workbench.forms.client.display.api.HumanTaskFormDisplayProvider;
import org.jbpm.workbench.forms.client.display.api.HumanTaskFormDisplayer;
import org.jbpm.workbench.forms.client.display.views.FormDisplayerView;
import org.jbpm.workbench.forms.client.i18n.Constants;
import org.jbpm.workbench.forms.display.FormRenderingSettings;
import org.jbpm.workbench.forms.display.api.HumanTaskDisplayerConfig;
import org.jbpm.workbench.forms.display.api.TaskFormPermissionDeniedException;
import org.jbpm.workbench.forms.service.shared.FormServiceEntryPoint;
import org.uberfire.ext.widgets.common.client.common.popups.errors.ErrorPopup;
import org.uberfire.mvp.BiParameterizedCommand;

@ApplicationScoped
public class HumanTaskFormDisplayProviderImpl implements HumanTaskFormDisplayProvider {

    protected Constants constants = GWT.create(Constants.class);

    @Inject
    protected SyncBeanManager iocManager;

    @Inject
    private Caller<FormServiceEntryPoint> formServices;

    private Map<Class<? extends FormRenderingSettings>, HumanTaskFormDisplayer> taskDisplayers = new HashMap<>();

    @PostConstruct
    public void init() {
        taskDisplayers.clear();

        final Collection<SyncBeanDef<HumanTaskFormDisplayer>> taskDisplayersBeans = iocManager.lookupBeans(
                HumanTaskFormDisplayer.class);
        if (taskDisplayersBeans != null) {
            for (final SyncBeanDef displayerDef : taskDisplayersBeans) {

                HumanTaskFormDisplayer displayer = (HumanTaskFormDisplayer) displayerDef.getInstance();

                taskDisplayers.put(displayer.getSupportedRenderingSettings(),
                                   displayer);
            }
        }
    }

    @Override
    public void setup(final HumanTaskDisplayerConfig config,
                      final FormDisplayerView view) {
        display(config,
                view);
    }

    protected void display(final HumanTaskDisplayerConfig config,
                           final FormDisplayerView view) {
        if (taskDisplayers != null) {
            formServices.call((RemoteCallback<FormRenderingSettings>) settings -> {

                if (settings == null) {
                    ErrorPopup.showMessage(constants.UnableToFindFormForTask(config.getKey().getTaskId()));
                } else {
                    HumanTaskFormDisplayer displayer = taskDisplayers.get(settings.getClass());
                    if (displayer != null) {
                        config.setRenderingSettings(settings);
                        displayer.init(config, view.getOnCloseCommand(), () -> display(config, view));
                        displayer.addOnValidationFailedCallBack(new BiParameterizedCommand<String, String>() {
							@Override
							public void execute(String parameter1, String parameter2) {
								view.showFormError(parameter1, parameter2);
							}
						});
                        view.display(displayer);
                    }
                }
            }, (ErrorCallback<Message>) (message, throwable) -> {
                String errorMessage;
                if (throwable instanceof TaskFormPermissionDeniedException) {
                    errorMessage = Constants.INSTANCE.PermissionDenied();
                } else {
                    errorMessage = Constants.INSTANCE.Exception(throwable.getMessage());
                }
                view.displayErrorMessage(Constants.INSTANCE.TaskFormErrorHeader(), errorMessage);
                return false;
            }).getFormDisplayTask(config.getKey().getServerTemplateId(),
                                  config.getKey().getDeploymentId(),
                                  config.getKey().getTaskId());
        }
    }
}
