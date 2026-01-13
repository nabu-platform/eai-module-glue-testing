/*
* Copyright (C) 2020 Alexander Verbruggen
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

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "runProfile")
public class RunProfileConfiguration {
	public static class ServiceConfiguration {
		private List<String> inputQueries;
		private String output;
		private String errorMessage, errorCode;
		public List<String> getInputQueries() {
			return inputQueries;
		}
		public void setInputQueries(List<String> inputQueries) {
			this.inputQueries = inputQueries;
		}
		public String getOutput() {
			return output;
		}
		public void setOutput(String output) {
			this.output = output;
		}
		public String getErrorMessage() {
			return errorMessage;
		}
		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		public String getErrorCode() {
			return errorCode;
		}
		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}
		
	}
	
	public static class ServiceProfile {
		private DefinedService service;
		private List<ServiceConfiguration> configurations;
		public List<ServiceConfiguration> getConfigurations() {
			return configurations;
		}
		public void setConfigurations(List<ServiceConfiguration> configurations) {
			this.configurations = configurations;
		}
		@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
		public DefinedService getService() {
			return service;
		}
		public void setService(DefinedService service) {
			this.service = service;
		}
	}
	
	private List<ServiceProfile> profiles;
	private String description;

	public List<ServiceProfile> getProfiles() {
		return profiles;
	}
	public void setProfiles(List<ServiceProfile> profiles) {
		this.profiles = profiles;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
