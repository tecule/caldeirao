package com.sinosoft.openstack;

public class CloudManipulatorFactory {
	public static CloudManipulator createCloudManipulator(CloudConfig config) {
		if (config.getCloudManipulatorVersion().equalsIgnoreCase("v2")) {
			return new CloudManipulatorV2(config);
		} else if (config.getCloudManipulatorVersion().equalsIgnoreCase("v3")) {
			return new CloudManipulatorV3(config);
		} else {
			return null;
		}
	}

	public static CloudManipulator createCloudManipulator(CloudConfig config, String projectId) {
		if (config.getCloudManipulatorVersion().equalsIgnoreCase("v2")) {
			return new CloudManipulatorV2(config, projectId);
		} else if (config.getCloudManipulatorVersion().equalsIgnoreCase("v3")) {
			return new CloudManipulatorV3(config, projectId);
		} else {
			return null;
		}
	}
}
