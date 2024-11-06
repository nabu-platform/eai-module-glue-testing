/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.services.glue.testing;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.module.services.glue.AllowTargetSwitchProvider;
import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.repository.api.Repository;
import be.nabu.glue.api.runs.GlueAttachment;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.api.runs.ScriptResult;
import be.nabu.glue.core.impl.methods.TestMethods;
import be.nabu.glue.core.impl.methods.v2.ScriptMethods;
import be.nabu.glue.impl.SimpleScriptResult;
import be.nabu.glue.impl.formatted.FormattedScriptResult;
import be.nabu.glue.impl.formatters.MarkdownOutputFormatter;
import be.nabu.glue.json.JSONOutputFormatter;
import be.nabu.glue.services.CombinedExecutionContextImpl;
import be.nabu.glue.services.GlueService;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.java.BeanInstance;

public class GlueTestServiceArtifact extends GlueServiceArtifact {

	private GlueService service;
	private boolean useJsonFormatting = false;
	
	// don't allow remote switching for glue services
	// we want to be able to pass along webdriver instances
	public GlueTestServiceArtifact(String id, ResourceContainer<?> directory, Repository repository) throws IOException {
		super(id, directory, repository, new AllowTargetSwitchProvider() {
			
			@Override
			public boolean allowTargetSwitch(Service service, ExecutionContext context, ComplexContent input) {
				return !(service instanceof GlueServiceArtifact);
			}
		});
	}

	@Override
	public ServiceInstance newInstance() {
		return new GlueTestServiceInstance(getService(), useJsonFormatting);
	}

	protected GlueService getService() {
		if (service == null) {
			synchronized(this) {
				if (service == null) {
					GlueService service = new GlueService(getScript(), getExecutionEnvironment(), null);
					service.setImplementedInterface(MethodServiceInterface.wrap(TestCase.class, "run"));
//					((ModifiableComplexType) service.getServiceInterface().getOutputDefinition()).add(
//						new ComplexElementImpl("result", (ComplexType) BeanResolver.getInstance().resolve(FormattedScriptResult.class), service.getServiceInterface().getOutputDefinition()));
//					((ModifiableComplexType) service.getServiceInterface().getOutputDefinition()).setProperty(new ValueImpl<Type>(SuperTypeProperty.getInstance(), BeanResolver.getInstance().resolve(FormattedScriptResult.class)));
					this.service = service;
				}
			}
		}
		return service;
	}

	protected void reset() {
		service = null;
	}
	
	public static class GlueTestServiceInstance implements ServiceInstance {

		private GlueService service;
		private boolean useJsonFormatting;

		public GlueTestServiceInstance(GlueService service, boolean useJsonFormatting) {
			this.service = service;
			this.useJsonFormatting = useJsonFormatting;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
			Map<String, Object> map = new HashMap<String, Object>();
			if (input != null) {
				// map input
				for (Element<?> element : TypeUtils.getAllChildren(input.getType())) {
					map.put(element.getName(), input.get(element.getName()));
				}
			}
			ScriptRuntime currentRuntime = ScriptRuntime.getRuntime();
			ScriptRuntime runtime = new ScriptRuntime(service.getScript(), new CombinedExecutionContextImpl(executionContext, service.getEnvironment(), service.getLabelEvaluator()), map);
			StringWriter writer = new StringWriter();
			
			if (useJsonFormatting) {
				JSONOutputFormatter formatter = new JSONOutputFormatter(currentRuntime == null ? null : currentRuntime.getFormatter());
				runtime.setFormatter(formatter);
			}
			else {
				MarkdownOutputFormatter formatter = new MarkdownOutputFormatter(writer);
				if (currentRuntime != null) {
					formatter.setParent(currentRuntime.getFormatter());
				}
				formatter.setAllowDeepLogging(true);
				runtime.setFormatter(formatter);
			}
			runtime.run();
			if (runtime.getException() != null) {
				throw new ServiceException(runtime.getException());
			}
			// map output back
			ComplexContent output = service.getServiceInterface().getOutputDefinition().newInstance();
			// we explicitly only map the local children of the output type (if any)
			// the super type is mapped separately as it is a formatted script result
			for (Element<?> element : output.getType()) {
				output.set(element.getName(), runtime.getExecutionContext().getPipeline().get(element.getName()));
			}
			// map script result
			List<GlueValidation> validations  = (List<GlueValidation>) runtime.getContext().get(TestMethods.VALIDATION);
			List<GlueAttachment> attachments = (List<GlueAttachment>) runtime.getContext().get(ScriptMethods.ATTACHMENT);
			ScriptResult result = new SimpleScriptResult(service.getEnvironment(), runtime.getScript(), runtime.getStarted(), runtime.getStopped(), runtime.getException(), writer.toString(), validations == null ? new ArrayList<GlueValidation>() : validations, attachments);
			FormattedScriptResult format = FormattedScriptResult.format(result, null);
			BeanInstance<FormattedScriptResult> beanInstance = new BeanInstance<FormattedScriptResult>(format);
			output.set("result", beanInstance);
//			for (Element<?> field : TypeUtils.getAllChildren(beanInstance.getType())) {
//				output.set(field.getName(), beanInstance.get(field.getName()));
//			}
			return output;
		}

		@Override
		public Service getDefinition() {
			return service;
		}

	}

	public boolean isUseJsonFormatting() {
		return useJsonFormatting;
	}

	public void setUseJsonFormatting(boolean useJsonFormatting) {
		this.useJsonFormatting = useJsonFormatting;
	}

}
