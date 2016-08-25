package be.nabu.eai.module.services.glue.testing;

import java.io.IOException;

import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.resources.api.ResourceContainer;

public class GlueTestServiceArtifact extends GlueServiceArtifact {

	public GlueTestServiceArtifact(String id, ResourceContainer<?> directory, Repository repository) throws IOException {
		super(id, directory, repository);
	}

}
