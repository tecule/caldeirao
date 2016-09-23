package com.sinosoft.openstack.exception;

@SuppressWarnings("serial")
public class CloudException extends RuntimeException  {
	public CloudException(String message, Throwable cause) {
		super(message, cause);
	}
}
