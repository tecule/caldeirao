package com.sinosoft.openstack;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Flavor;
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
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.User;
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
import org.openstack4j.model.storage.block.BlockLimits.Absolute;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.telemetry.MeterSample;
import org.openstack4j.model.telemetry.SampleCriteria;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sinosoft.openstack.exception.CloudException;
import com.sinosoft.openstack.type.ActionResult;
import com.sinosoft.openstack.type.CloudConfig;
import com.sinosoft.openstack.type.ServerInfo;
import com.sinosoft.openstack.type.telemetry.Alarm;
import com.sinosoft.openstack.type.telemetry.Query;
import com.sinosoft.openstack.type.telemetry.ServerSamples;
import com.sinosoft.openstack.type.telemetry.ThresholdRule;

public class CloudManipulatorV3 implements CloudManipulator {
	private static Logger logger = LoggerFactory.getLogger(CloudManipulatorV3.class);

	// appConfig cannot be @Autowired, since it's been used in the constructor, and @Autowired is done after
	// construction

	private String OS_AUTH_URL;
	private String OS_USER_DOMAIN_NAME;
	private String OS_USER_DOMAIN_ID;
	private String OS_USERNAME;
	private String OS_USER_ID;
	private String OS_PASSWORD;
	// private String OS_PROJECT_ID;
	private String OS_ROLE_NAME;
	private String AODH_SERVICE_URL;
	private String PUBLIC_NETWORK_ID;

	private String projectId;
	private OSClientV3 projectClient;

	/**
	 * class constructor. create connection to admin project.
	 * 
	 * @param appConfig
	 * @author xiangqian
	 */
	// public CloudManipulatorV3(CloudConfig appConfig) {
	// OS_AUTH_URL = appConfig.getAuthUrl();
	// OS_USER_DOMAIN_NAME = appConfig.getDomainName();
	// OS_USER_DOMAIN_ID = appConfig.getDomainId();
	// OS_USERNAME = appConfig.getAdminUsername();
	// OS_USER_ID = appConfig.getAdminUserId();
	// OS_PASSWORD = appConfig.getAdminPassword();
	// OS_PROJECT_ID = appConfig.getAdminProjectId();
	// OS_ROLE_NAME = appConfig.getAdminRoleName();
	// AODH_SERVICE_URL = appConfig.getAodhServiceUrl();
	// PUBLIC_NETWORK_ID = appConfig.getPublicNetworkId();
	//
	// this.projectId = OS_PROJECT_ID;
	// try {
	// projectClient = OSFactory.builderV3().endpoint(OS_AUTH_URL)
	// .credentials(OS_USERNAME, OS_PASSWORD, Identifier.byName(OS_USER_DOMAIN_NAME))
	// .scopeToProject(Identifier.byId(OS_PROJECT_ID)).authenticate();
	// } catch (AuthenticationException e) {
	// throw new CloudException("云服务认证发生错误。", e);
	// } catch (Exception e) {
	// throw new CloudException("创建云服务连接发生错误。", e);
	// }
	// }

	public CloudManipulatorV3(CloudConfig appConfig, String projectId) {
		OS_AUTH_URL = appConfig.getAuthUrl();
		OS_USER_DOMAIN_NAME = appConfig.getDomainName();
		OS_USER_DOMAIN_ID = appConfig.getDomainId();
		OS_USERNAME = appConfig.getAdminUsername();
		OS_USER_ID = appConfig.getAdminUserId();
		OS_PASSWORD = appConfig.getAdminPassword();
		// OS_PROJECT_ID = appConfig.getAdminProjectId();
		OS_ROLE_NAME = appConfig.getAdminRoleName();
		AODH_SERVICE_URL = appConfig.getAodhServiceUrl();
		PUBLIC_NETWORK_ID = appConfig.getPublicNetworkId();

		this.projectId = projectId;
		try {
			projectClient = OSFactory.builderV3().endpoint(OS_AUTH_URL)
					.credentials(OS_USERNAME, OS_PASSWORD, Identifier.byName(OS_USER_DOMAIN_NAME))
					.scopeToProject(Identifier.byId(projectId)).authenticate();
		} catch (AuthenticationException e) {
			throw new CloudException("云服务认证发生错误。", e);
		} catch (Exception e) {
			throw new CloudException("创建云服务连接发生错误。", e);
		}
	}

	@Override
	public String createProject(String projectName, String projectDescription, int instanceQuota, int cpuQuota,
			int memoryQuota) {
		String projectId = "";

		try {
			OSClientV3 domainClient = OSFactory.builderV3().endpoint(OS_AUTH_URL).credentials(OS_USER_ID, OS_PASSWORD)
					.scopeToDomain(Identifier.byId(OS_USER_DOMAIN_ID)).authenticate();

			// create tenant
			Project project = domainClient
					.identity()
					.projects()
					.create(Builders.project().name(projectName + "_" + UUID.randomUUID().toString())
							.description(projectDescription).build());
			projectId = project.getId();

			// rename
			domainClient.identity().projects()
					.update(project.toBuilder().name(projectName + "_" + projectId.substring(0, 8)).build());

			// set user permission
			List<? extends User> users = domainClient.identity().users().getByName(OS_USERNAME);
			if (users.size() <= 0) {
				throw new CloudException("获取用户发生错误。");
			}
			User adminUser = users.get(0);
			List<? extends Role> roles = domainClient.identity().roles().getByName(OS_ROLE_NAME);
			if (roles.size() <= 0) {
				throw new CloudException("获取角色发生错误。");
			}
			Role adminRole = roles.get(0);
			ActionResponse response = domainClient.identity().roles()
					.grantProjectUserRole(projectId, adminUser.getId(), adminRole.getId());
			if (false == response.isSuccess()) {
				logger.error("设置角色发生错误。" + response.getFault());
				throw new CloudException("设置角色发生错误。");
			}

			// use project client for following operation
			OSClientV3 newProjectClient = OSFactory.builderV3().endpoint(OS_AUTH_URL)
					.credentials(OS_USERNAME, OS_PASSWORD, Identifier.byName(OS_USER_DOMAIN_NAME))
					.scopeToProject(Identifier.byId(projectId)).authenticate();

			// set quota
			newProjectClient
					.compute()
					.quotaSets()
					.updateForTenant(
							projectId,
							Builders.quotaSet().instances(instanceQuota).cores(cpuQuota).ram(memoryQuota * 1024)
									.build());

			// build network and router
			Network network = newProjectClient
					.networking()
					.network()
					.create(Builders.network().name("private" + "_" + projectId.substring(0, 8)).tenantId(projectId)
							.adminStateUp(true).build());
			// .addDNSNameServer("114.114.114.114")
			Subnet subnet = newProjectClient
					.networking()
					.subnet()
					.create(Builders.subnet().name("private_subnet" + "_" + projectId.substring(0, 8))
							.networkId(network.getId()).tenantId(projectId).ipVersion(IPVersionType.V4)
							.cidr("192.168.32.0/24").gateway("192.168.32.1").enableDHCP(true).build());
			Router router = newProjectClient
					.networking()
					.router()
					.create(Builders.router().name("router" + "_" + projectId.substring(0, 8)).adminStateUp(true)
							.externalGateway(PUBLIC_NETWORK_ID).tenantId(projectId).build());
			@SuppressWarnings("unused")
			RouterInterface iface = newProjectClient.networking().router()
					.attachInterface(router.getId(), AttachInterfaceType.SUBNET, subnet.getId());

			// add security group rule
			List<? extends SecGroupExtension> secGroups = newProjectClient.compute().securityGroups().list();
			for (SecGroupExtension secGroup : secGroups) {
				newProjectClient
						.compute()
						.securityGroups()
						.createRule(
								Builders.secGroupRule().cidr("0.0.0.0/0").parentGroupId(secGroup.getId())
										.protocol(IPProtocol.ICMP).range(-1, -1).build());
				newProjectClient
						.compute()
						.securityGroups()
						.createRule(
								Builders.secGroupRule().cidr("0.0.0.0/0").parentGroupId(secGroup.getId())
										.protocol(IPProtocol.TCP).range(1, 65535).build());
				newProjectClient
						.compute()
						.securityGroups()
						.createRule(
								Builders.secGroupRule().cidr("0.0.0.0/0").parentGroupId(secGroup.getId())
										.protocol(IPProtocol.UDP).range(1, 65535).build());
			}
		} catch (Exception e) {
			throw new CloudException("创建项目发生错误，项目ID：" + projectId + "。", e);
		}

		return projectId;
	}

	@Override
	public QuotaSet updateComputeServiceQuota(int instanceQuota, int cpuQuota, int memoryQuota) {
		try {
			QuotaSet quota = projectClient
					.compute()
					.quotaSets()
					.updateForTenant(
							projectId,
							Builders.quotaSet().cores(cpuQuota).instances(instanceQuota).ram(memoryQuota * 1024)
									.build());

			return quota;
		} catch (Exception e) {
			throw new CloudException("更新计算服务配额发生错误。", e);
		}
	}

	@Override
	public Absolute getBlockStorageQuotaUsage() {
		try {
			BlockLimits limits = projectClient.blockStorage().getLimits();
			Absolute absolute = limits.getAbsolute();
			return absolute;
		} catch (Exception e) {
			throw new CloudException("获取块存储配额使用量发生错误。", e);
		}
	}

	@Override
	public BlockQuotaSet updateBlockStorageQuota(int volumes, int gigabytes) {
		try {
			BlockQuotaSet quota = projectClient.blockStorage().quotaSets()
					.updateForTenant(projectId, Builders.blockQuotaSet().volumes(volumes).gigabytes(gigabytes).build());
			return quota;
		} catch (Exception e) {
			throw new CloudException("更新块存储配额发生错误。", e);
		}
	}

	@Override
	public ActionResult deleteProject() {
		ActionResult result = new ActionResult();
		boolean success = false;
		String message = "";

		ActionResponse response;

		try {
			// check if this tenant has server
			List<? extends Server> servers = projectClient.compute().servers().list();
			if (servers.size() > 0) {
				success = false;
				message = "删除项目发生错误，当前项目包含虚拟机，不允许删除。";
				logger.error(message);
				result.setSuccess(success);
				result.setMessage(message);
				return result;
			}

			// get internal subnet
			List<Network> tenantNetworks = new ArrayList<Network>();
			List<Subnet> tenantSubnets = new ArrayList<Subnet>();
			List<? extends Network> networks = projectClient.networking().network().list();
			for (Network network : networks) {
				if (network.getTenantId().equalsIgnoreCase(projectId)) {
					tenantNetworks.add(network);
					tenantSubnets.addAll(network.getNeutronSubnets());
				}
			}

			// (1) delete router,
			List<? extends Router> routers = projectClient.networking().router().list();
			for (Router router : routers) {
				if (router.getTenantId().equalsIgnoreCase(projectId)) {
					// detach from internal network
					for (Subnet subnet : tenantSubnets) {
						projectClient.networking().router().detachInterface(router.getId(), subnet.getId(), null);
					}

					// delete "HA subnet tenant xxx" automatically
					response = projectClient.networking().router().delete(router.getId());
					if (response.isSuccess() == false) {
						success = false;
						message = "删除项目发生错误，删除路由器失败。";
						logger.error(message + response.getFault());
						result.setSuccess(success);
						result.setMessage(message);
						return result;
					}
				}
			}

			// (2) delete tenant network
			for (Network network : tenantNetworks) {
				response = projectClient.networking().network().delete(network.getId());
				if (response.isSuccess() == false) {
					success = false;
					message = "删除项目发生错误，删除网络失败。";
					logger.error(message + response.getFault());
					result.setSuccess(success);
					result.setMessage(message);
					return result;
				}
			}

			// (3) delete project
			response = projectClient.identity().projects().delete(projectId);
			if (response.isSuccess() == false) {
				success = false;
				message = "删除项目发生错误，删除项目失败。";
				logger.error(message + response.getFault());
				result.setSuccess(success);
				result.setMessage(message);
				return result;
			}
		} catch (Exception e) {
			success = false;
			message = "删除项目发生错误。";
			logger.error(message + e.getMessage());
			result.setSuccess(success);
			result.setMessage(message);
			return result;
		}

		success = true;
		message = "";
		result.setSuccess(success);
		result.setMessage(message);
		return result;
	}

	@Override
	public List<? extends Volume> getVolumes() {
		try {
			List<? extends Volume> volumes = projectClient.blockStorage().volumes().list();
			return volumes;
		} catch (Exception e) {
			throw new CloudException("获取卷列表发生错误。", e);
		}
	}

	@Override
	public Volume createVolume(String volumeName, String volumeDescription, int volumeSize) {
		try {
			/*
			 * TODO volume name & description with Chinese characters not supported in openstack4j 3.0.2.
			 */
			Volume volume = projectClient.blockStorage().volumes()
					.create(Builders.volume().name(volumeName).description(volumeDescription).size(volumeSize).build());
			return volume;
		} catch (Exception e) {
			String message = "";
			if (e.getMessage().indexOf("VolumeLimitExceeded") >= 0) {
				message = "已经创建的卷达到了系统允许的最大配额，请增加块存储配额，或删除不需要的卷。";
			} else {
				message = "创建卷发生错误。";
			}

			throw new CloudException(message, e);
		}
	}

	@Override
	public boolean modifyVolume(String volumeId, String volumeName, String volumeDescription) {
		try {
			// modification will return true as long as cinder-api is working
			ActionResponse response = projectClient.blockStorage().volumes()
					.update(volumeId, volumeName, volumeDescription);
			if (false == response.isSuccess()) {
				logger.error("修改卷信息失败：" + response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("修改卷信息发生错误。", e);
		}
	}

	@Override
	public Volume getVolume(String volumeId) {
		try {
			Volume volume = projectClient.blockStorage().volumes().get(volumeId);
			return volume;
		} catch (Exception e) {
			throw new CloudException("获取卷发生错误。", e);
		}
	}

	@Override
	public boolean deleteVolume(String volumeId) {
		try {
			ActionResponse response = projectClient.blockStorage().volumes().delete(volumeId);
			if (false == response.isSuccess()) {
				logger.error("删除卷失败：" + response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("删除卷发生错误。", e);
		}
	}

	@Override
	public boolean waitVolumeStatus(String volumeId, List<Volume.Status> statusList, int minute)
			throws InterruptedException {
		int sleepInterval = 6000;
		int sleepCount = minute * 60 * 1000 / sleepInterval;

		int loop = 0;
		while (loop < sleepCount) {
			Volume volume = getVolume(volumeId);
			if (null == volume) {
				return false;
			}

			if (volume.getStatus() != null) {
				for (Volume.Status status : statusList) {
					if (volume.getStatus() == status) {
						return true;
					}
				}
			}

			Thread.sleep(sleepInterval);
			loop++;
		}

		return false;
	}

	@Override
	public boolean waitVolumeDeleted(String volumeId, int minute) throws InterruptedException {
		int sleepInterval = 6000;
		int sleepCount = minute * 60 * 1000 / sleepInterval;

		int loop = 0;
		while (loop < sleepCount) {
			Volume volume = getVolume(volumeId);
			if (null == volume) {
				return true;
			}

			Thread.sleep(sleepInterval);
			loop++;
		}

		return false;
	}

	@Override
	public List<? extends Image> getImages() {
		try {
			List<? extends Image> images = projectClient.images().list();
			return images;
		} catch (Exception e) {
			throw new CloudException("获取镜像列表发生错误。", e);
		}
	}

	@Override
	public Image getImage(String imageId) {
		try {
			Image image = projectClient.images().get(imageId);
			return image;
		} catch (Exception e) {
			throw new CloudException("获取镜像发生错误。", e);
		}
	}

	@Override
	public boolean waitImageStatus(String imageId, org.openstack4j.model.image.Image.Status status, int minute)
			throws InterruptedException {
		int sleepInterval = 6000;
		int sleepCount = minute * 60 * 1000 / sleepInterval;

		int loop = 0;
		while (loop < sleepCount) {
			Image image = getImage(imageId);
			if (null == image) {
				return false;
			}

			if (image.getStatus() != null) {
				if (image.getStatus() == status) {
					return true;
				}
			}

			Thread.sleep(sleepInterval);
			loop++;
		}

		return false;
	}

	// @Override
	// public boolean waitImageDeleted(String imageId, int minute) throws InterruptedException {
	// int sleepInterval = 6000;
	// int sleepCount = minute * 60 * 1000 / sleepInterval;
	//
	// int loop = 0;
	// while (loop < sleepCount) {
	// Image image = getImage(imageId);
	// if (null == image) {
	// return true;
	// }
	//
	// Thread.sleep(sleepInterval);
	// loop++;
	// }
	//
	// return false;
	// }

	@Override
	public Image updateImage(String imageId, String imageName, boolean publicity) {
		// TODO image name with chinese character not work in "PUT /v1/images/xxxxxx"

		try {
			Image image = getImage(imageId);

			// Image newImage =
			// projectClient.images().update(image.toBuilder().name(imageName).isPublic(publicity).build());
			Image newImage = projectClient.images().update(image.toBuilder().isPublic(publicity).build());
			return newImage;
		} catch (Exception e) {
			throw new CloudException("更新镜像发生错误。", e);
		}
	}

	@Override
	public boolean deleteImage(String imageId) {
		try {
			ActionResponse response = projectClient.images().delete(imageId);
			if (false == response.isSuccess()) {
				logger.error("删除镜像失败：" + response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("删除镜像发生错误。", e);
		}
	}

	@Override
	public List<? extends Hypervisor> getHypervisors() {
		try {
			List<? extends Hypervisor> hypervisors = projectClient.compute().hypervisors().list();
			return hypervisors;
		} catch (Exception e) {
			throw new CloudException("获取计算节点列表发生错误。", e);
		}
	}

	@Override
	public List<? extends Server> getServers() {
		try {
			List<? extends Server> servers = projectClient.compute().servers().list();
			return servers;
		} catch (Exception e) {
			throw new CloudException("获取虚拟机实例列表发生错误。", e);
		}
	}

	@Override
	public Flavor getFlavor(int cpu, int memory, int disk) {
		try {
			Flavor flavor = null;
			List<? extends Flavor> flavors = projectClient.compute().flavors().list();
			Iterator<? extends Flavor> it = flavors.iterator();
			while (it.hasNext()) {
				Flavor f = (Flavor) it.next();
				if (f.getRam() == memory * 1024 && f.getDisk() == disk && f.getVcpus() == cpu) {
					flavor = f;
					break;
				}
			}

			return flavor;
		} catch (Exception e) {
			throw new CloudException("获取虚拟机配置发生错误。", e);
		}
	}

	@Override
	public Flavor createFlavor(int cpu, int memory, int disk) {
		try {
			Flavor flavor = null;
			String flavorName = "cpu" + cpu + "_mem" + memory + "_disk" + disk;
			// TODO why rxtxFactor != 1.0
			flavor = Builders.flavor().name(flavorName).ram(memory * 1024).vcpus(cpu).disk(disk).rxtxFactor(1.2f)
					.build();
			flavor = projectClient.compute().flavors().create(flavor);

			return flavor;
		} catch (Exception e) {
			throw new CloudException("创建虚拟机配置发生错误。", e);
		}
	}

	@Override
	public Server bootServer(String serverName, String flavorId, String imageId) {
		try {
			Server server = projectClient.compute().servers()
					.boot(Builders.server().name(serverName).flavor(flavorId).image(imageId).build());
			return server;
		} catch (Exception e) {
			throw new CloudException("创建虚拟机实例发生错误。", e);
		}
	}

	@Override
	public boolean waitServerStatus(String serverId, List<Status> statusList, int minute) throws InterruptedException {
		int sleepInterval = 6000;
		int sleepCount = minute * 60 * 1000 / sleepInterval;

		int loop = 0;
		while (loop < sleepCount) {
			Server server = getServer(serverId);
			if (null == server) {
				return false;
			}

			if (server.getStatus() != null) {
				for (Status status : statusList) {
					if ((server.getStatus() == status) && (null == server.getTaskState())) {
						return true;
					}
				}
			}

			Thread.sleep(sleepInterval);
			loop++;
		}

		return false;
	}

	@Override
	public Server getServer(String serverId) {
		try {
			Server server = projectClient.compute().servers().get(serverId);
			return server;
		} catch (Exception e) {
			throw new CloudException("获取虚拟机实例发生错误。", e);
		}
	}

	@Override
	public boolean startServer(String serverId) {
		try {
			ActionResponse response = projectClient.compute().servers().action(serverId, Action.START);
			if (false == response.isSuccess()) {
				logger.error("启动虚拟机失败：" + response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("启动虚拟机发生错误。", e);
		}
	}

	@Override
	public boolean rebootServer(String serverId) {
		try {
			ActionResponse response = projectClient.compute().servers().reboot(serverId, RebootType.SOFT);
			if (false == response.isSuccess()) {
				logger.error("重启虚拟机失败：" + response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("重启虚拟机发生错误。", e);
		}
	}

	@Override
	public boolean stopServer(String serverId) {
		try {
			ActionResponse response = projectClient.compute().servers().action(serverId, Action.STOP);
			if (false == response.isSuccess()) {
				logger.error("关闭虚拟机失败：" + response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("关闭虚拟机发生错误。", e);
		}
	}

	@Override
	public boolean deleteServer(String serverId) {
		try {
			ActionResponse response = projectClient.compute().servers().delete(serverId);
			if (false == response.isSuccess()) {
				logger.error(response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("删除虚拟机发生错误。", e);
		}
	}

	@Override
	public VNCConsole getServerVNCConsole(String serverId) {
		try {
			VNCConsole console = projectClient.compute().servers().getVNCConsole(serverId, Type.NOVNC);
			return console;
		} catch (Exception e) {
			throw new CloudException("获取虚拟机控制台发生错误。", e);
		}
	}

	@Override
	public Server renameServer(String serverId, String newName) {
		try {
			Server server = projectClient.compute().servers()
					.update(serverId, ServerUpdateOptions.create().name(newName));
			return server;
		} catch (Exception e) {
			throw new CloudException("重命名虚拟机发生错误。", e);
		}
	}

	// @Override
	// public ActionResult associateFloatingIp(String serverId, String floatingIpAddress) {
	// ActionResult result = new ActionResult();
	// boolean success = false;
	// String message = "";
	//
	// try {
	// Server server = getServer(serverId);
	// if (null == server) {
	// success = false;
	// message = "绑定地址失败，服务器不存在。";
	// logger.error(message);
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// }
	//
	// List<String> pools = projectClient.compute().floatingIps().getPoolNames();
	// if (pools.size() != 1) {
	// success = false;
	// message = "绑定地址失败，项目地址池数量不为1。";
	// logger.error(message);
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// }
	//
	// // allocate all free ips from pool
	// String pool = pools.get(0);
	// while (true) {
	// try {
	// projectClient.compute().floatingIps().allocateIP(pool);
	// } catch (Exception e) {
	// break;
	// }
	// }
	//
	// /*
	// * associate ip if it's free, deallocate other free ip back to pool, so other project can make use of it.
	// */
	// List<? extends FloatingIP> availableIps = projectClient.compute().floatingIps().list();
	// for (FloatingIP ip : availableIps) {
	// if (ip.getInstanceId() == null) {
	// if (ip.getFloatingIpAddress().equalsIgnoreCase(floatingIpAddress)) {
	// ActionResponse response = projectClient.compute().floatingIps().addFloatingIP(server,
	// floatingIpAddress);
	// if (true == response.isSuccess()) {
	// success = true;
	// message = "";
	//
	// continue;
	// } else {
	// success = false;
	// message = "绑定地址失败。";
	// logger.error(message + response.getFault());
	// }
	// }
	//
	// // deallocate unused ip back to the pool
	// ActionResponse response2 = projectClient.compute().floatingIps().deallocateIP(ip.getId());
	// if (false == response2.isSuccess()) {
	// logger.error("释放地址" + ip.getFloatingIpAddress() + "失败：" + response2.getFault());
	// }
	// }
	// }
	//
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// } catch (Exception e) {
	// success = false;
	// message = "绑定地址发生错误。";
	// logger.error(message + e.getMessage());
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// }
	// }
	//
	// private boolean deallocate2(OSClientV3 projectClient, Server server, String floatingIpAddress) {
	// // disassociate ip from server
	// ActionResponse response = projectClient.compute().floatingIps().removeFloatingIP(server, floatingIpAddress);
	// if (false == response.isSuccess()) {
	// logger.error("解绑地址失败。" + response.getFault());
	// return false;
	// }
	//
	// // deallocate ip back to pool
	// List<? extends FloatingIP> floatingIps = projectClient.compute().floatingIps().list();
	// for (FloatingIP floatingIp : floatingIps) {
	// if (floatingIp.getFloatingIpAddress().equalsIgnoreCase(floatingIpAddress)) {
	// ActionResponse response2 = projectClient.compute().floatingIps().deallocateIP(floatingIp.getId());
	// if (false == response2.isSuccess()) {
	// logger.warn("解绑地址出现警告，释放地址" + floatingIp.getFloatingIpAddress() + "失败。" + response2.getFault());
	// }
	// // if (true == response2.isSuccess()) {
	// // return true;
	// // } else {
	// // logger.error(response2.getFault());
	// // return false;
	// // }
	// }
	// }
	//
	// // return true despite the ip is not deallocated back to pool
	// return true;
	// }
	//
	// @Override
	// public ActionResult deallocateFloatingIp(String serverId, String floatingIpAddress) {
	// ActionResult result = new ActionResult();
	// boolean success = false;
	// String message = "";
	//
	// try {
	// Server server = getServer(serverId);
	// if (null == server) {
	// success = false;
	// message = "解绑地址失败，服务器不存在。";
	// logger.error(message);
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// }
	//
	// Iterator<List<? extends Address>> it = server.getAddresses().getAddresses().values().iterator();
	// while (it.hasNext()) {
	// List<? extends Address> addresses = it.next();
	// for (Address address : addresses) {
	// String addrType = address.getType(), addr = address.getAddr();
	//
	// if (addrType.equalsIgnoreCase("floating") && (addr.equalsIgnoreCase(floatingIpAddress))) {
	// boolean deallocated = deallocate2(projectClient, server, floatingIpAddress);
	// if (true == deallocated) {
	// success = true;
	// message = "";
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// } else {
	// success = false;
	// message = "解绑地址失败。";
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// }
	// }
	// }
	// }
	// } catch (Exception e) {
	// success = false;
	// message = "解绑地址发生错误。";
	// logger.error(message + e.getMessage());
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// }
	//
	// success = false;
	// message = "";
	// result.setSuccess(success);
	// result.setMessage(message);
	// return result;
	// }

	@Override
	public boolean attachVolume(String serverId, String volumeId) {
		try {
			VolumeAttachment attachment = projectClient.compute().servers().attachVolume(serverId, volumeId, null);
			if (null == attachment) {
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("挂载卷发生错误。", e);
		}
	}

	@Override
	public boolean detachVolume(String serverId, String volumeId) {
		try {
			ActionResponse response = projectClient.compute().servers().detachVolume(serverId, volumeId);
			if (false == response.isSuccess()) {
				logger.error("卸载卷失败：" + response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("卸载卷发生错误。", e);
		}
	}

	@Override
	public String createSnapshot(String serverId, String snapshotName) {
		try {
			String snapshotId = projectClient.compute().servers().createSnapshot(serverId, snapshotName);
			return snapshotId;
		} catch (Exception e) {
			throw new CloudException("创建快照发生错误。", e);
		}
	}

	@Override
	public boolean liveMigrate(String serverId, String hypervisorName) {
		try {
			// mitaka use FQDN in live migration
			ActionResponse response = projectClient.compute().servers()
					.liveMigrate(serverId, LiveMigrateOptions.create().host(hypervisorName));
			if (false == response.isSuccess()) {
				logger.error("迁移虚拟机实例失败：" + response.getFault());
				return false;
			}

			return true;
		} catch (Exception e) {
			throw new CloudException("迁移虚拟机实例发生错误。", e);
		}
	}

	@Override
	public ServerInfo getServerInfo(String serverId) {
		Server server = getServer(serverId);
		if (null == server) {
			return null;
		}

		String privateIp = "", floatingIp = "";
		Iterator<List<? extends Address>> it = server.getAddresses().getAddresses().values().iterator();
		while (it.hasNext()) {
			List<? extends Address> addresses = it.next();
			for (int i = 0; i < addresses.size(); i++) {
				/*
				 * use addresses.get(i).getType().equalsIgnoreCase("fixed") instead of addresses.get(i).getType() ==
				 * "fixed"
				 */
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

		ServerInfo info = new ServerInfo();
		info.setPrivateIp(privateIp);
		info.setFloatingIp(floatingIp);
		info.setPhysicalMachine(physicalMachine);
		return info;
	}

	/**
	 * get resource id by server and meter.
	 * 
	 * @param serverId
	 *            - server id
	 * @param meterName
	 *            - meter name
	 * @return resource id, return null if resource not found
	 * @author xiangqian
	 */
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
			Server server = getServer(serverId);
			if (null == server) {
				logger.error("虚拟机实例不存在");
				return null;
			}

			// get network port resource id
			List<? extends Port> ports = projectClient.networking().port()
					.list(PortListOptions.create().deviceId(serverId));

			// TODO: assume ports length > 0
			if (ports.size() <= 0) {
				logger.error("虚拟机实例缺少网络端口");
				return null;
			}

			String networkResourceId = server.getInstanceName() + "-" + serverId + "-tap" + ports.get(0).getId();
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
			throw new CloudException("创建虚拟机告警发生错误，无效的资源类型。");
		}

		Alarm alarm = new Alarm();
		// alarm name must be unique inside the tenant, better suffix with the instance id
		alarm.setName(alarmName + "@" + serverId);
		alarm.setDescription(alarmName + " high");
		alarm.setEnabled(true);
		alarm.setType("threshold");
		alarm.setSeverity("low");
		alarm.setState("insufficient data");

		ThresholdRule rule = new ThresholdRule();
		rule.setMeterName(meterName);
		rule.setComparisonOperator("ge");
		rule.setThreshold(threshold);
		rule.setStatistic("avg");
		rule.setPeriod(60);
		rule.setEvaluationPeriods(1);

		Query query = new Query();
		query.setField("resource_id");
		query.setOp("eq");
		query.setValue(resourceId);
		List<Query> queries = new ArrayList<Query>();
		queries.add(query);
		rule.setQuery(queries);
		alarm.setThresholdRule(rule);

		List<String> actions = new ArrayList<String>();
		actions.add("log://");
		alarm.setAlarmActions(actions);

		try {
			// use resteasy client if reasteasy-xxx-2.x is used
			ClientRequest request = new ClientRequest(AODH_SERVICE_URL);
			request.header("X-Auth-Token", projectClient.getToken().getId());

			/*
			 * post alarm object raise RuntimeException, post the JSON string is ok: java.lang.RuntimeException: could
			 * not find writer for content-type application/json type
			 */
			// request.body(MediaType.APPLICATION_JSON, alarm);
			ObjectMapper mapper = new ObjectMapper();
			String alarmJson = mapper.writeValueAsString(alarm);
			request.body(MediaType.APPLICATION_JSON, alarmJson);

			ClientResponse<String> response = request.post(String.class);
			int responseCode = response.getResponseStatus().getStatusCode();
			String responseContent = response.getEntity();
			logger.trace("responseCode: " + responseCode + ", responseContent：" + responseContent);
			if (201 == responseCode) {
				Alarm createdAlarm = mapper.readValue(responseContent, Alarm.class);
				return createdAlarm.getAlarmId();
			} else {
				String message = "创建虚拟机告警失败，虚拟机ID：" + serverId + ", 监控指标：" + meterName + "。返回代码: " + responseCode;
				throw new CloudException(message);
			}
		} catch (Exception e) {
			throw new CloudException("创建虚拟机告警发生错误。", e);
		}
	}

	/**
	 * get alarm by id.
	 * 
	 * @param alarmId
	 *            - alarm id
	 * @return alarm with the given id, or null if not found
	 * @author xiangqian
	 */
	private Alarm getAlarm(String alarmId) {
		try {
			ClientRequest request = new ClientRequest(AODH_SERVICE_URL + "/" + alarmId);
			request.header("X-Auth-Token", projectClient.getToken().getId());
			ClientResponse<String> response = request.get(String.class);

			int responseCode = response.getResponseStatus().getStatusCode();
			String responseContent = response.getEntity();
			logger.trace("responseCode: " + responseCode + ", responseContent：" + responseContent);
			if (200 == responseCode) {
				ObjectMapper mapper = new ObjectMapper();
				Alarm alarm = mapper.readValue(responseContent, Alarm.class);
				return alarm;
			} else {
				logger.error("获取虚拟机告警失败，告警ID：" + alarmId + "。response code: " + responseCode + ", responseContent："
						+ responseContent);
				return null;
			}
		} catch (Exception e) {
			throw new CloudException("获取虚拟机告警发生错误。", e);
		}
	}

	@Override
	public boolean updateAlarm(String alarmId, boolean enabled, float threshold) {
		try {
			Alarm alarm = getAlarm(alarmId);
			if (null == alarm) {
				String message = "更新虚拟机告警失败，告警不存在，告警ID：" + alarmId;
				logger.error(message);
				return false;
			}

			alarm.setEnabled(enabled);

			ThresholdRule rule = alarm.getThresholdRule();
			rule.setThreshold(threshold);

			alarm.setThresholdRule(rule);

			ClientRequest request = new ClientRequest(AODH_SERVICE_URL + "/" + alarmId);
			request.header("X-Auth-Token", projectClient.getToken().getId());

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

			String alarmJson = mapper.writeValueAsString(alarm);
			request.body(MediaType.APPLICATION_JSON, alarmJson);
			ClientResponse<String> response = request.put(String.class);

			int responseCode = response.getResponseStatus().getStatusCode();
			String responseContent = response.getEntity();
			logger.trace("返回代码: " + responseCode + ", 返回内容：" + responseContent);
			if (200 == responseCode) {
				return true;
			} else {
				String message = "更新虚拟机告警失败，告警ID：" + alarmId + "。返回代码: " + responseCode + ", 返回内容：" + responseContent;
				logger.error(message);
				return false;
			}
		} catch (Exception e) {
			throw new CloudException("更新虚拟机告警发生错误。", e);
		}
	}

	@Override
	public boolean deleteAlarm(String alarmId) {
		try {
			ClientRequest request = new ClientRequest(AODH_SERVICE_URL + "/" + alarmId);
			request.header("X-Auth-Token", projectClient.getToken().getId());
			ClientResponse<String> response = request.delete(String.class);

			int responseCode = response.getResponseStatus().getStatusCode();
			String responseContent = response.getEntity();
			logger.trace("responseCode: " + responseCode + ", responseContent：" + responseContent);
			if (204 == responseCode) {
				return true;
			} else {
				String message = "删除虚拟机告警失败，告警ID：" + alarmId + "。返回代码: " + responseCode + ", 返回内容：" + responseContent;
				logger.error(message);
				return false;
			}
		} catch (Exception e) {
			throw new CloudException("删除虚拟机告警发生错误。" + e.getMessage(), e);
		}
	}

	@Override
	public String getAlarmState(String alarmId) {
		Alarm alarm = getAlarm(alarmId);
		if (null != alarm) {
			return alarm.getState();
		}

		return null;
	}

	private String convertUtcToLocal(String sampleTimestamp) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date localDate = formatter.parse(sampleTimestamp);
		formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String localFormat = formatter.format(localDate);

		return localFormat;

		// int hour = Integer.parseInt(dateTime.split("T")[1].split(":")[0]);
		// int min = Integer.parseInt(dateTime.split("T")[1].split(":")[1]);
		// int sec = Integer.parseInt(dateTime.split("T")[1].split(":")[2].substring(0, 2));
		// int day = Integer.parseInt(dateTime.split("T")[0].split("-")[2]);
		// int month = Integer.parseInt(dateTime.split("T")[0].split("-")[1]);
		// int year = Integer.parseInt(dateTime.split("T")[0].split("-")[0]);
		//
		// if (hour >= 16) {
		// hour = hour - 16;
		// day = day + 1;
		// } else {
		// hour = hour + 8;
		// }
		//
		// if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10) {
		// if (day == 32) {
		// month++;
		// day = 1;
		// }
		// } else if (month == 4 || month == 6 || month == 9 || month == 11) {
		// if (day == 31) {
		// month++;
		// day = 1;
		// }
		// } else if (month == 12 && day == 32) {
		// year++;
		// day = 1;
		// month = 1;
		// } else if (month == 2) {
		// if (year % 4 == 0) {
		// if (day == 30) {
		// month++;
		// day = 1;
		// }
		// } else {
		// if (day == 29) {
		// month++;
		// day = 1;
		// }
		// }
		// }
		//
		// return year + "-" + (month < 10 ? ("0" + month) : month) + "-" + (day < 10 ? ("0" + day) : day) + " "
		// + (hour < 10 ? ("0" + hour) : hour) + ":" + (min < 10 ? ("0" + min) : min) + ":"
		// + (sec < 10 ? ("0" + sec) : sec);
	}

	@Override
	public ServerSamples getSamples(String serverId, String meterName, long timestamp) {
		ServerSamples serverSamples = new ServerSamples();
		serverSamples.setMeterName(meterName);

		String resourceId = getResourceId(serverId, meterName);
		if (null == resourceId) {
			return serverSamples;
		}

		List<String> timeSeries = new ArrayList<String>();
		List<Float> samples = new ArrayList<Float>();
		try {
			SampleCriteria criteria = new SampleCriteria().resource(resourceId).timestamp(SampleCriteria.Oper.GT,
					timestamp);
			List<? extends MeterSample> meterSamples = projectClient.telemetry().meters().samples(meterName, criteria);
			/*
			 * invert order
			 */
			for (int index = meterSamples.size() - 1; index >= 0; index--) {
				MeterSample sample = meterSamples.get(index);
				timeSeries.add(convertUtcToLocal(sample.getTimestamp()));
				samples.add(sample.getCounterVolume());
			}
//			for (MeterSample sample : meterSamples) {
//				timeSeries.add(convertUtcToLocal(sample.getTimestamp()));
//				samples.add(sample.getCounterVolume());
//			}			

			serverSamples.setTimeSeries(timeSeries);
			serverSamples.setSamples(samples);
			return serverSamples;
		} catch (Exception e) {
			throw new CloudException("获取虚拟机负载发生错误。", e);
		}
	}

	@Override
	public List<String> getExternalIps() {
		try {
			List<String> floatingIpRange = new ArrayList<String>();

			Network publicNetwork = projectClient.networking().network().get(PUBLIC_NETWORK_ID);
			if (null == publicNetwork) {
				logger.error("获取浮动IP地址范围出错，外部网络不存在。");
				return floatingIpRange;
			}

			List<String> subnetIds = publicNetwork.getSubnets();
			if (1 != subnetIds.size()) {
				logger.error("获取浮动IP地址范围出错，外部网络所属的子网数不为1。");
				return floatingIpRange;
			}

			String subnetId = subnetIds.get(0);
			Subnet subnet = projectClient.networking().subnet().get(subnetId);
			if (null == subnet) {
				logger.error("获取浮动IP地址范围出错，外部网络所属的子网不存在。");
				return floatingIpRange;
			}

			// // prefix == 24
			// String cidr = subnet.getCidr();
			// int prefix = Integer.parseInt(cidr.substring(cidr.indexOf('/') + 1));
			// if (24 != prefix) {
			// logger.error("不支持非24的浮动IP掩码");
			// return floatingIpRange;
			// }

			List<? extends Pool> pools = subnet.getAllocationPools();
			if (0 == pools.size()) {
				logger.error("获取浮动IP地址范围出错，外部网络所属的子网没有指定地址池。");
				return floatingIpRange;
			}

			for (Pool pool : pools) {
				/*
				 * each allocation pool must be defined in one C class net.
				 */
				String startIpAddress = pool.getStart();
				String endIpAddress = pool.getEnd();

				// int CClassIpLength = 24;
				int lastDotPosition = startIpAddress.lastIndexOf('.');
				String startIpAddressCPrefix = startIpAddress.substring(0, lastDotPosition);

				lastDotPosition = endIpAddress.lastIndexOf('.');
				String endIpAddressCPrefix = endIpAddress.substring(0, lastDotPosition);

				// String startIpAddressCPrefix = startIpAddress.substring(0, CClassIpLength);
				// String endIpAddressCPrefix = endIpAddress.substring(0, CClassIpLength);
				if (false == startIpAddressCPrefix.equalsIgnoreCase(endIpAddressCPrefix)) {
					logger.error("获取浮动IP地址范围出错，外部网络所属的子网地址池不是C类网段，该子网被忽略。");

					continue;
				}

				// prefix of the start and end is the same, use any one.
				String addressPrefix = startIpAddressCPrefix;
				int start = Integer.parseInt((startIpAddress.substring(lastDotPosition + 1)));
				int end = Integer.parseInt((endIpAddress.substring(lastDotPosition + 1)));
				for (int i = start; i <= end; i++) {
					floatingIpRange.add(addressPrefix + "." + i);
				}
			}

			// // TODO assert all allocation pools have the same prefix
			// String ipSegment = pools.get(0).getStart().substring(0, pools.get(0).getStart().lastIndexOf('.'));
			// for (Pool p : pools) {
			// int start = Integer.parseInt(p.getStart().substring(p.getStart().lastIndexOf('.') + 1));
			// int end = Integer.parseInt(p.getEnd().substring(p.getEnd().lastIndexOf('.') + 1));
			// for (int i = start; i <= end; i++) {
			// floatingIpRange.add(ipSegment + "." + i);
			// }
			// }

			return floatingIpRange;
		} catch (NumberFormatException e) {
			throw new CloudException("获取外网地址空间发生错误。", e);
		} catch (Exception e) {
			throw new CloudException("获取外网地址空间发生错误。", e);
		}
	}

	@Override
	public List<? extends Port> getGatewayPorts() {
		try {
			List<? extends Port> gatewayPorts = projectClient.networking().port()
					.list(PortListOptions.create().networkId(PUBLIC_NETWORK_ID).deviceOwner("network:router_gateway"));
			return gatewayPorts;
		} catch (Exception e) {
			throw new CloudException("获取路由网关端口发生错误。", e);
		}
	}

	@Override
	public List<? extends Port> getFloatingIpPorts() {
		try {
			List<? extends Port> floatingIpPorts = projectClient.networking().port()
					.list(PortListOptions.create().networkId(PUBLIC_NETWORK_ID).deviceOwner("network:floatingip"));
			return floatingIpPorts;
		} catch (Exception e) {
			throw new CloudException("获取浮动IP端口发生错误。", e);
		}
	}

	@Override
	public List<? extends NetFloatingIP> getFloatingIps() {
		try {
			List<? extends NetFloatingIP> floatingIPs = projectClient.networking().floatingip().list();
			return floatingIPs;
		} catch (Exception e) {
			throw new CloudException("获取浮动IP列表发生错误。", e);
		}
	}

	@Override
	public Port getPort(String portId) {
		try {
			Port port = projectClient.networking().port().get(portId);
			return port;
		} catch (Exception e) {
			throw new CloudException("获取端口发生错误。", e);
		}
	}

	@Override
	public Router getRouter(String routerId) {
		try {
			Router router = projectClient.networking().router().get(routerId);
			return router;
		} catch (Exception e) {
			throw new CloudException("获取路由发生错误。", e);
		}
	}

	@Override
	public ActionResult createFloatingIp(String ipAddress, String serverId) {
		ActionResult result = new ActionResult();
		boolean success = false;
		String message = "";

		NetFloatingIP floatingIp;
		try {
			Server server = getServer(serverId);
			if (null == server) {
				success = false;
				message = "绑定地址失败，服务器不存在。";
				logger.error(message);
				result.setSuccess(success);
				result.setMessage(message);
				return result;
			}

			/*
			 * not possible to create and then associate using openstack4j, so create it with the port id.
			 */
			NeutronFloatingIP fip = new NeutronFloatingIP();
			fip.setFloatingIpAddress(ipAddress);
			fip.setTenantId(server.getTenantId());
			fip.setFloatingNetworkId(PUBLIC_NETWORK_ID);

			List<? extends Port> ports = projectClient.networking().port()
					.list(PortListOptions.create().deviceId(serverId));
			if (1 != ports.size()) {
				success = false;
				message = "绑定地址失败，服务器端口数不为1。";
				logger.error(message);
				result.setSuccess(success);
				result.setMessage(message);
				return result;
			}

			Port port = ports.get(0);
			String portId = port.getId();
			fip.setPortId(portId);
			floatingIp = projectClient.networking().floatingip().create(fip);

			success = true;
			message = floatingIp.getId();
			result.setSuccess(success);
			result.setMessage(message);
			return result;
		} catch (Exception e) {
			throw new CloudException("创建地址发生错误。", e);
		}
	}

	@Override
	public ActionResult deleteFloatingIp(String ipAddress) {
		ActionResult result = new ActionResult();
		boolean success = false;
		String message = "";

		try {
			List<? extends NetFloatingIP> floatingIps = getFloatingIps();

			for (NetFloatingIP floatingIp : floatingIps) {
				String floatingIpAddress = floatingIp.getFloatingIpAddress();
				if (true == ipAddress.equalsIgnoreCase(floatingIpAddress)) {
					ActionResponse response = projectClient.networking().floatingip().delete(floatingIp.getId());
					if (true == response.isSuccess()) {
						success = true;
						message = "";
					} else {
						success = false;
						message = response.getFault();
					}

					break;
				}
			}
		} catch (Exception e) {
			throw new CloudException("删除地址发生错误。", e);
		}

		result.setSuccess(success);
		result.setMessage(message);
		return result;
	}
}
