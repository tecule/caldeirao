package com.sinosoft.caldeirao;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;

import com.sinosoft.openstack.CloudConfig;
import com.sinosoft.openstack.CloudManipulator;
import com.sinosoft.openstack.CloudManipulatorFactory;


/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		testClient();
		System.out.println("Hello World!");
	}
	
	@SuppressWarnings("unused")
	private static void testClient() {
		
		CloudConfig config = new CloudConfig();
		config.setCloudManipulatorVersion("v3");
		config.setAuthUrl("http://192.168.101.121:5000/v3");
		config.setAdminUsername("admin");
		config.setAdminPassword("123456");
		config.setPublicNetworkId("cd3a36fb-6c72-4d99-bb5e-04269eb1c1f2");
		config.setAdminUserId("7c7caee4862249cf97347e69c4cc603d");
		config.setDomainName("default");
		config.setDomainId("70167b6b0c6e4955a19983e94cb02239");
		config.setAdminProjectId("f00fa7550d56409f91c2162821ce759b");
		config.setAdminRoleName("admin");
		config.setAodhServiceUrl("http://192.168.101.121:8042/v2/alarms");		
		
		// Get a project-scoped token:
		OSClientV3 projectClient = OSFactory.builderV3().endpoint("http://192.168.101.121:5000/v3")
				.credentials("admin", "123456", Identifier.byName("default"))
				.scopeToProject(Identifier.byId("f00fa7550d56409f91c2162821ce759b")).authenticate();
		
		// Get a domain-scoped token (Note that you’re going to need a role-assignment on the domain first!)
		// http://docs.openstack.org/developer/keystone/api_curl_examples.html
		OSClientV3 domainClient = OSFactory.builderV3().endpoint("http://192.168.101.121:5000/v3").credentials("7c7caee4862249cf97347e69c4cc603d", "123456")
				.scopeToDomain(Identifier.byId("70167b6b0c6e4955a19983e94cb02239")).authenticate();
		
		CloudManipulator cloud = CloudManipulatorFactory.createCloudManipulator(config);
		cloud.createProject("test1", "d", 1, 1, 1);
		
		return;
	}
}
