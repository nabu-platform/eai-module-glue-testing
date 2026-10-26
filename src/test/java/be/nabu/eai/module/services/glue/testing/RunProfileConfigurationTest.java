package be.nabu.eai.module.services.glue.testing;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.Test;

import be.nabu.eai.module.services.glue.testing.RunProfileConfiguration.ServiceConfiguration;

public class RunProfileConfigurationTest {
	@Test
	public void embeddedScriptRoundTrip() throws Exception {
		String xml = "<runProfile><profiles><configurations><mode>script</mode><script><![CDATA[@return\nname = \"A & B\"]]></script><output>{\"name\":\"static\"}</output></configurations></profiles></runProfile>";
		JAXBContext context = JAXBContext.newInstance(RunProfileConfiguration.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		RunProfileConfiguration parsed = (RunProfileConfiguration) unmarshaller.unmarshal(new StringReader(xml));
		ServiceConfiguration configuration = parsed.getProfiles().get(0).getConfigurations().get(0);
		Assert.assertEquals("script", configuration.getMode());
		Assert.assertEquals("@return\nname = \"A & B\"", configuration.getScript());
		Marshaller marshaller = context.createMarshaller();
		StringWriter writer = new StringWriter();
		marshaller.marshal(parsed, writer);
		RunProfileConfiguration restored = (RunProfileConfiguration) unmarshaller.unmarshal(new StringReader(writer.toString()));
		Assert.assertEquals(configuration.getScript(), restored.getProfiles().get(0).getConfigurations().get(0).getScript());
		Assert.assertEquals(configuration.getOutput(), restored.getProfiles().get(0).getConfigurations().get(0).getOutput());
	}

	@Test
	public void absentModeRemainsStatic() throws Exception {
		RunProfileConfiguration parsed = (RunProfileConfiguration) JAXBContext.newInstance(RunProfileConfiguration.class)
			.createUnmarshaller().unmarshal(new StringReader("<runProfile><profiles><configurations><output>{}</output></configurations></profiles></runProfile>"));
		Assert.assertNull(parsed.getProfiles().get(0).getConfigurations().get(0).getMode());
	}
}
