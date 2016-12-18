package com.sinosoft.openstack;

import java.util.List;

import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.QuotaSet;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.VNCConsole;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.storage.block.BlockLimits.Absolute;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.Volume;

import com.sinosoft.openstack.type.ActionResult;
import com.sinosoft.openstack.type.ServerInfo;
import com.sinosoft.openstack.type.telemetry.ServerSamples;

public interface CloudManipulator {
	/**
	 * create project
	 * 
	 * @param projectName
	 *            - project name
	 * @param projectDescription
	 *            - project description
	 * @param instanceQuota
	 *            - instance quota
	 * @param cpuQuota
	 *            - cpu quota
	 * @param memoryQuota
	 *            - memory quota
	 * @return project id
	 * @author xiangqian
	 */
	public String createProject(String projectName, String projectDescription, int instanceQuota, int cpuQuota,
			int memoryQuota);

	/**
	 * update compute service quota of a project.
	 * 
	 * @param instanceQuota
	 *            - instance quota
	 * @param cpuQuota
	 *            - cpu quota
	 * @param memoryQuota
	 *            - memory quota
	 * @return updated quota
	 * @author xiangqian
	 */
	public QuotaSet updateComputeServiceQuota(int instanceQuota, int cpuQuota, int memoryQuota);

	/**
	 * get absolute limits used by a project.
	 * 
	 * @return the absolute limits
	 * @author xiangqian
	 */
	public Absolute getBlockStorageQuotaUsage();

	/**
	 * update block storage quota of a project.
	 * 
	 * @param volumes
	 *            - volume count
	 * @param gigabytes
	 *            - volume storage capacity
	 * @return updated quota
	 * @author xiangqian
	 */
	public BlockQuotaSet updateBlockStorageQuota(int volumes, int gigabytes);

	/**
	 * delete project
	 * 
	 * @return action result
	 * @author xiangqian
	 */
	public ActionResult deleteProject();

	/**
	 * get volume list of a project.
	 * 
	 * @return volume list
	 * @author xiangqian
	 */
	public List<? extends Volume> getVolumes();

	/**
	 * create volume.
	 * 
	 * @param volumeName
	 *            - volume name
	 * @param volumeDescription
	 *            - volume description
	 * @param volumeSize
	 *            - volume size
	 * @return
	 */
	public Volume createVolume(String volumeName, String volumeDescription, int volumeSize);

	/**
	 * modify volume.
	 * 
	 * @param volumeId
	 *            - volume id
	 * @param volumeName
	 *            - new name
	 * @param volumeDescription
	 *            - new description
	 * @return true if modify request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean modifyVolume(String volumeId, String volumeName, String volumeDescription);

	/**
	 * get volume by id.
	 * 
	 * @param volumeId
	 *            - volume id
	 * @return volume with the given id, or null if not found
	 * @author xiangqian
	 */
	public Volume getVolume(String volumeId);

	/**
	 * delete volume.
	 * 
	 * @param volumeId
	 *            - volume id
	 * @return true if delete request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean deleteVolume(String volumeId);

	/**
	 * wait volume until its status transfer to the wait status in the given time.
	 * 
	 * @param volumeId
	 *            - volume id
	 * @param statusList
	 *            - list of wait status
	 * @param minute
	 *            - wait time limit
	 * @return true if volume status transfer to wait status during the given time, return false if otherwise
	 * @throws InterruptedException
	 */
	public boolean waitVolumeStatus(String volumeId, List<org.openstack4j.model.storage.block.Volume.Status> statusList,
			int minute) throws InterruptedException;

	/**
	 * wait volume until it's been deleted in the given time.
	 * 
	 * @param volumeId
	 *            - volume id
	 * @param minute
	 *            - wait time limit
	 * @return true if volume is deleted during the given time, return false if otherwise
	 * @throws InterruptedException
	 * @author xiangqian
	 */
	public boolean waitVolumeDeleted(String volumeId, int minute) throws InterruptedException;

	/**
	 * get the public images in the cloud.
	 * 
	 * @return the image list
	 * @author xiangqian
	 */
	public List<? extends Image> getImages();

	/**
	 * get image.
	 * 
	 * @param imageId
	 *            - image id
	 * @return image with the given id, or null if not found
	 * @author xiangqian
	 */
	public Image getImage(String imageId);

	/**
	 * wait image until its status transfer to the wait status in the given time.
	 * 
	 * @param imageId
	 *            - image id
	 * @param status
	 *            - wait status
	 * @param minute
	 *            - wait time limit
	 * @return true if image status transfer to wait status during the given time, return false if otherwise
	 * @throws InterruptedException
	 * @author xiangqian
	 */
	public boolean waitImageStatus(String imageId, org.openstack4j.model.image.Image.Status status, int minute)
			throws InterruptedException;

	// /**
	// * wait image until it's been deleted in the given time.
	// *
	// * @param imageId
	// * - image id
	// * @param minute
	// * - wait time limit
	// * @return true if snapshot is deleted during the given time, return false if otherwise
	// * @throws InterruptedException
	// * @author xiangqian
	// */
	// public boolean waitImageDeleted(String imageId, int minute) throws InterruptedException;

	/**
	 * update image.
	 * 
	 * @param imageId
	 *            - image id
	 * @param imageName
	 *            - image name
	 * @param publicity
	 *            - publicity
	 * @return updated image
	 * @author xiangqian
	 */
	public Image updateImage(String imageId, String imageName, boolean publicity);

	/**
	 * delete image.
	 * 
	 * @param imageId
	 *            - image id
	 * @return true if delete request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean deleteImage(String imageId);

	/**
	 * get hypervisor list in the cloud.
	 * 
	 * @return the hypervisor list
	 * @author xiangqian
	 */
	public List<? extends Hypervisor> getHypervisors();

	/**
	 * get server list of a project.
	 * 
	 * @return server list
	 * @author xiangqian
	 */
	public List<? extends Server> getServers();

	/**
	 * get flavor.
	 * 
	 * @param cpu
	 *            - cpu parameter
	 * @param memory
	 *            - memory parameter
	 * @param disk
	 *            - disk parameter
	 * @return flavor with the given parameters, or null if not found
	 * @author xiangqian
	 */
	public Flavor getFlavor(int cpu, int memory, int disk);

	/**
	 * create flavor.
	 * 
	 * @param cpu
	 *            - cpu parameter
	 * @param memory
	 *            - memory parameter
	 * @param disk
	 *            - disk parameter
	 * @return created flavor
	 * @author xiangqian
	 */
	public Flavor createFlavor(int cpu, int memory, int disk);

	/**
	 * create server.
	 * 
	 * @param serverName
	 *            - server name
	 * @param flavorId
	 *            - flavor id
	 * @param imageId
	 *            - image id
	 * @return created server
	 * @author xiangqian
	 */
	public Server bootServer(String serverName, String flavorId, String imageId);

	/**
	 * wait server until its status transfer to the wait status in the given time. the server task status is None, which
	 * means the server is in stable statue.
	 * 
	 * @param serverId
	 *            - server id
	 * @param statusList
	 *            - list of wait status
	 * @param minute
	 *            - wait time limit
	 * @return true if volume status transfer to wait status during the given time, return false if otherwise
	 * @throws InterruptedException
	 */
	public boolean waitServerStatus(String serverId, List<Status> statusList, int minute) throws InterruptedException;

	/**
	 * get server by id.
	 * 
	 * @param serverId
	 *            - server id
	 * @return server with the given id, or null if not found
	 * @author xiangqian
	 */
	public Server getServer(String serverId);

	/**
	 * start server.
	 * 
	 * @param serverId
	 *            - server id
	 * @return true if start request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean startServer(String serverId);

	/**
	 * reboot server.
	 * 
	 * @param serverId
	 *            - server id
	 * @return true if start request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean rebootServer(String serverId);

	/**
	 * stop server.
	 * 
	 * @param serverId
	 *            - server id
	 * @return true if start request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean stopServer(String serverId);

	/**
	 * delete server.
	 * 
	 * @param serverId
	 *            - server id
	 * @return true if delete request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean deleteServer(String serverId);

	/**
	 * get novnc console.
	 * 
	 * @param serverId
	 *            - server id
	 * @return console of server with the given id, or null if not found
	 * @author xiangqian
	 */
	public VNCConsole getServerVNCConsole(String serverId);

	/**
	 * renamve server.
	 * 
	 * @param serverId
	 *            - server id
	 * @param newName
	 *            - new name
	 * @return renamed server
	 * @author xiangqian
	 */
	public Server renameServer(String serverId, String newName);

//	/**
//	 * associate floating ip to server.
//	 * 
//	 * @param serverId
//	 *            - server id
//	 * @param floatingIpAddress
//	 *            - floating ip address
//	 * @return action result
//	 * @author xiangqian
//	 */
//	@Deprecated
//	public ActionResult associateFloatingIp(String serverId, String floatingIpAddress);
//
//	/**
//	 * remove floating ip from server, and deallocate back to pool.
//	 * 
//	 * @param serverId
//	 *            - server id
//	 * @param floatingIpAddress
//	 *            - floating ip address
//	 * @return action result
//	 * @author xiangqian
//	 */
//	@Deprecated
//	public ActionResult deallocateFloatingIp(String serverId, String floatingIpAddress);

	/**
	 * create snapshot of given server.
	 * 
	 * @param serverId
	 *            - server id
	 * @param snapshotName
	 *            - snapshot name
	 * @return snapshot id
	 * @author xiangqian
	 */
	public String createSnapshot(String serverId, String snapshotName);

	/**
	 * attach volume to a server.
	 * 
	 * @param serverId
	 *            - server id
	 * @param volumeId
	 *            - volume id
	 * @return true if attach request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean attachVolume(String serverId, String volumeId);

	/**
	 * detach volume from a server.
	 * 
	 * @param serverId
	 *            - server id
	 * @param volumeId
	 *            - volume id
	 * @return true if detach request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean detachVolume(String serverId, String volumeId);

	/**
	 * live migrate server to the hypervisor.
	 * 
	 * @param serverId
	 *            - server id
	 * @param hypervisorName
	 *            - hypervisor name
	 * @return true if migrate request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean liveMigrate(String serverId, String hypervisorName);

	/**
	 * get server info.
	 * 
	 * @param serverId
	 *            - server id
	 * @return server info with the given id, or null if not found
	 * @author xiangqian
	 */
	public ServerInfo getServerInfo(String serverId);

	/**
	 * create alarm.
	 * 
	 * @param serverId
	 *            - server id
	 * @param alarmName
	 *            - alarm name
	 * @param meterName
	 *            - meter name
	 * @param threshold
	 *            - threshold
	 * @return alarm id
	 * @author xiangqian
	 */
	public String createAlarm(String serverId, String alarmName, String meterName, float threshold);

	/**
	 * update alarm
	 * 
	 * @param alarmId
	 *            - alarm id
	 * @param enabled
	 *            - enabled
	 * @param threshold
	 *            - alarm threshold
	 * @return true if update request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean updateAlarm(String alarmId, boolean enabled, float threshold);

	/**
	 * delete alarm.
	 * 
	 * @param alarmId
	 *            - alarm id
	 * @return true if delete request is sent successfully, return false if otherwise
	 * @author xiangqian
	 */
	public boolean deleteAlarm(String alarmId);

	/**
	 * get alarm state by id
	 * 
	 * @param alarmId
	 *            - alarm id
	 * @return alarm state, or null if alarm not found
	 * @author xiangqian
	 */
	public String getAlarmState(String alarmId);

	/**
	 * get server load data.
	 * 
	 * @param serverId
	 *            - server id
	 * @param meterName
	 *            - meter name
	 * @param timestamp
	 *            - start time
	 * @return server sample
	 * @author xiangqian
	 */
	public ServerSamples getSamples(String serverId, String meterName, long timestamp);

	/**
	 * get external ip range. the range includes a collection of ips defined in all allocation pools.
	 * 
	 * @return ip list
	 * @author xiangqian
	 */
	public List<String> getExternalIps();

	/**
	 * get router gateway port list.
	 * 
	 * @return gateway port list
	 * @author xiangqian
	 */
	public List<? extends Port> getGatewayPorts();

	/**
	 * get floating ip port list.
	 * 
	 * @return floating ip port list
	 * @author xiangqian
	 */
	public List<? extends Port> getFloatingIpPorts();

	/**
	 * get floating ip created in the cloud.
	 * 
	 * @return floating ip list
	 * @author xiangqian
	 */
	public List<? extends NetFloatingIP> getFloatingIps();

	/**
	 * get port.
	 * 
	 * @param portId
	 *            - port id
	 * @return port with the given id, or null if not found
	 * @author xiangqian
	 */
	public Port getPort(String portId);

	/**
	 * get router.
	 * 
	 * @param routerId
	 *            - router id
	 * @return router with the given id, or null if not found
	 * @author xiangqian
	 */
	public Router getRouter(String routerId);

	/**
	 * create a floating ip and associate it to the given server.
	 * 
	 * @param ipAddress
	 *            - floating ip address
	 * @param serverId
	 *            - server id
	 * @return action result
	 * @author xiangqian
	 */
	public ActionResult createFloatingIp(String ipAddress, String serverId);

	/**
	 * delete the floating ip, it'll be disassociated from server.
	 * 
	 * @param ipAddress
	 *            - floating ip address
	 * @return action result
	 * @author xiangqian
	 */
	public ActionResult deleteFloatingIp(String ipAddress);
}
