package com.sinosoft.openstack.type;

public class CloudConfig {
	private String cloudManipulatorVersion;
	private String authUrl;
	private String adminUsername;
	private String adminPassword;
	private String adminProjectName;
	private String publicNetworkId;
	private String adminUserId;
	private String domainName;
	private String domainId;
	private String adminProjectId;
	private String adminRoleName;
	private String aodhServiceUrl;

	public String getCloudManipulatorVersion() {
		return cloudManipulatorVersion;
	}

	public void setCloudManipulatorVersion(String cloudManipulatorVersion) {
		this.cloudManipulatorVersion = cloudManipulatorVersion;
	}

	public String getAuthUrl() {
		return authUrl;
	}

	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}

	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String getAdminProjectName() {
		return adminProjectName;
	}

	public void setAdminProjectName(String adminProjectName) {
		this.adminProjectName = adminProjectName;
	}

	public String getPublicNetworkId() {
		return publicNetworkId;
	}

	public void setPublicNetworkId(String publicNetworkId) {
		this.publicNetworkId = publicNetworkId;
	}

	public String getAdminUserId() {
		return adminUserId;
	}

	public void setAdminUserId(String adminUserId) {
		this.adminUserId = adminUserId;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	public String getAdminProjectId() {
		return adminProjectId;
	}

	public void setAdminProjectId(String adminProjectId) {
		this.adminProjectId = adminProjectId;
	}

	public String getAdminRoleName() {
		return adminRoleName;
	}

	public void setAdminRoleName(String adminRoleName) {
		this.adminRoleName = adminRoleName;
	}

	public String getAodhServiceUrl() {
		return aodhServiceUrl;
	}

	public void setAodhServiceUrl(String aodhServiceUrl) {
		this.aodhServiceUrl = aodhServiceUrl;
	}
}
