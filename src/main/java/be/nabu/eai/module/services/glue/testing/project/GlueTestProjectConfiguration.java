package be.nabu.eai.module.services.glue.testing.project;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "testProject")
@XmlType(propOrder = { "amountOfThreads", "maxScriptRuntime", "tests", "serviceContext", "features" })
public class GlueTestProjectConfiguration {
	// the service context where the test should be run in
	private String serviceContext;
	private List<DefinedService> tests;
	private Integer amountOfThreads;
	private Long maxScriptRuntime;
	// the features you want enabled for this project
	private List<String> features;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.services.glue.testing.TestCase.run")	
	public List<DefinedService> getTests() {
		return tests;
	}
	public void setTests(List<DefinedService> tests) {
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
	
	public String getServiceContext() {
		return serviceContext;
	}
	public void setServiceContext(String serviceContext) {
		this.serviceContext = serviceContext;
	}
	public List<String> getFeatures() {
		return features;
	}
	public void setFeatures(List<String> features) {
		this.features = features;
	}

}
