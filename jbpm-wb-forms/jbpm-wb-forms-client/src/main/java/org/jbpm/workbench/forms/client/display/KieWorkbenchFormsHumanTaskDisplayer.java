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

package org.jbpm.workbench.forms.client.display;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.common.client.ui.ElementWrapperWidget;
import org.jbpm.workbench.forms.client.display.displayer.KieWorkbenchFormDisplayer;
import org.jbpm.workbench.forms.display.FormDisplayerConfig;
import org.jbpm.workbench.forms.client.display.task.AbstractHumanTaskFormDisplayer;
import org.jbpm.workbench.ht.model.TaskKey;
import org.jbpm.workbench.forms.display.api.KieWorkbenchFormRenderingSettings;
import org.jbpm.workbench.forms.display.service.KieWorkbenchFormsEntryPoint;
import org.uberfire.mvp.Command;

@Dependent
public class KieWorkbenchFormsHumanTaskDisplayer extends AbstractHumanTaskFormDisplayer<KieWorkbenchFormRenderingSettings> {

    private KieWorkbenchFormDisplayer formDisplayer;

    private Caller<KieWorkbenchFormsEntryPoint> service;

    @Inject
    public KieWorkbenchFormsHumanTaskDisplayer(KieWorkbenchFormDisplayer formDisplayer,
                                               Caller<KieWorkbenchFormsEntryPoint> service) {
        this.formDisplayer = formDisplayer;
        this.service = service;
    }

    @Override
    public void init(FormDisplayerConfig<TaskKey, KieWorkbenchFormRenderingSettings> config,
                     Command onCloseCommand,
                     Command onRefreshCommand) {
        super.init(config,
                   onCloseCommand,
                   onRefreshCommand);
    }

    @Override
    protected void initDisplayer() {
        formDisplayer.show(renderingSettings.getRenderingContext(), renderingSettings.isDefaultForms());
        formContainer.add(ElementWrapperWidget.getWidget(formDisplayer.getElement()));
    }

    @Override
    protected void completeFromDisplayer() {
        formDisplayer.validAndRun(this, RunTypeEnum.COMPLETE);
    }

    @Override
    protected void saveStateFromDisplayer() {
        if (formDisplayer.isValid()) {
            service.call(getSaveTaskStateCallback(),
                         getUnexpectedErrorCallback()).saveTaskStateFromRenderContext(
                    renderingSettings.getTimestamp(),
                    renderingSettings.getRenderingContext().getModel(),
                    serverTemplateId,
                    deploymentId,
                    taskId);
        }
    }

    @Override
    protected void startFromDisplayer() {
        service.call(response -> start()).clearContext(renderingSettings.getTimestamp());
    }

    @Override
    protected void claimFromDisplayer() {
        service.call(response -> claim()).clearContext(renderingSettings.getTimestamp());
    }

    @Override
    protected void releaseFromDisplayer() {
        service.call(response -> release()).clearContext(renderingSettings.getTimestamp());
    }

    @Override
    protected void clearRenderingSettings() {
        service.call().clearContext(renderingSettings.getTimestamp());
        super.clearRenderingSettings();
    }

    @Override
    public Class<KieWorkbenchFormRenderingSettings> getSupportedRenderingSettings() {
        return KieWorkbenchFormRenderingSettings.class;
    }

    @Override
    public boolean appendFooter() {
        return true;
    }

    @Override
    public void runCallbackAfterValid(RunTypeEnum runType) {
        if(runType == RunTypeEnum.COMPLETE){
        	if(!isRootFormNeedValid()){
        		completeTaskFromContext();
        	}else{
        		parteorComponentDataService.call(new RemoteCallback() {
					@Override
					public void callback(Object response) {
						if(null == response || "".equals(response.toString())){
							completeTaskFromContext();
						}else{
							validFailed("Erreur d'exécution du script validation de formulaire: ", response.toString());
						}
					}
				}, new ErrorCallback() {
					@Override
					public boolean error(Object message, Throwable throwable) {
						validFailed("Imposible d'appeler exécution du script validation de formulaire: ", message + "");
						return false;
					}
				}).runFormValidationJSScript(serverTemplateId, deploymentId, taskId, renderingSettings.getRenderingContext().getNodeRenderingSettingsMetaData().get("onValidationAction").toString(), renderingSettings.getRenderingContext().getModel());
        	}
        }
    }
    
    private void completeTaskFromContext(){
    	service.call(getCompleteTaskRemoteCallback(), getUnexpectedErrorCallback()).completeTaskFromContext(renderingSettings.getTimestamp(), renderingSettings.getRenderingContext().getModel(), serverTemplateId, deploymentId, taskId);
    }
    
    private boolean isRootFormNeedValid(){
    	Object scriptFormValid = renderingSettings.getRenderingContext().getNodeRenderingSettingsMetaData().get("onValidationAction");
    	if(null != scriptFormValid && scriptFormValid.toString().trim().length() > 0){
    		return true;
    	}
    	return false;
    }
}
