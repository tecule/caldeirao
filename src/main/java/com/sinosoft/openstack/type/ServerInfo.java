package com.sinosoft.openstack.type;

public class ServerInfo {
	private String privateIp;
	private String floatingIp;
	private String physicalMachine;
	
	public String getPrivateIp() {
		return privateIp;
	}
	public void setPrivateIp(String privateIp) {
		this.privateIp = privateIp;
	}
	public String getFloatingIp() {
		return floatingIp;
	}
	public void setFloatingIp(String floatingIp) {
		this.floatingIp = floatingIp;
	}
	public String getPhysicalMachine() {
		return physicalMachine;
	}
	public void setPhysicalMachine(String physicalMachine) {
		this.physicalMachine = physicalMachine;
	}
}
