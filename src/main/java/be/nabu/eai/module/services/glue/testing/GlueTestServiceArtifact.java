package be.nabu.eai.module.services.glue.testing;

import java.io.IOException;

import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.repository.api.Repository;

public class GlueTestServiceArtifact extends GlueServiceArtifact {

	public GlueTestServiceArtifact(String id, Repository repository) throws IOException {
		super(id, repository);
	}

}
