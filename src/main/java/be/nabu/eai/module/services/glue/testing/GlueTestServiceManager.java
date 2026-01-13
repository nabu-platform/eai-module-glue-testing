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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.module.services.glue.GlueServiceManager;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.validator.api.Validation;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class GlueTestServiceManager extends GlueServiceManager {

	@Override
	public GlueServiceArtifact newArtifact(ResourceEntry entry) throws IOException {
		return new GlueTestServiceArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Class<GlueServiceArtifact> getArtifactClass() {
		Class clazz = GlueTestServiceArtifact.class;
		return clazz;
	}
	
	private RunProfileConfiguration unmarshalRunConfiguration(Repository repository, InputStream input) {
		try {
			JAXBContext context = JAXBContext.newInstance(RunProfileConfiguration.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setAdapter(new ArtifactXMLAdapter(repository));
			return (RunProfileConfiguration) unmarshaller.unmarshal(input);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void marshalRunConfiguration(Repository repository, RunProfileConfiguration configuration, OutputStream output) {
		try {
			JAXBContext context = JAXBContext.newInstance(RunProfileConfiguration.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setAdapter(new ArtifactXMLAdapter(repository));
			marshaller.marshal(configuration, output);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GlueServiceArtifact load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		GlueServiceArtifact glueServiceArtifact = super.load(entry, messages);
		if (glueServiceArtifact instanceof GlueTestServiceArtifact) {
			Resource child = entry.getContainer().getChild("stub.xml");
			if (child != null) {
				ReadableContainer<ByteBuffer> readable = ((ReadableResource) child).getReadable();
				try {
					byte[] bytes = IOUtils.toBytes(readable);
					((GlueTestServiceArtifact) glueServiceArtifact).setRunProfile(unmarshalRunConfiguration(entry.getRepository(), new ByteArrayInputStream(bytes)));
				}
				finally {
					readable.close();
				}
			}
			else {
				((GlueTestServiceArtifact) glueServiceArtifact).setRunProfile(new RunProfileConfiguration());
			}
		}
		return glueServiceArtifact;
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, GlueServiceArtifact artifact) throws IOException {
		List<Validation<?>> save = super.save(entry, artifact);
		if (artifact instanceof GlueTestServiceArtifact) {
			Resource child = entry.getContainer().getChild("stub.xml");
			RunProfileConfiguration runProfile = ((GlueTestServiceArtifact) artifact).getRunProfile();
			if (runProfile != null) {
				if (child == null) {
					child = ((ManageableContainer<?>) entry.getContainer()).create("stub.xml", "application/xml");
				}
				WritableContainer<ByteBuffer> writable = ((WritableResource) child).getWritable();
				try {
					marshalRunConfiguration(entry.getRepository(), runProfile, IOUtils.toOutputStream(writable));
				}
				finally {
					writable.close();
				}
			}
			else if (child != null) {
				((ManageableContainer<?>) entry.getContainer()).delete("stub.xml");
			}
		}
		return save;
	}
	
	
}
