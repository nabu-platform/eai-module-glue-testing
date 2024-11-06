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
