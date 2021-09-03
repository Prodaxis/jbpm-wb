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

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.ui.client.local.api.elemental2.IsElement;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.jbpm.workbench.forms.client.display.GenericFormDisplayer;
import org.jbpm.workbench.forms.client.display.RunTypeEnum;
import org.jbpm.workbench.forms.client.display.field.AsyncErrorType;
import org.jbpm.workbench.forms.client.i18n.Constants;
import org.kie.workbench.common.forms.dynamic.client.DynamicFormRenderer;
import org.kie.workbench.common.forms.dynamic.service.shared.FormRenderingContext;
import org.kie.workbench.common.forms.dynamic.service.shared.ParteorComponentDataService;
import org.kie.workbench.common.forms.processing.engine.handling.FormField;
import org.kie.workbench.common.forms.processing.engine.handling.FormHandler;
import org.uberfire.client.promise.Promises;

import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;

import elemental2.dom.HTMLDivElement;
import elemental2.promise.Promise;

@Templated
@Dependent
public class KieWorkbenchFormDisplayerViewImpl implements KieWorkbenchFormDisplayerView, IsElement {

	@Inject
	@DataField
	private HTMLDivElement header;

	@Inject
	@DataField
	private HTMLDivElement alert;

	@Inject
	@DataField
	private DynamicFormRenderer form;

	@Inject
	private Promises promises;

	@Inject
	protected Caller<ParteorComponentDataService> parteorComponentDataService;

	@Override
	public void show(FormRenderingContext ctx, boolean showHeader) {
		boolean alertAdded = header.contains(alert);

		if (!alertAdded && showHeader) {
			header.appendChild(alert);
		} else if (alertAdded && !showHeader) {
			header.removeChild(alert);
		}

		form.render(ctx);
	}

	@Override
	public boolean isValid() {
		return form.isValid();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void validAsyncAndRun(GenericFormDisplayer genericFormDisplayer, RunTypeEnum runType) {
		FormHandler mainFormHandler = form.getFormHandler();
		List<FormField> fieldsNeedChek = mainFormHandler.getForm().getFields().stream().filter(field -> doRemoteCheckNeed(field)).collect(Collectors.toList());
		if (null != fieldsNeedChek && !fieldsNeedChek.isEmpty()) {
			promises.all(fieldsNeedChek, field -> checkValueExistCall(field)).then(values -> {
				if (mainFormHandler.getForm().getFields().stream().allMatch(f -> f.getAsyncErrorKey() == null)) {
					genericFormDisplayer.runCallbackAfterValid(runType);
				}
				return promises.resolve();
			}).catch_(error -> {
				return null;
			});
		} else {
			genericFormDisplayer.runCallbackAfterValid(runType);
		}
	}

	protected Object getFieldValue(FormField field) {
		if (field.getWidget() instanceof HasValue) {
			return ((HasValue) field.getWidget()).getValue();
		} else if (field.getWidget() instanceof TakesValue) {
			return ((TakesValue) field.getWidget()).getValue();
		} else if (field.getWidget() instanceof HasText) {
			return ((HasText) field.getWidget()).getText();
		}
		throw new IllegalStateException("Unexpected widget type: impossible to read the value");
	}

	protected Promise<Boolean> checkValueExistCall(FormField field) {
		return promises.promisify(parteorComponentDataService, s -> {
            return s.checkDataExist(field.getMethodClassMappingParteor(), field.getKeyMappingParteor(), getFieldValue(field));
        }).then(result -> {
        	if (Boolean.FALSE.equals(result)) {
                String table = "Parteor";
                String dataKeyFieldBinding = field.getKeyMappingParteor();
                if(dataKeyFieldBinding != null && dataKeyFieldBinding.contains("#")){
                    table = field.getKeyMappingParteor().substring(0, dataKeyFieldBinding.indexOf("#"));
                }
                field.setAsyncErrorKey(AsyncErrorType.NOT_EXIST.toString());
                field.showError(Constants.INSTANCE.ValueNotExistInTable(getFieldValue(field).toString(), table));
            }else{
            	field.setAsyncErrorKey(null);
            }
            return promises.resolve();
        }).catch_(i -> {
        	field.showWarning(Constants.INSTANCE.UnexpectedError(Constants.INSTANCE.ActionCheckValueExist()));
            return promises.resolve();
        });
	}

	protected boolean checkInfoForValueExistNeed(FormField field) {
		String dataKeyFieldBinding = field.getKeyMappingParteor();
		if (null == dataKeyFieldBinding || "".equals(dataKeyFieldBinding)) {
			field.showError(Constants.INSTANCE.FieldKeyMappingIsRequiredToCheckExist());
		} else {
			if (!dataKeyFieldBinding.contains("#")) {
				field.showError(Constants.INSTANCE.FieldKeyValueMappingFormatErorr());
			} else {
				return true;
			}
		}
		return false;
	}

	protected boolean doRemoteCheckNeed(FormField field) {
		boolean check = field.isCheckValueExist();
		if(check){
			Object value = getFieldValue(field);
			if(null != value && !"".equals(value)){
				check = checkInfoForValueExistNeed(field);	
			}else{
				return false;
			}
		}
		return check;
	}
}
