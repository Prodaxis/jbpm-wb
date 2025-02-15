/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.jbpm.workbench.forms.client.display.displayer;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.common.client.api.elemental2.IsElement;
import org.jbpm.workbench.forms.client.display.GenericFormDisplayer;
import org.jbpm.workbench.forms.client.display.RunTypeEnum;
import org.kie.soup.commons.validation.PortablePreconditions;
import org.kie.workbench.common.forms.dynamic.service.shared.FormRenderingContext;

import elemental2.dom.HTMLElement;

@Dependent
public class KieWorkbenchFormDisplayer implements IsElement {

    private KieWorkbenchFormDisplayerView view;

    @Inject
    public KieWorkbenchFormDisplayer(KieWorkbenchFormDisplayerView view) {
        this.view = view;
    }

    @Override
    public HTMLElement getElement() {
        return view.getElement();
    }

    public void show(FormRenderingContext ctx, boolean automaticallyGenerated) {
        PortablePreconditions.checkNotNull("ctx", ctx);

        view.show(ctx, automaticallyGenerated);
    }

    public boolean isValid() {
        return view.isValid();
    }
    
    public void validAndRun(GenericFormDisplayer genericFormDisplayer, RunTypeEnum runType){
        if(isValid()){
            view.validAsyncAndRun(genericFormDisplayer, runType);
        }
    }
}
