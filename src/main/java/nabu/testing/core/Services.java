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

package nabu.testing.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import be.nabu.eai.module.services.glue.testing.GlueTestServiceArtifact;
import be.nabu.eai.module.services.glue.testing.project.GlueTestProjectArtifact;
import be.nabu.eai.module.services.glue.testing.project.GlueTestProjectArtifact.GlueTestProjectOutput;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.glue.api.ExecutionException;
import be.nabu.glue.api.Executor;
import be.nabu.glue.api.ExecutorContext;
import be.nabu.glue.api.ExecutorGroup;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.core.impl.methods.TestMethods;
import be.nabu.glue.core.impl.parsers.GlueParserProvider;
import be.nabu.glue.core.repositories.DynamicScript;
import be.nabu.glue.core.repositories.DynamicScriptRepository;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.formatted.FormattedValidation;
import be.nabu.glue.services.CombinedExecutionContextImpl;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.libs.datastore.api.DataProperties;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.memory.MemoryDirectory;
import be.nabu.libs.resources.memory.MemoryItem;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import nabu.testing.core.types.TestAttachment;

@WebService
public class Services {

	private ExecutionContext executionContext;
	private boolean generated;
	
	public GlueTestProjectOutput runScript(@WebParam(name = "script") String script, @WebParam(name = "id") String id, @WebParam(name = "matrix") String matrix, @WebParam(name = "resources") List<URI> resources, @WebParam(name = "attachments") List<TestAttachment> attachments, @WebParam(name = "features") List<String> features) throws IOException, ServiceException, ParseException, URISyntaxException {
		MemoryDirectory root = new MemoryDirectory();
		MemoryDirectory privateDirectory = (MemoryDirectory) root.create(EAIResourceRepository.PRIVATE, Resource.CONTENT_TYPE_DIRECTORY);
		GlueTestServiceArtifact test = new GlueTestServiceArtifact(id, root, EAIResourceRepository.getInstance());
		test.setUseJsonFormatting(true);
		test.setContent(script);
		if (matrix != null) {
			MemoryItem matrixResource = (MemoryItem) privateDirectory.create("input.matrix.csv", "text/plain");
			try (WritableContainer<ByteBuffer> writable = matrixResource.getWritable()) {
				writable.write(IOUtils.wrap(matrix.getBytes(Charset.forName("UTF-8")), true));
			}
		}
		if (resources != null) {
			for (URI resource : resources) {
				if (resource == null) {
					continue;
				}
				nabu.frameworks.datastore.Services datastore = nabu.frameworks.datastore.Services.getInstance(ServiceRuntime.getRuntime());
				DataProperties properties = datastore.properties(resource);
				if (properties == null) {
					throw new IllegalArgumentException("Could not resolve: " + resource);
				}
				MemoryItem targetResource = (MemoryItem) privateDirectory.create(properties.getName(), properties.getContentType());
				try (WritableContainer<ByteBuffer> writable = targetResource.getWritable(); ReadableContainer<ByteBuffer> readable = IOUtils.wrap(datastore.retrieve(resource, false))) {
					IOUtils.copyBytes(readable, writable);
				}
			}
		}
		if (attachments != null) {
			for (TestAttachment attachment : attachments) {
				if (attachment == null) {
					continue;
				}
				MemoryItem targetResource = (MemoryItem) privateDirectory.create(attachment.getName(), attachment.getContentType());
				try (WritableContainer<ByteBuffer> writable = targetResource.getWritable()) {
					writable.write(IOUtils.wrap(attachment.getContent(), true));
				}
			}
		}
		GlueTestProjectArtifact glueTestProjectArtifact = new GlueTestProjectArtifact(id + ".$project", root, EAIResourceRepository.getInstance());
		if (features != null) {
			glueTestProjectArtifact.getConfig().setFeatures(features);
		}
		glueTestProjectArtifact.getConfig().setTests(Arrays.asList(test));
		ComplexContent output = glueTestProjectArtifact.newInstance().execute(executionContext, null);
		return (GlueTestProjectOutput) output.get("result");
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateEquals(@WebParam(name = "message") String message, @WebParam(name = "expected") Object expected, @WebParam(name = "actual") Object actual) throws IOException {
		start();
		try {
			TestMethods.validateEquals(message, expected, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateNotEquals(@WebParam(name = "message") String message, @WebParam(name = "expected") Object expected, @WebParam(name = "actual") Object actual) throws IOException {
		start();
		try {
			TestMethods.validateNotEquals(message, expected, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateTrue(@WebParam(name = "message") String message, @WebParam(name = "actual") Boolean actual) throws IOException {
		start();
		try {
			TestMethods.validateTrue(message, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}

	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateFalse(@WebParam(name = "message") String message, @WebParam(name = "actual") Boolean actual) throws IOException {
		start();
		try {
			TestMethods.validateFalse(message, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateMatches(@WebParam(name = "message") String message, @WebParam(name = "regex") String regex, @WebParam(name = "actual") String actual) throws IOException {
		start();
		try {
			TestMethods.validateMatches(message, regex, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateNotMatches(@WebParam(name = "message") String message, @WebParam(name = "regex") String regex, @WebParam(name = "actual") String actual) throws IOException {
		start();
		try {
			TestMethods.validateNotMatches(message, regex, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}

	private void start() {
		ScriptRuntime runtime = ScriptRuntime.getRuntime();
		if (runtime == null) {
			generated = true;
			runtime = generateRuntime();
			runtime.registerInThread();
		}
	}
	private void stop() {
		if (generated) {
			ScriptRuntime.getRuntime().unregisterInThread();
		}
	}
	
	private ScriptRuntime generateRuntime() {
		GlueParserProvider provider = new GlueParserProvider(new ServiceMethodProvider(EAIResourceRepository.getInstance(), EAIResourceRepository.getInstance()));
		ScriptRepository scriptRepository = new DynamicScriptRepository(provider);
		try {
			// get the id of the parent service, the current service is the validation routine
			String id = ((DefinedService) ServiceRuntime.getRuntime().getParent().getService()).getId();
			DynamicScript script = new DynamicScript(
				id.indexOf('.') > 0 ? id.substring(0, id.lastIndexOf('.')) : null,
				id.indexOf('.') > 0 ? id.substring(id.lastIndexOf('.') + 1) : id,
				scriptRepository,
				Charset.defaultCharset(),
				null
			);
			CombinedExecutionContextImpl executionEnvironment =	new CombinedExecutionContextImpl(executionContext, new SimpleExecutionEnvironment("local"), null);
			ScriptRuntime runtime = new ScriptRuntime(script, executionEnvironment, new HashMap<String, Object>());
			runtime.getExecutionContext().setCurrent(new Executor() {
				@Override
				public boolean shouldExecute(be.nabu.glue.api.ExecutionContext context) throws ExecutionException {
					return true;
				}
				@Override
				public void execute(be.nabu.glue.api.ExecutionContext context) throws ExecutionException {
					// do nothing
				}
				@Override
				public ExecutorContext getContext() {
					return null;
				}
				@Override
				public String getId() {
					return id;
				}
				@Override
				public ExecutorGroup getParent() {
					return null;
				}
				@Override
				public boolean isGenerated() {
					return true;
				}
			});
			return runtime;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
