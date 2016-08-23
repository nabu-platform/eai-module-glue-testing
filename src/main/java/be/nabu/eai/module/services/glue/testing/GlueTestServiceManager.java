package be.nabu.eai.module.services.glue.testing;

import java.io.IOException;

import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.module.services.glue.GlueServiceManager;
import be.nabu.eai.repository.api.ResourceEntry;

public class GlueTestServiceManager extends GlueServiceManager {

	@Override
	public GlueServiceArtifact newArtifact(ResourceEntry entry) throws IOException {
		return new GlueTestServiceArtifact(entry.getId(), entry.getRepository());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Class<GlueServiceArtifact> getArtifactClass() {
		Class clazz = GlueTestServiceArtifact.class;
		return clazz;
	}
	
}
