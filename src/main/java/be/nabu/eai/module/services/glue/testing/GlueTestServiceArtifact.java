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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.services.glue.AllowTargetSwitchProvider;
import be.nabu.eai.module.services.glue.DynamicCacheProvider;
import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.module.services.glue.testing.RunProfileConfiguration.ServiceConfiguration;
import be.nabu.eai.module.services.glue.testing.RunProfileConfiguration.ServiceProfile;
import be.nabu.eai.repository.api.Repository;
import be.nabu.glue.api.runs.GlueAttachment;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.api.runs.ScriptResult;
import be.nabu.glue.core.impl.methods.TestMethods;
import be.nabu.glue.core.impl.methods.v2.ScriptMethods;
import be.nabu.glue.core.impl.providers.StaticJavaMethodProvider;
import be.nabu.glue.impl.SimpleScriptResult;
import be.nabu.glue.impl.formatted.FormattedScriptResult;
import be.nabu.glue.impl.formatters.MarkdownOutputFormatter;
import be.nabu.glue.json.JSONOutputFormatter;
import be.nabu.glue.services.CombinedExecutionContextImpl;
import be.nabu.glue.services.GlueService;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.cache.api.CacheProvider;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.PathAnalyzer;
import be.nabu.libs.evaluator.QueryParser;
import be.nabu.libs.evaluator.impl.VariableOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.evaluator.types.operations.TypesOperationProvider;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.FeaturedExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.java.BeanInstance;

public class GlueTestServiceArtifact extends GlueServiceArtifact implements CacheProvider {

	private GlueService service;
	private boolean useJsonFormatting = false;
	private RunProfileConfiguration runProfile = new RunProfileConfiguration();
	
	private DynamicCacheProvider dynamicCacheProvider;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	// don't allow remote switching for glue services
	// we want to be able to pass along webdriver instances
	public GlueTestServiceArtifact(String id, ResourceContainer<?> directory, Repository repository) throws IOException {
		this(id, directory, repository, new GlueTestServiceMethods());
	}
	// horrible horrible workaround to get a reference to the methods instance
	private GlueTestServiceArtifact(String id, ResourceContainer<?> directory, Repository repository, GlueTestServiceMethods methods) throws IOException {
		super(id, directory, repository, new AllowTargetSwitchProvider() {
			@Override
			public boolean allowTargetSwitch(Service service, ExecutionContext context, ComplexContent input) {
				return !(service instanceof GlueServiceArtifact);
			}
		}, false, new StaticJavaMethodProvider(methods));
		methods.setArtifact(this);
	}
	
	@Override
	public ServiceInstance newInstance() {
		GlueTestServiceInstance glueTestServiceInstance = new GlueTestServiceInstance(getService(), useJsonFormatting);
		glueTestServiceInstance.setEnabledFeatures(List.of(getId()));
		return glueTestServiceInstance;
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
	
	public class GlueTestServiceInstance implements ServiceInstance {

		private GlueService service;
		private boolean useJsonFormatting;
		private List<String> enabledFeatures;

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
			if (enabledFeatures != null && !enabledFeatures.isEmpty() && executionContext instanceof FeaturedExecutionContext) {
				((FeaturedExecutionContext) executionContext).getEnabledFeatures().addAll(enabledFeatures);
			}
			ScriptRuntime runtime = new ScriptRuntime(service.getScript(), new CombinedExecutionContextImpl(executionContext, service.getEnvironment(), service.getLabelEvaluator()), map);
			StringWriter writer = new StringWriter();
			
//			dynamicCacheProvider = new DynamicCacheProvider(runtime, executionContext);
			
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
		
		public List<String> getEnabledFeatures() {
			return enabledFeatures;
		}

		public void setEnabledFeatures(List<String> enabledFeatures) {
			this.enabledFeatures = enabledFeatures;
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

	@Override
	public Cache get(String name) throws IOException {
		List<String> features = ServiceRuntime.getRuntime() != null && ServiceRuntime.getRuntime().getExecutionContext() instanceof FeaturedExecutionContext 
			? ((FeaturedExecutionContext) ServiceRuntime.getRuntime().getExecutionContext()).getEnabledFeatures() 
			: null;
		if (features != null && features.contains(getId()) && dynamicCacheProvider != null) {
			Cache cache = dynamicCacheProvider.get(name);
			if (cache != null) {
				return cache;
			}
		}
		if (features != null && features.contains(getId()) && runProfile != null) {
			List<ServiceProfile> profiles = runProfile.getProfiles();
			if (profiles != null) {
				for (ServiceProfile profile : profiles) {
					// if we have a profile for this service and at least one configuration, return it
					if (profile.getService() != null && profile.getService().getId().equals(name) && profile.getConfigurations() != null) {
						return new Cache() {
							@Override
							public boolean put(Object key, Object value) throws IOException {
								// no can do
								return true;
							}
							@Override
							public Object get(Object key) throws IOException {
								try {
									// we expect the service input here
									if (key instanceof ComplexContent || key == null) {
										for (ServiceConfiguration configuration : profile.getConfigurations()) {
											boolean matches = true;
											// a null key can only match with no input queries
											if (key == null) {
												matches = configuration.getInputQueries() == null || configuration.getInputQueries().isEmpty();
											}
											else if (configuration.getInputQueries() != null) {
												for (String inputQuery : configuration.getInputQueries()) {
													Object result = getVariable((ComplexContent) key, inputQuery);
													if (result != null) {
														Boolean booleanResult = result instanceof Boolean
															? (Boolean) result
															: ConverterFactory.getInstance().getConverter().convert(result, Boolean.class);
														// if we can't convert it to a boolean directly, we assume any non-null value is true and null itself is false
														// this should be consistent with blox step conditions
														if (booleanResult == null) {
															booleanResult = result != null;
														}
														matches &= booleanResult;
													}
												}
											}
											// if we have a match, return the stored output
											if (matches) {
												if (configuration.getErrorCode() != null) {
													throw new ServiceException(configuration.getErrorCode(), configuration.getErrorMessage());
												}
												else if (configuration.getOutput() == null || configuration.getOutput().trim().isEmpty()) {
													return null;
												}
//												XMLBinding binding = new XMLBinding(profile.getService().getServiceInterface().getOutputDefinition(), Charset.forName("UTF-8"));
//												binding.setIgnoreUndefined(true);
												JSONBinding binding = new JSONBinding(profile.getService().getServiceInterface().getOutputDefinition(), Charset.forName("UTF-8"));
												binding.setIgnoreUnknownElements(true);
												return binding.unmarshal(new ByteArrayInputStream(configuration.getOutput().getBytes(Charset.forName("UTF-8"))), new Window[0]);
											}
										}
									}
								}
								catch (ServiceException e) {
									throw new IOException(e);
								}
								catch (Exception e) {
									logger.error("Could not establish runtime profile for " + getId() + " service " + profile.getService().getId(), e);
								}
								return null;
							}
							@Override
							public void clear(Object key) throws IOException {
								// no can do
							}
							@Override
							public void clear() throws IOException {
								// no can do
							}
							@Override
							public void prune() throws IOException {
								// no can do
							}
							@Override
							public void refresh() throws IOException {
								// no can do
							}
							@Override
							public void refresh(Object key) throws IOException {
								// no can do
							}
						};
					}
				}
			}
		}
		return null;
	}

	@Override
	public void remove(String name) throws IOException {
		if (dynamicCacheProvider != null) {
			dynamicCacheProvider.remove(name);
		}
	}

	public DynamicCacheProvider getDynamicCacheProvider() {
		return dynamicCacheProvider;
	}
	void setDynamicCacheProvider(DynamicCacheProvider dynamicCacheProvider) {
		this.dynamicCacheProvider = dynamicCacheProvider;
	}
	
	private Map<String, TypeOperation> analyzedOperations = new HashMap<String, TypeOperation>();
	protected TypeOperation getOperation(String query) throws ParseException {
		if (!analyzedOperations.containsKey(query)) {
			synchronized(analyzedOperations) {
				if (!analyzedOperations.containsKey(query))
					analyzedOperations.put(query, (TypeOperation) new PathAnalyzer<ComplexContent>(new TypesOperationProvider()).analyze(QueryParser.getInstance().parse(query)));
			}
		}
		return analyzedOperations.get(query);
	}
	
	protected Object getVariable(ComplexContent pipeline, String query) throws ServiceException {
		VariableOperation.registerRoot();
		try {
			return getOperation(query).evaluate(pipeline);
		}
		catch (EvaluationException e) {
			throw new RuntimeException(e);
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
		finally {
			VariableOperation.unregisterRoot();
		}
	}
	public RunProfileConfiguration getRunProfile() {
		return runProfile;
	}
	public void setRunProfile(RunProfileConfiguration runProfile) {
		this.runProfile = runProfile;
	}
}
