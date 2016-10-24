package com.sinosoft.openstack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV2;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.IPProtocol;
import org.openstack4j.model.compute.QuotaSet;
import org.openstack4j.model.compute.RebootType;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.ServerUpdateOptions;
import org.openstack4j.model.compute.VNCConsole;
import org.openstack4j.model.compute.VNCConsole.Type;
import org.openstack4j.model.compute.VolumeAttachment;
import org.openstack4j.model.compute.actions.LiveMigrateOptions;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.identity.v2.Role;
import org.openstack4j.model.identity.v2.Tenant;
import org.openstack4j.model.identity.v2.User;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.network.AttachInterfaceType;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Pool;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.options.PortListOptions;
import org.openstack4j.model.storage.block.BlockLimits;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.BlockLimits.Absolute;
import org.openstack4j.model.telemetry.Alarm;
import org.openstack4j.model.telemetry.Alarm.ThresholdRule;
import org.openstack4j.model.telemetry.MeterSample;
import org.openstack4j.model.telemetry.SampleCriteria;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.telemetry.domain.CeilometerAlarm;
import org.openstack4j.openstack.telemetry.domain.CeilometerAlarm.CeilometerQuery;
import org.openstack4j.openstack.telemetry.domain.CeilometerAlarm.CeilometerThresholdRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudManipulatorV2 implements CloudManipulator {
	private static Logger logger = LoggerFactory.getLogger(CloudManipulatorV2.class);

	private String OS_AUTH_URL;
	private String OS_USERNAME;
	private String OS_PASSWORD;
	private String OS_TENANT_NAME;
	private String PUBLIC_NETWORK_ID;

	private String projectId;
	private OSClientV2 tenantClient;

	public CloudManipulatorV2(CloudConfig appConfig) {
		OS_AUTH_URL = appConfig.getAuthUrl();
		OS_USERNAME = appConfig.getAdminUsername();
		OS_PASSWORD = appConfig.getAdminPassword();
		OS_TENANT_NAME = appConfig.getAdminProjectName();
		PUBLIC_NETWORK_ID = appConfig.getPublicNetworkId();

		this.projectId = null;
		tenantClient = OSFactory.builderV2().endpoint(OS_AUTH_URL).credentials(OS_USERNAME, OS_PASSWORD)
				.tenantName(OS_TENANT_NAME).authenticate();
	}

	public CloudManipulatorV2(CloudConfig appConfig, String projectId) {
		OS_AUTH_URL = appConfig.getAuthUrl();
		OS_USERNAME = appConfig.getAdminUsername();
		OS_PASSWORD = appConfig.getAdminPassword();
		OS_TENANT_NAME = appConfig.getAdminProjectName();
		PUBLIC_NETWORK_ID = appConfig.getPublicNetworkId();

		this.projectId = projectId;
		tenantClient = OSFactory.builderV2().endpoint(OS_AUTH_URL).credentials(OS_USERNAME, OS_PASSWORD)
				.tenantId(projectId).authenticate();
	}

	@Override
	public String createProject(String projectName, String projectDescription, int instanceQuota, int cpuQuota,
			int memoryQuota) {
		OSClientV2 client = OSFactory.builderV2().endpoint(OS_AUTH_URL).credentials(OS_USERNAME, OS_PASSWORD)
				.tenantName(OS_TENANT_NAME).perspective(Facing.ADMIN).authenticate();

		// create tenant
		Tenant tenant = client
				.identity()
				.tenants()
				.create(Builders.identityV2().tenant().name(projectName + "_" + UUID.randomUUID().toString())
						.description(projectDescription).build());
		String tenantId = tenant.getId();

		// set quota
		client.compute()
				.quotaSets()
				.updateForTenant(tenantId,
						Builders.quotaSet().instances(instanceQuota).cores(cpuQuota).ram(memoryQuota * 1024).build());

		// set user permission
		User adminUser = client.identity().users().getByName(OS_USERNAME);
		Role adminRole = client.identity().roles().getByName("admin");
		ActionResponse response = client.identity().roles().addUserRole(tenantId, adminUser.getId(), adminRole.getId());
		if (false == response.isSuccess()) {
			logger.error(response.getFault());
		}

		// build network and router
		Network network = client.networking().network()
				.create(Builders.network().name("private").tenantId(tenantId).adminStateUp(true).build());
		// .addDNSNameServer("124.16.136.254")
		Subnet subnet = client
				.networking()
				.subnet()
				.create(Builders.subnet().name("private_subnet").networkId(network.getId()).tenantId(tenantId)
						.ipVersion(IPVersionType.V4).cidr("192.168.32.0/24").gateway("192.168.32.1").enableDHCP(true)
						.build());
		Router router = client
				.networking()
				.router()
				.create(Builders.router().name("router").adminStateUp(true).externalGateway(PUBLIC_NETWORK_ID)
						.tenantId(tenantId).build());
		RouterInterface iface = client.networking().router()
				.attachInterface(router.getId(), AttachInterfaceType.SUBNET, subnet.getId());

		// adjust security group
		OSClientV2 tenantClient = OSFactory.builderV2().endpoint(OS_AUTH_URL).credentials(OS_USERNAME, OS_PASSWORD)
				.tenantId(tenantId).authenticate();
		List<? extends SecGroupExtension> secGroups = tenantClient.compute().securityGroups().list();
		for (SecGroupExtension secGroup : secGroups) {
			tenantClient
					.compute()
					.securityGroups()
					.createRule(
							Builders.secGroupRule().cidr("0.0.0.0/0").parentGroupId(secGroup.getId())
									.protocol(IPProtocol.ICMP).range(-1, -1).build());
			tenantClient
					.compute()
					.securityGroups()
					.createRule(
							Builders.secGroupRule().cidr("0.0.0.0/0").parentGroupId(secGroup.getId())
									.protocol(IPProtocol.TCP).range(1, 65535).build());
			tenantClient
					.compute()
					.securityGroups()
					.createRule(
							Builders.secGroupRule().cidr("0.0.0.0/0").parentGroupId(secGroup.getId())
									.protocol(IPProtocol.UDP).range(1, 65535).build());
		}

		return tenantId;
	}

	@Override
	public QuotaSet updateProjectComputeServiceQuota(int instanceQuota, int cpuQuota, int memoryQuota) {
		OSClientV2 client = OSFactory.builderV2().endpoint(OS_AUTH_URL).credentials(OS_USERNAME, OS_PASSWORD)
				.tenantId(projectId).perspective(Facing.ADMIN).authenticate();

		QuotaSet quota = client
				.compute()
				.quotaSets()
				.updateForTenant(projectId,
						Builders.quotaSet().cores(cpuQuota).instances(instanceQuota).ram(memoryQuota * 1024).build());

		return quota;
	}

	@Override
	public Absolute getProjectBlockStorageQuotaUsage() {
		BlockLimits limits = tenantClient.blockStorage().getLimits();
		Absolute absolute = limits.getAbsolute();

		return absolute;
	}

	@Override
	public BlockQuotaSet updateProjectBlockStorageQuota(int volumes, int gigabytes) {
		BlockQuotaSet quota = tenantClient.blockStorage().quotaSets()
				.updateForTenant(projectId, Builders.blockQuotaSet().volumes(volumes).gigabytes(gigabytes).build());
		return quota;
	}
	
	@Override
	public boolean deleteProject() {
		ActionResponse response;
		OSClientV2 client = OSFactory.builderV2().endpoint(OS_AUTH_URL).credentials(OS_USERNAME, OS_PASSWORD)
				.tenantId(projectId).perspective(Facing.ADMIN).authenticate();

		// check if this tenant has vm
		List<? extends Server> servers = client.compute().servers().list();
		if (servers.size() > 0) {
			logger.error("项目包含虚拟机，不允许删除");
			return false;
		}

		// get internal subnet
		List<Network> tenantNetworks = new ArrayList<Network>();
		List<Subnet> tenantSubnets = new ArrayList<Subnet>();
		List<? extends Network> networks = client.networking().network().list();
		for (Network network : networks) {
			if (network.getTenantId().equalsIgnoreCase(projectId)) {
				tenantNetworks.add(network);
				tenantSubnets.addAll(network.getNeutronSubnets());
			}
		}

		// delete router
		List<? extends Router> routers = client.networking().router().list();
		for (Router router : routers) {
			if (router.getTenantId().equalsIgnoreCase(projectId)) {
				// detach from internal network
				for (Subnet subnet : tenantSubnets) {
					client.networking().router().detachInterface(router.getId(), subnet.getId(), null);
				}

				response = client.networking().router().delete(router.getId());
				if (response.isSuccess() == false) {
					logger.error("删除路由器失败。" + response.getFault());
					return false;
				}
			}
		}

		// delete tenant network
		for (Network network : tenantNetworks) {
			response = client.networking().network().delete(network.getId());
			if (response.isSuccess() == false) {
				logger.error("删除网络失败。" + response.getFault());
				return false;
			}
		}

		response = client.identity().tenants().delete(projectId);
		if (response.isSuccess() == false) {
			logger.error("删除项目失败。" + response.getFault());
			return false;
		}

		return true;
	}

	@Override
	public List<? extends Volume> getVolumes() {
		List<? extends Volume> volumes = tenantClient.blockStorage().volumes().list();
		return volumes;
	}

	@Override
	public Volume createVolume(String volumeName, String volumeDescription, int volumeCapacity) {
		Volume volume = tenantClient.blockStorage().volumes()
				.create(Builders.volume().name(volumeName).description(volumeDescription).size(volumeCapacity).build());
		return volume;
	}

	@Override
	public boolean modifyVolume(String volumeId, String volumeName, String volumeDescription) {
		ActionResponse response = tenantClient.blockStorage().volumes().update(volumeId, volumeName, volumeDescription);
		if (false == response.isSuccess()) {
			logger.error(response.getFault());
			return false;
		}

		return true;
	}
	
	@Override
	public Volume getVolume(String volumeId) {
		Volume volume = tenantClient.blockStorage().volumes().get(volumeId);
		return volume;
	}

	@Override
	public boolean deleteVolume(String volumeId) {
		ActionResponse response = tenantClient.blockStorage().volumes().delete(volumeId);
		if (response.isSuccess()) {
			return true;
		} else {
			logger.error(response.getFault());
			return false;
		}
	}

	@Override
	public boolean attachVolume(String serverId, String volumeId) {
		VolumeAttachment attachment = tenantClient.compute().servers().attachVolume(serverId, volumeId, null);
		if (null == attachment) {
			return false;
		}

		return true;
	}

	@Override
	public boolean detachVolume(String serverId, String diskId) {
		ActionResponse response = tenantClient.compute().servers().detachVolume(serverId, diskId);
		if (false == response.isSuccess()) {
			logger.error(response.getFault());
			return false;
		}

		return true;
	}

	@Override
	public void waitVolumeStatus(String volumeId, org.openstack4j.model.storage.block.Volume.Status status, int minute)
			throws InterruptedException {
		int sleepInterval = 6000;
		int sleepCount = minute * 60 * 1000 / sleepInterval;

		int loop = 0;
		while (loop < sleepCount) {
			Volume volume = tenantClient.blockStorage().volumes().get(volumeId);
			if ((volume.getStatus() != null) && (volume.getStatus() == status)) {
				break;
			}

			Thread.sleep(sleepInterval);
			loop++;
		}

		return;
	}

	@Override
	public List<? extends Image> getImages() {
		List<? extends Image> images = tenantClient.images().list();
		return images;
	}

	@Override
	public Image getImage(String imageId) {
		Image image = tenantClient.images().get(imageId);
		return image;
	}

	@Override
	public void waitImageStatus(String imageId, org.openstack4j.model.image.Image.Status status, int minute)
			throws InterruptedException {
		int sleepInterval = 6000;
		int sleepCount = minute * 60 * 1000 / sleepInterval;

		int loop = 0;
		while (loop < sleepCount) {
			Image image = tenantClient.images().get(imageId);
			if ((image.getStatus() != null) && (image.getStatus() == status)) {
				break;
			}

			Thread.sleep(sleepInterval);
			loop++;
		}

		return;
	}

	@Override
	public Image updateImage(String imageId, String imageName, boolean publicity) {
		Image image = tenantClient.images().get(imageId);
		Image newImage = tenantClient.images().update(image.toBuilder().name(imageName).isPublic(publicity).build());
		return newImage;
	}

	@Override
	public boolean deleteImage(String imageId) {
		ActionResponse response = tenantClient.images().delete(imageId);
		if (false == response.isSuccess()) {
			logger.error(response.getFault());
			return false;
		}

		return true;
	}

	@Override
	public List<? extends Hypervisor> getHypervisors() {
		List<? extends Hypervisor> hypervisors = tenantClient.compute().hypervisors().list();
		return hypervisors;
	}

	@Override
	public List<? extends Server> getServers() {
		List<? extends Server> servers = tenantClient.compute().servers().list();
		return servers;
	}

	@Override
	public Flavor getFlavor(int cpu, int memory, int disk) {
		Flavor flavor = null;
		List<? extends Flavor> flavors = tenantClient.compute().flavors().list();
		Iterator<? extends Flavor> it = flavors.iterator();
		while (it.hasNext()) {
			Flavor f = (Flavor) it.next();
			if (f.getRam() == memory * 1024 && f.getDisk() == disk && f.getVcpus() == cpu) {
				flavor = f;
				break;
			}
		}

		return flavor;
	}

	@Override
	public Flavor createFlavor(int cpu, int memory, int disk) {
		Flavor flavor = null;
		String flavorName = "cpu" + cpu + "_mem" + memory + "_disk" + disk;
		// TODO why rxtxFactor != 1.0
		flavor = Builders.flavor().name(flavorName).ram(memory * 1024).vcpus(cpu).disk(disk).rxtxFactor(1.2f).build();
		flavor = tenantClient.compute().flavors().create(flavor);

		return flavor;
	}

	@Override
	public Server bootServer(String serverName, String flavorId, String imageId) {
		Server server = tenantClient.compute().servers()
				.boot(Builders.server().name(serverName).flavor(flavorId).image(imageId).build());
		return server;

	}

	@Override
	public void waitServerStatus(String serverId, List<Status> statusList, int minute) throws InterruptedException {
		int sleepInterval = 6000;
		int sleepCount = minute * 60 * 1000 / sleepInterval;

		int loop = 0;
		boolean exitWait = false;
		// wait until vm is ready
		while (loop < sleepCount) {
			Server server = tenantClient.compute().servers().get(serverId);
			if (server.getStatus() != null) {
				// TODO: user may change server status
				for (Status status : statusList) {
					if (server.getStatus() == status) {
						exitWait = true;
						break;
					}
				}
			}

			if (exitWait == true) {
				break;
			}

			Thread.sleep(sleepInterval);
			loop++;
		}

		return;
	}

	@Override
	public Server getServer(String serverId) {
		Server server = tenantClient.compute().servers().get(serverId);
		return server;
	}

	@Override
	public boolean startServer(String serverId) {
		Server server = tenantClient.compute().servers().get(serverId);
		Status status = server.getStatus();
		if (false == status.name().equalsIgnoreCase("ACTIVE")) {
			ActionResponse response = tenantClient.compute().servers().action(serverId, Action.START);
			if (false == response.isSuccess()) {
				logger.error(response.getFault());
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean rebootServer(String serverId) {
		Server server = tenantClient.compute().servers().get(serverId);
		Status status = server.getStatus();
		if (true == status.name().equalsIgnoreCase("ACTIVE")) {
			ActionResponse response = tenantClient.compute().servers().reboot(serverId, RebootType.SOFT);
			if (false == response.isSuccess()) {
				logger.error(response.getFault());
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean stopServer(String serverId) {
		Server server = tenantClient.compute().servers().get(serverId);
		Status status = server.getStatus();
		if (true == status.name().equalsIgnoreCase("ACTIVE")) {
			ActionResponse response = tenantClient.compute().servers().action(serverId, Action.STOP);
			if (false == response.isSuccess()) {
				logger.error(response.getFault());
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean deleteServer(String serverId) {
		ActionResponse response = tenantClient.compute().servers().delete(serverId);
		if (false == response.isSuccess()) {
			logger.error(response.getFault());
			return false;
		}

		return true;
	}

	@Override
	public VNCConsole getServerVNCConsole(String serverId) {
		VNCConsole console = tenantClient.compute().servers().getVNCConsole(serverId, Type.NOVNC);
		return console;
	}

	@Override
	public Server renameServer(String serverId, String newName) {
		Server server = tenantClient.compute().servers().update(serverId, ServerUpdateOptions.create().name(newName));
		return server;
	}

	@Override
	public boolean associateFloatingIp(String serverId, String floatingIpAddress) {
		// allocate all free ips from pool
		String pool = tenantClient.compute().floatingIps().getPoolNames().get(0);
		while (true) {
			try {
				tenantClient.compute().floatingIps().allocateIP(pool);
			} catch (Exception e) {
				break;
			}
		}

		// associate ip if it's free
		// deallocate other free ip back to pool, so other project can make use of it
		List<? extends FloatingIP> availableIps = tenantClient.compute().floatingIps().list();
		Server server = tenantClient.compute().servers().get(serverId);
		boolean associated = false;
		for (FloatingIP ip : availableIps) {
			if (ip.getInstanceId() == null) {
				if (ip.getFloatingIpAddress().equalsIgnoreCase(floatingIpAddress)) {
					ActionResponse response = tenantClient.compute().floatingIps()
							.addFloatingIP(server, floatingIpAddress);
					if (true == response.isSuccess()) {
						associated = true;
					} else {
						logger.error(response.getFault());
					}

					continue;
				}

				// deallocate unused ip back to the pool
				ActionResponse response2 = tenantClient.compute().floatingIps().deallocateIP(ip.getId());
				if (false == response2.isSuccess()) {
					logger.error("response.getFault()");
				}
			}
		}

		return associated;
	}

	private boolean deallocate2(OSClientV2 tenantClient, Server server, String floatingIpAddress) {
		// disassociate ip from server
		ActionResponse response = tenantClient.compute().floatingIps().removeFloatingIP(server, floatingIpAddress);
		if (false == response.isSuccess()) {
			logger.error(response.getFault());
			return false;
		}

		// deallocate ip back to pool
		List<? extends FloatingIP> floatingIps = tenantClient.compute().floatingIps().list();
		for (FloatingIP floatingIp : floatingIps) {
			if (floatingIp.getFloatingIpAddress().equalsIgnoreCase(floatingIpAddress)) {
				ActionResponse response2 = tenantClient.compute().floatingIps().deallocateIP(floatingIp.getId());
				if (true == response2.isSuccess()) {
					return true;
				} else {
					logger.error(response2.getFault());
					return false;
				}
			}
		}

		return false;
	}

	@Override
	public boolean deallocateFloatingIp(String serverId, String floatingIpAddress) {
		Server server = tenantClient.compute().servers().get(serverId);
		Iterator<List<? extends Address>> it = server.getAddresses().getAddresses().values().iterator();
		while (it.hasNext()) {
			List<? extends Address> addresses = it.next();
			for (Address address : addresses) {
				String addrType = address.getType(), addr = address.getAddr();

				if (addrType.equalsIgnoreCase("floating") && (addr.equalsIgnoreCase(floatingIpAddress))) {
					boolean deallocated = deallocate2(tenantClient, server, floatingIpAddress);
					return deallocated;
				}
			}
		}

		return false;
	}

	@Override
	public String createSnapshot(String serverId, String snapshotName) {
		String snapshotId = tenantClient.compute().servers().createSnapshot(serverId, snapshotName);
		return snapshotId;
	}

	@Override
	public boolean liveMigrate(String serverId, String hypervisorName) {
		String host = hypervisorName;
		int fqdnDotPosition = hypervisorName.indexOf('.');
		if (fqdnDotPosition >= 0) {
			host = hypervisorName.substring(0, fqdnDotPosition);
		}

		// kilo use host name in live migration
		// use host name when live migration, NOT FQDN!!!
		ActionResponse response = tenantClient.compute().servers()
				.liveMigrate(serverId, LiveMigrateOptions.create().host(host));
		if (false == response.isSuccess()) {
			logger.error(response.getFault());
			return false;
		}

		return true;
	}

	@Override
	public Map<String, String> getServerInfo(String serverId) {
		Map<String, String> map = new HashMap<String, String>();

		Server server = tenantClient.compute().servers().get(serverId);

		String privateIp = "", floatingIp = "";
		Iterator<List<? extends Address>> it = server.getAddresses().getAddresses().values().iterator();
		while (it.hasNext()) {
			List<? extends Address> addresses = it.next();
			for (int i = 0; i < addresses.size(); i++) {
				// use addresses.get(i).getType().equalsIgnoreCase("fixed")
				// instead of addresses.get(i).getType() == "fixed"
				if (addresses.get(i).getType().equalsIgnoreCase("fixed")) {
					// privateIp += addresses.get(i).getAddr() + ", ";
					privateIp = addresses.get(i).getAddr();
				} else if (addresses.get(i).getType().equalsIgnoreCase("floating")) {
					// floatingIp += addresses.get(i).getAddr() + ", ";
					floatingIp = addresses.get(i).getAddr();
				}
			}
		}
		// if (privateIp.length() > 2) {
		// privateIp = privateIp.substring(0, privateIp.length() - 2);
		// }
		// if (floatingIp.length() > 2) {
		// floatingIp = floatingIp.substring(0, floatingIp.length() - 2);
		// }

		String physicalMachine = server.getHypervisorHostname();

		map.put("privateIp", privateIp);
		map.put("floatingIp", floatingIp);
		map.put("physicalMachine", physicalMachine);

		return map;
	}

	private String getResourceId(String serverId, String meterName) {
		List<String> serverResourceMeters = new ArrayList<String>(Arrays.asList("cpu_util", "memory.resident",
				"disk.read.bytes.rate", "disk.write.bytes.rate"));
		List<String> networkResourceMeters = new ArrayList<String>(Arrays.asList("network.outgoing.bytes.rate",
				"network.incoming.bytes.rate"));

		// get resource id
		String resourceId;
		if (serverResourceMeters.contains(meterName)) {
			resourceId = serverId;
		} else if (networkResourceMeters.contains(meterName)) {
			// get network port resource id
			List<? extends Port> ports = tenantClient.networking().port()
					.list(PortListOptions.create().deviceId(serverId));
			// TODO: assume ports length > 0
			String networkResourceId = tenantClient.compute().servers().get(serverId).getInstanceName() + "-"
					+ serverId + "-tap" + ports.get(0).getId();
			resourceId = networkResourceId.substring(0, 69);
		} else {
			logger.error("无效的监控指标");
			return null;
		}

		return resourceId;
	}

	@Override
	public String createAlarm(String serverId, String alarmName, String meterName, float threshold) {
		// get resource id
		String resourceId = getResourceId(serverId, meterName);
		if (null == resourceId) {
			return null;
		}

		CeilometerQuery query = new CeilometerQuery();
		query.setField("resource_id");
		query.setOp(Alarm.ThresholdRule.ComparisonOperator.EQ);
		query.setValue(resourceId);
		List<CeilometerQuery> queries = new ArrayList<CeilometerAlarm.CeilometerQuery>();
		queries.add(query);

		CeilometerThresholdRule rule = new CeilometerThresholdRule();
		rule.setMeterName(meterName);
		rule.setThreshold(threshold);
		rule.setComparisonOperator(Alarm.ThresholdRule.ComparisonOperator.GE);
		rule.setStatistic(Alarm.ThresholdRule.Statistic.AVG);
		rule.setPeriod(60);
		rule.setEvaluationPeriods(1);
		rule.setQuery(queries);

		List<String> actions = new ArrayList<String>();
		actions.add("log://");

		// alarm name must be unique inside the tenant, better suffix with the instance id
		Alarm alarm = tenantClient
				.telemetry()
				.alarms()
				.create(Builders.alarm().name(alarmName + "@" + serverId).description(alarmName + " high")
						.type(Alarm.Type.THRESHOLD).thresholeRule(rule).alarmActions(actions).isEnabled(true).build());

		return alarm.getAlarmId();
	}

	@Override
	public boolean updateAlarm(String alarmId, boolean enabled, float threshold) {
		Alarm alarm = tenantClient.telemetry().alarms().getById(alarmId);
		alarm.isEnabled(enabled);

		ThresholdRule modifiedRule = alarm.getThresholdRule();
		modifiedRule.setThreshold(threshold);
		alarm.setThresholdRule((CeilometerThresholdRule) modifiedRule);

		tenantClient.telemetry().alarms().update(alarm.getAlarmId(), alarm);

		// TODO: verify if this works
		// tenantClient.telemetry().alarms().update(alarm.getAlarmId(),
		// alarm.toBuilder().isEnabled(enabled).thresholeRule((CeilometerThresholdRule) modifiedRule).build());

		return true;
	}

	@Override
	public boolean deleteAlarm(String alarmId) {
		ActionResponse response = tenantClient.telemetry().alarms().delete(alarmId);
		if (false == response.isSuccess()) {
			logger.error(response.getFault());
			return false;
		}

		return true;
	}

	@Override
	public String getAlarmState(String alarmId) {
		Alarm alarm = tenantClient.telemetry().alarms().getById(alarmId);
		return alarm.getState();
	}

	private String processOpenstackTime(String dateTime) {
		// TODO: what does the function do, and how to improve that?

		int hour = Integer.parseInt(dateTime.split("T")[1].split(":")[0]);
		int min = Integer.parseInt(dateTime.split("T")[1].split(":")[1]);
		int sec = Integer.parseInt(dateTime.split("T")[1].split(":")[2].substring(0, 2));
		int day = Integer.parseInt(dateTime.split("T")[0].split("-")[2]);
		int month = Integer.parseInt(dateTime.split("T")[0].split("-")[1]);
		int year = Integer.parseInt(dateTime.split("T")[0].split("-")[0]);

		if (hour >= 16) {
			hour = hour - 16;
			day = day + 1;
		} else {
			hour = hour + 8;
		}

		if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10) {
			if (day == 32) {
				month++;
				day = 1;
			}
		} else if (month == 4 || month == 6 || month == 9 || month == 11) {
			if (day == 31) {
				month++;
				day = 1;
			}
		} else if (month == 12 && day == 32) {
			year++;
			day = 1;
			month = 1;
		} else if (month == 2) {
			if (year % 4 == 0) {
				if (day == 30) {
					month++;
					day = 1;
				}
			} else {
				if (day == 29) {
					month++;
					day = 1;
				}
			}
		}

		return year + "-" + (month < 10 ? ("0" + month) : month) + "-" + (day < 10 ? ("0" + day) : day) + " "
				+ (hour < 10 ? ("0" + hour) : hour) + ":" + (min < 10 ? ("0" + min) : min) + ":"
				+ (sec < 10 ? ("0" + sec) : sec);
	}

	@Override
	public Map<String, Object> getSamples(String serverId, String meterName, long timestamp) {
		String resourceId = getResourceId(serverId, meterName);
		if (null == resourceId) {
			return null;
		}

		List<String> time_series = new ArrayList<String>();
		List<Float> samples = new ArrayList<Float>();
		Map<String, Object> map = new HashMap<String, Object>();
		SampleCriteria criteria = new SampleCriteria().resource(resourceId)
				.timestamp(SampleCriteria.Oper.GT, timestamp);
		List<? extends MeterSample> meterSamples = tenantClient.telemetry().meters().samples(meterName, criteria);
		for (MeterSample sample : meterSamples) {
			time_series.add(processOpenstackTime(sample.getRecordedAt()));
			samples.add(sample.getCounterVolume());
		}

		map.put("time_series", time_series);
		map.put("samples", samples);

		return map;
	}

	@Override
	public List<String> getFloatingIpRange() {
		List<String> floatingIpRange = new ArrayList<String>();

		// TODO assert only one subnet
		String subnetId = tenantClient.networking().network().get(PUBLIC_NETWORK_ID).getSubnets().get(0);
		Subnet subnet = tenantClient.networking().subnet().get(subnetId);
		String cidr = subnet.getCidr();

		// prefix == 24
		int prefix = Integer.parseInt(cidr.substring(cidr.indexOf('/') + 1));
		if (24 != prefix) {
			logger.error("不支持非24的浮动IP掩码");
			return floatingIpRange;
		}

		List<? extends Pool> pools = subnet.getAllocationPools();
		// TODO assert all allocation pools have the same prefix
		String ipSegment = pools.get(0).getStart().substring(0, pools.get(0).getStart().lastIndexOf('.'));
		for (Pool p : pools) {
			int start = Integer.parseInt(p.getStart().substring(p.getStart().lastIndexOf('.') + 1));
			int end = Integer.parseInt(p.getEnd().substring(p.getEnd().lastIndexOf('.') + 1));
			for (int i = start; i <= end; i++) {
				floatingIpRange.add(ipSegment + "." + i);
			}
		}

		return floatingIpRange;
	}

	@Override
	public List<? extends Port> getGateways() {
		List<? extends Port> gateways = tenantClient.networking().port()
				.list(PortListOptions.create().networkId(PUBLIC_NETWORK_ID).deviceOwner("network:router_gateway"));
		return gateways;
	}

	@Override
	public List<? extends NetFloatingIP> getFloatingIpList() {
		List<? extends NetFloatingIP> floatingIPs = tenantClient.networking().floatingip().list();
		return floatingIPs;
	}

	@Override
	public List<? extends FloatingIP> getProjectFloatingIpList() {
		List<? extends FloatingIP> floatingIps = tenantClient.compute().floatingIps().list();
		return floatingIps;
	}

	@Override
	public List<String> getAvailableFloatingIp() {
		List<String> availableFloatingIp = new ArrayList<String>();

		// allocate all ips from pool
		String pool = tenantClient.compute().floatingIps().getPoolNames().get(0);
		while (true) {
			try {
				tenantClient.compute().floatingIps().allocateIP(pool);
			} catch (Exception e) {
				break;
			}
		}

		List<? extends FloatingIP> floatingIps = tenantClient.compute().floatingIps().list();
		for (FloatingIP floatingIp : floatingIps) {
			if (floatingIp.getInstanceId() == null) {
				availableFloatingIp.add(floatingIp.getFloatingIpAddress());

				// deallocate ip back to pool
				ActionResponse response = tenantClient.compute().floatingIps().deallocateIP(floatingIp.getId());
				if (false == response.isSuccess()) {
					logger.error(floatingIp.getFloatingIpAddress() + "未释放。" + response.getFault());
				}
			}
		}

		return availableFloatingIp;
	}
}
