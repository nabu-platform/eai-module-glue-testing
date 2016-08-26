package be.nabu.eai.module.services.glue.testing.project;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.services.glue.testing.GlueTestServiceArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

@XmlRootElement(name = "testProject")
@XmlType(propOrder = { "amountOfThreads", "maxScriptRuntime", "tests" })
public class GlueTestProjectConfiguration {
	
	private List<GlueTestServiceArtifact> tests;
	private Integer amountOfThreads;
	private Long maxScriptRuntime;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<GlueTestServiceArtifact> getTests() {
		return tests;
	}
	public void setTests(List<GlueTestServiceArtifact> tests) {
		this.tests = tests;
	}
	
	@EnvironmentSpecific
	public Integer getAmountOfThreads() {
		return amountOfThreads;
	}
	public void setAmountOfThreads(Integer amountOfThreads) {
		this.amountOfThreads = amountOfThreads;
	}
	
	@EnvironmentSpecific
	public Long getMaxScriptRuntime() {
		return maxScriptRuntime;
	}
	public void setMaxScriptRuntime(Long maxScriptRuntime) {
		this.maxScriptRuntime = maxScriptRuntime;
	}
}
