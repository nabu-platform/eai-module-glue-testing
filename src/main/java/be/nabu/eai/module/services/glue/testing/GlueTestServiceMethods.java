package be.nabu.eai.module.services.glue.testing;

import be.nabu.eai.module.services.glue.DynamicCacheProvider;
import be.nabu.glue.core.api.Lambda;

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
	
	protected void setArtifact(GlueTestServiceArtifact artifact) {
		this.artifact = artifact;
	}
}
