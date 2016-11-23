package com.sinosoft.openstack;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.QuotaSet;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.VNCConsole;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.BlockLimits.Absolute;

public interface CloudManipulator {
	public String createProject(String projectName, String projectDescription, int instanceQuota, int cpuQuota,
			int memoryQuota);

	public QuotaSet updateProjectComputeServiceQuota(int instanceQuota, int cpuQuota, int memoryQuota);

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

	public boolean deleteProject();

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
	 * @return true if modify request sent successfully, return false if
	 *         otherwise
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
	 * @return true if delete request sent successfully, return false if
	 *         otherwise
	 * @author xiangqian
	 */
	public boolean deleteVolume(String volumeId);

	/**
	 * wait volume until its status transfer to the wait status in the given
	 * time.
	 * 
	 * @param volumeId
	 *            - volume id
	 * @param statusList
	 *            - list of wait status
	 * @param minute
	 *            - wait time limit
	 * @return true if volume status transfer to wait status during the given
	 *         time, return false if otherwise
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
	 * @return true if volume is deleted during the given time, return false if
	 *         otherwise
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

	public Image getImage(String imageId);

	public void waitImageStatus(String imageId, org.openstack4j.model.image.Image.Status status, int minute)
			throws InterruptedException;

	public Image updateImage(String imageId, String imageName, boolean publicity);

	public boolean deleteImage(String imageId);

	/**
	 * get hypervisor list in the cloud.
	 * 
	 * @return the hypervisor list
	 * @author xiangqian
	 */
	public List<? extends Hypervisor> getHypervisors();

	public List<? extends Server> getServers();

	public Flavor getFlavor(int cpu, int memory, int disk);

	public Flavor createFlavor(int cpu, int memory, int disk);

	public Server bootServer(String serverName, String flavorId, String imageId);

	public void waitServerStatus(String serverId, List<Status> statusList, int minute) throws InterruptedException;

	public Server getServer(String serverId);

	public boolean startServer(String serverId);

	public boolean rebootServer(String serverId);

	public boolean stopServer(String serverId);

	public boolean deleteServer(String serverId);

	public VNCConsole getServerVNCConsole(String serverId);

	public Server renameServer(String serverId, String newName);

	public boolean associateFloatingIp(String serverId, String floatingIpAddress);

	public boolean deallocateFloatingIp(String serverId, String floatingIpAddress);

	public String createSnapshot(String serverId, String snapshotName);

	/**
	 * attach volume to a virtual machine.
	 * 
	 * @param serverId
	 *            - virtual machine id
	 * @param volumeId
	 *            - volume id
	 * @return true if attach request sent successfully, return false if
	 *         otherwise
	 * @author xiangqian
	 */
	public boolean attachVolume(String serverId, String volumeId);

	/**
	 * detach volume from a virtual machine.
	 * 
	 * @param serverId
	 *            - virtual machine id
	 * @param volumeId
	 *            - volume id
	 * @return true if detach request sent successfully, return false if
	 *         otherwise
	 * @author xiangqian
	 */
	public boolean detachVolume(String serverId, String volumeId);

	public boolean liveMigrate(String serverId, String hypervisorName);

	public Map<String, String> getServerInfo(String serverId);

	public String createAlarm(String serverId, String alarmName, String meterName, float threshold);

	public boolean updateAlarm(String alarmId, boolean enabled, float threshold);

	public boolean deleteAlarm(String alarmId);

	public String getAlarmState(String alarmId);

	public Map<String, Object> getSamples(String serverId, String meterName, long timestamp);

	public List<String> getFloatingIpRange();

	public List<? extends Port> getGateways();

	public List<? extends NetFloatingIP> getFloatingIpList();

	public List<? extends FloatingIP> getProjectFloatingIpList();

	public List<String> getAvailableFloatingIp();
}
