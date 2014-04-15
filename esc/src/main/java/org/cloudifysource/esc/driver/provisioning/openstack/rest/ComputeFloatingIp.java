package org.cloudifysource.esc.driver.provisioning.openstack.rest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * Represents a floating ip of the openstack compute a api
 * 
 * See <a href="http://docs.openstack.org/api/openstack-compute/2/content/GET_os-floating-ips-v2_ListFloatingIPs_v2__tenant_id__os-floating-ips_ext-os-floating-ips.html">Openstack API</a>
 * 
 * @author Daniel Baur
 *
 */
@JsonRootName("floatingip")
public class ComputeFloatingIp {
	
	private String fixedIp;
	private String id;
	private String instanceId;
	private String ip;
	private String pool;
	
	public String getFixedIp() {
		return fixedIp;
	}

	public void setFixedIp(String fixedIp) {
		this.fixedIp = fixedIp;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getPool() {
		return pool;
	}

	public void setPool(String pool) {
		this.pool = pool;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
