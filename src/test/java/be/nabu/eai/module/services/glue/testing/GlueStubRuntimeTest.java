package be.nabu.eai.module.services.glue.testing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.services.glue.testing.RunProfileConfiguration.ServiceConfiguration;
import be.nabu.eai.module.services.glue.testing.RunProfileConfiguration.ServiceProfile;
import be.nabu.eai.repository.api.Repository;
import be.nabu.libs.cache.api.CacheProvider;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.SimpleTransactionContext;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.FeaturedExecutionContext;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.api.ServiceResult;
import be.nabu.libs.services.api.ServiceRunner;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.types.base.SimpleElementImpl;

public class GlueStubRuntimeTest {
	@Test
	public void nestedServiceCallBypassesStubAndRestoresInterception() throws Exception {
		Structure inputType = new Structure();
		inputType.add(new SimpleElementImpl<String>("value", new be.nabu.libs.types.simple.String(), inputType));
		Structure outputType = new Structure();
		outputType.add(new SimpleElementImpl<String>("value", new be.nabu.libs.types.simple.String(), outputType));
		ServiceInterface targetInterface = (ServiceInterface) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ServiceInterface.class }, (proxy, method, args) -> {
			if ("getInputDefinition".equals(method.getName())) {
				return inputType;
			}
			if ("getOutputDefinition".equals(method.getName())) {
				return outputType;
			}
			return null;
		});
		AtomicInteger calls = new AtomicInteger();
		DefinedService target = (DefinedService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { DefinedService.class }, (proxy, method, args) -> {
			if ("getId".equals(method.getName())) {
				return "target.lookup";
			}
			if ("getServiceInterface".equals(method.getName())) {
				return targetInterface;
			}
			if ("newInstance".equals(method.getName())) {
				return (be.nabu.libs.services.api.ServiceInstance) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { be.nabu.libs.services.api.ServiceInstance.class }, (instance, operation, parameters) -> {
					if (!"execute".equals(operation.getName())) {
						return null;
					}
					ExecutionContext execution = (ExecutionContext) parameters[0];
					ComplexContent input = (ComplexContent) parameters[1];
					calls.incrementAndGet();
					Assert.assertEquals("rewritten", input.get("value"));
					Assert.assertFalse(((FeaturedExecutionContext) execution).getEnabledFeatures().contains("test.owner"));
					Assert.assertTrue(((FeaturedExecutionContext) execution).getEnabledFeatures().contains("other"));
					((FeaturedExecutionContext) execution).getEnabledFeatures().add("test.owner");
					Assert.assertNotNull(((CacheProvider) execution.getServiceContext().getCacheProvider()).get("target.lookup"));
					ComplexContent output = outputType.newInstance();
					output.set("value", "real");
					return output;
				});
			}
			return null;
		});
		ServiceProfile profile = new ServiceProfile();
		profile.setService(target);
		ServiceConfiguration configuration = new ServiceConfiguration();
		configuration.setMode("script");
		configuration.setScript("original = target.lookup(\"rewritten\")\n@return\nvalue = original/value");
		profile.setConfigurations(Arrays.asList(configuration));
		Repository repository = (Repository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Repository.class }, (proxy, method, args) -> {
			if ("getName".equals(method.getName())) {
				return "test";
			}
			if ("getServices".equals(method.getName())) {
				return Arrays.asList(target);
			}
			if ("getServiceRunner".equals(method.getName())) {
				return (ServiceRunner) (service, execution, input, observers) -> {
					CompletableFuture<ServiceResult> result = new CompletableFuture<ServiceResult>();
					try {
						ComplexContent output = new ServiceRuntime(service, execution).run(input);
						result.complete((ServiceResult) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ServiceResult.class }, (p, m, a) -> "getOutput".equals(m.getName()) ? output : null));
					}
					catch (Exception e) {
						LoggerFactory.getLogger(getClass()).error("Nested service failed", e);
						result.completeExceptionally(e);
					}
					return result;
				};
			}
			return null;
		});
		ResourceContainer<?> directory = (ResourceContainer<?>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ResourceContainer.class }, (proxy, method, args) -> null);
		GlueTestServiceArtifact artifact = new GlueTestServiceArtifact("test.owner", directory, repository);
		artifact.getRunProfile().setProfiles(Arrays.asList(profile));
		List<String> enabled = new ArrayList<String>(Arrays.asList("test.owner", "TEST", "other"));
		ServiceContext services = (ServiceContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ServiceContext.class }, (proxy, method, args) -> {
			if ("getCacheProvider".equals(method.getName())) {
				return (CacheProvider) artifact;
			}
			if ("getServiceTrackerProvider".equals(method.getName())) {
				return (be.nabu.libs.services.api.ServiceRuntimeTrackerProvider) runtime -> null;
			}
			return "getServiceAuthorizerProvider".equals(method.getName()) ? (be.nabu.libs.services.api.ServiceAuthorizerProvider) runtime -> null : null;
		});
		ExecutionContext execution = (ExecutionContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ExecutionContext.class, FeaturedExecutionContext.class }, (proxy, method, args) -> {
			if ("getEnabledFeatures".equals(method.getName())) {
				return enabled;
			}
			if ("getTransactionContext".equals(method.getName())) {
				return new SimpleTransactionContext();
			}
			if ("getSecurityContext".equals(method.getName())) {
				return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { be.nabu.libs.services.api.SecurityContext.class }, (p, m, a) -> null);
			}
			return "getServiceContext".equals(method.getName()) ? services : null;
		});
		be.nabu.libs.services.api.DefinedServiceResolver resolver = id -> "target.lookup".equals(id) ? target : null;
		be.nabu.libs.services.DefinedServiceResolverFactory.getInstance().addResolver(resolver);
		Map<String, Object> previousGlobal = ServiceRuntime.getGlobalContext();
		Map<String, Object> global = new HashMap<String, Object>();
		List<String> inherited = new ArrayList<String>(enabled);
		global.put(ServiceRuntime.ENABLED_FEATURES, inherited);
		ServiceRuntime.setGlobalContext(global);
		try {
			ComplexContent input = inputType.newInstance();
			input.set("value", "incoming");
			ComplexContent output = new ServiceRuntime(target, execution).run(input);
			Assert.assertEquals("real", output.get("value"));
			Assert.assertEquals(1, calls.get());
			Assert.assertEquals(enabled, inherited);
			Assert.assertTrue(enabled.contains("test.owner"));
			configuration.setScript("throw(\"stub.failed\")");
			boolean failed = false;
			try {
				new ServiceRuntime(target, execution).run(input);
			}
			catch (be.nabu.libs.services.api.ServiceException e) {
				LoggerFactory.getLogger(getClass()).info("Expected failed stub", e);
				failed = true;
			}
			Assert.assertTrue("Failed stub should not fall through", failed);
			Assert.assertEquals(1, calls.get());
			ServiceRuntime probe = new ServiceRuntime(target, execution);
			probe.registerInThread(false);
			try {
				Assert.assertNotNull(artifact.get("target.lookup"));
			}
			finally {
				probe.unregisterInThread();
			}
		}
		finally {
			ServiceRuntime.setGlobalContext(previousGlobal);
			be.nabu.libs.services.DefinedServiceResolverFactory.getInstance().removeResolver(resolver);
		}
	}

	@Test
	public void scriptMapsTargetContractAndRestoresFeatures() throws Exception {
		Structure inputType = new Structure();
		Structure outputType = new Structure();
		ServiceInterface targetInterface = (ServiceInterface) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ServiceInterface.class }, (proxy, method, args) -> {
			if ("getInputDefinition".equals(method.getName())) {
				return inputType;
			}
			if ("getOutputDefinition".equals(method.getName())) {
				return outputType;
			}
			return null;
		});
		DefinedService target = (DefinedService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { DefinedService.class }, (proxy, method, args) -> {
			if ("getId".equals(method.getName())) {
				return "target.lookup";
			}
			if ("getServiceInterface".equals(method.getName())) {
				return targetInterface;
			}
			return null;
		});
		ServiceProfile profile = new ServiceProfile();
		profile.setService(target);
		ServiceConfiguration configuration = new ServiceConfiguration();
		configuration.setScript("@return\nextra = \"value\"");
		profile.setConfigurations(Arrays.asList(configuration));
		Repository repository = (Repository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Repository.class }, (proxy, method, args) -> {
			if ("getName".equals(method.getName())) {
				return "test";
			}
			return null;
		});
		ResourceContainer<?> directory = (ResourceContainer<?>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ResourceContainer.class }, (proxy, method, args) -> null);
		GlueTestServiceArtifact artifact = new GlueTestServiceArtifact("test.owner", directory, repository);
		List<String> enabled = new ArrayList<String>(Arrays.asList("test.owner", "TEST", "other"));
		ExecutionContext execution = (ExecutionContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ExecutionContext.class, FeaturedExecutionContext.class }, (proxy, method, args) -> "getEnabledFeatures".equals(method.getName()) ? enabled : null);
		ServiceRuntime runtime = new ServiceRuntime(target, execution);
		Map<String, Object> context = new HashMap<String, Object>();
		List<String> runtimeFeatures = new ArrayList<String>(Arrays.asList("test.owner", "other"));
		context.put(ServiceRuntime.ENABLED_FEATURES, runtimeFeatures);
		runtime.setContext(context);
		Map<String, Object> global = new HashMap<String, Object>();
		Map<String, Object> previousGlobal = ServiceRuntime.getGlobalContext();
		ServiceRuntime.setGlobalContext(global);
		runtime.registerInThread(false);
		try {
			Method execute = GlueTestServiceArtifact.class.getDeclaredMethod("executeStubScript", ServiceProfile.class, ServiceConfiguration.class, ComplexContent.class);
			execute.setAccessible(true);
			try {
				ComplexContent output = (ComplexContent) execute.invoke(artifact, profile, configuration, null);
				Assert.assertSame(outputType, output.getType());
			}
			catch (InvocationTargetException e) {
				LoggerFactory.getLogger(getClass()).error("Stub execution failed", e);
				throw (Exception) e.getCause();
			}
			configuration.setScript("throw(\"stub.failed\")");
			try {
				execute.invoke(artifact, profile, configuration, null);
				Assert.fail("A failed script must not return a stub result");
			}
			catch (InvocationTargetException e) {
				LoggerFactory.getLogger(getClass()).info("Expected stub failure", e);
				Assert.assertNotNull(e.getCause());
			}
			Assert.assertSame(context, runtime.getContext());
			Assert.assertSame(global, ServiceRuntime.getGlobalContext());
			Assert.assertEquals(Arrays.asList("test.owner", "TEST", "other"), enabled);
			Assert.assertEquals(Arrays.asList("test.owner", "other"), runtimeFeatures);
		}
		finally {
			runtime.unregisterInThread();
			ServiceRuntime.setGlobalContext(previousGlobal);
		}
	}
}
