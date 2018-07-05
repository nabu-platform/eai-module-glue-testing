package be.nabu.eai.module.services.glue.testing.project;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.eai.module.services.glue.testing.project.GlueTestProjectArtifact.GlueTestProjectOutput;

public interface GlueTestProjectInterface {
	@WebResult(name = "result")
	public GlueTestProjectOutput run(@WebParam(name = "amountOfThreads") Integer amountOfThreads, @WebParam(name = "maxScriptRuntime") Long maxScriptRuntime);
}