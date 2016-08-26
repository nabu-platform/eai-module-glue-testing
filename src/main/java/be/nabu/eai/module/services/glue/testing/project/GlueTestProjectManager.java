package be.nabu.eai.module.services.glue.testing.project;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class GlueTestProjectManager extends JAXBArtifactManager<GlueTestProjectConfiguration, GlueTestProjectArtifact> {

	public GlueTestProjectManager() {
		super(GlueTestProjectArtifact.class);
	}

	@Override
	protected GlueTestProjectArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new GlueTestProjectArtifact(id, container, repository);
	}

}
