package be.nabu.eai.module.services.glue.testing;

import java.util.List;
import java.util.Map;

import be.nabu.eai.module.services.glue.DynamicCacheProvider;
import be.nabu.glue.api.SecurityUpgradeable;
import be.nabu.glue.core.api.Lambda;
import be.nabu.glue.services.CombinedExecutionContext;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.libs.authentication.impl.ImpersonateToken;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.FeaturedExecutionContext;
import be.nabu.libs.services.api.SecurityContext;
import be.nabu.libs.services.api.UpgradeableSecurityContext;

public class GlueTestServiceMethods {
	private GlueTestServiceArtifact artifact;

	protected GlueTestServiceMethods() {
	}
	
	public void stub(String serviceId, Lambda lambda, String condition) {
		DynamicCacheProvider dynamicCacheProvider = artifact.getDynamicCacheProvider();
		if (dynamicCacheProvider == null) {
			throw new IllegalStateException("No dynamic cache provider found");
		}
		dynamicCacheProvider.registerOverride(serviceId, condition, lambda);
	}
	
	public void runAs(String name, String realm) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("A user name is required");
		}
		ScriptRuntime runtime = ScriptRuntime.getRuntime();
		if (runtime == null) {
			throw new IllegalStateException("No test script is running");
		}
		ExecutionContext context = (ExecutionContext) runtime.getContext().get(ServiceMethodProvider.SERVICE_CONTEXT);
		if (context == null && runtime.getExecutionContext() instanceof CombinedExecutionContext) {
			context = (CombinedExecutionContext) runtime.getExecutionContext();
		}
		if (context == null) {
			context = artifact.getRepository().newExecutionContext(null);
			if (context instanceof FeaturedExecutionContext) {
				List<String> enabledFeatures = ((FeaturedExecutionContext) context).getEnabledFeatures();
				if (runtime.getExecutionContext() instanceof FeaturedExecutionContext) {
					enabledFeatures.addAll(((FeaturedExecutionContext) runtime.getExecutionContext()).getEnabledFeatures());
				}
				Map<String, Object> globalContext = ServiceRuntime.getGlobalContext();
				if (globalContext != null && globalContext.get(ServiceRuntime.ENABLED_FEATURES) instanceof List) {
					for (Object feature : (List<?>) globalContext.get(ServiceRuntime.ENABLED_FEATURES)) {
						if (feature instanceof String && !enabledFeatures.contains(feature)) {
							enabledFeatures.add((String) feature);
						}
					}
				}
			}
			runtime.getContext().put(ServiceMethodProvider.SERVICE_CONTEXT, context);
		}
		SecurityContext securityContext = context.getSecurityContext();
		if (!(securityContext instanceof UpgradeableSecurityContext)) {
			throw new IllegalStateException("Test security context cannot be upgraded");
		}
		String effectiveRealm = realm == null && securityContext.getToken() != null ? securityContext.getToken().getRealm() : realm;
		ImpersonateToken token = new ImpersonateToken(null, effectiveRealm, name);
		((UpgradeableSecurityContext) securityContext).upgrade(token);
		if (!(runtime.getExecutionContext() instanceof CombinedExecutionContext) && runtime.getExecutionContext() instanceof SecurityUpgradeable) {
			((SecurityUpgradeable) runtime.getExecutionContext()).setPrincipal(token);
		}
	}

	protected void setArtifact(GlueTestServiceArtifact artifact) {
		this.artifact = artifact;
	}
}
