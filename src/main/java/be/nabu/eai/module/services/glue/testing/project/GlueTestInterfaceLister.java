package be.nabu.eai.module.services.glue.testing.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import be.nabu.eai.developer.api.InterfaceLister;
import be.nabu.eai.developer.util.InterfaceDescriptionImpl;

public class GlueTestInterfaceLister implements InterfaceLister {

	private static Collection<InterfaceDescription> descriptions = null;
	
	@Override
	public Collection<InterfaceDescription> getInterfaces() {
		if (descriptions == null) {
			synchronized(GlueTestInterfaceLister.class) {
				if (descriptions == null) {
					List<InterfaceDescription> descriptions = new ArrayList<InterfaceDescription>();
					descriptions.add(new InterfaceDescriptionImpl("Testing", "Test Case", "be.nabu.eai.module.services.glue.testing.TestCase.run"));
					GlueTestInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}

}
