/*
 * Copyright 2014 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.uniulm.omi.flexiant;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.*;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FlexiantDriver extends BaseProvisioningDriver {

    protected Cloud cloud;
    protected ComputeTemplate managementMachineTemplate;
    protected FlexiantComputeClient flexiantComputeClient;
    protected static final String ENDPOINT_OVERRIDE = "flexiant.endpoint";
    protected static final String DISK_PRODUCT_OFFER_OVERRIDE = "diskProductOffer";

    protected final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(this.getClass().getName());

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#initDeployer(org.cloudifysource.domain.cloud.Cloud)
     */
    @Override
    protected void initDeployer(Cloud cloud) {
        this.cloud = cloud;

        // we take the endpoint from the mangement machine template, as the
        // provider url is mapped in the templates .....
        final String managementMachineTemplateName = cloud.getConfiguration().getManagementMachineTemplate();
        this.managementMachineTemplate = cloud.getCloudCompute().getTemplates().get(managementMachineTemplateName);

        final String endpoint = (String) this.managementMachineTemplate.getOverrides().get(ENDPOINT_OVERRIDE);
        final String password = this.cloud.getUser().getApiKey();
        final String apiUserName = this.cloud.getUser().getUser();

        logger.fine(String.format("Creating connection to flexiant endpoint %s with username=%s and password=*****", endpoint, apiUserName));
        this.flexiantComputeClient = new FlexiantComputeClient(endpoint, apiUserName, password);


        System.setProperty("jsse.enableSNIExtension", "false");
    }

    /**
     * Creates a machine details object from the provided template and the server.
     *
     * @param template the template object
     * @param server the server.
     *
     * @return the machine details.
     */
    protected MachineDetails createMachineDetails(final ComputeTemplate template, final Server server) {

        final MachineDetails md = this.createMachineDetailsForTemplate(template);

        md.setMachineId(server.getServerId());
        md.setCloudifyInstalled(false);
        md.setInstallationDirectory(null);
        md.setOpenFilesLimit(template.getOpenFilesLimit());
        md.setPrivateAddress(server.getPrivateIpAddress());
        md.setPublicAddress(server.getPublicIpAddress());
        md.setRemoteUsername(server.getInitialUser());
        md.setRemotePassword(server.getInitialPassword());

        return md;
    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#createServer(String, long, org.cloudifysource.domain.cloud.compute.ComputeTemplate)
     */
    @Override
    protected MachineDetails createServer(String serverName, long endTime,
                                          ComputeTemplate template) throws CloudProvisioningException,
            TimeoutException {

        final String serverProductOffer = template.getHardwareId();
        final String diskProductOffer = (String) template.getOverrides().get(DISK_PRODUCT_OFFER_OVERRIDE);
        final String vdc = template.getLocationId();
        final String network = template.getComputeNetwork().getNetworks().get(0);
        final String image = template.getImageId();

        logger.fine("Creating new server");

        try {
            final Server server = this.flexiantComputeClient.createServer(serverName, serverProductOffer, diskProductOffer, vdc, network, image);
            this.flexiantComputeClient.startServer(server.getServerId());
            logger.fine(String.format("Created new server with resource id %s",server.getServerId()));
            return this.createMachineDetails(template, server);
        } catch (FlexiantException e) {
            throw new CloudProvisioningException("Could not create server", e);
        }
    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#handleProvisioningFailure(int, int, Exception, org.cloudifysource.esc.driver.provisioning.MachineDetails[])
     */
    @Override
    protected void handleProvisioningFailure(int numberOfManagementMachines,
                                             int numberOfErrors, Exception firstCreationException,
                                             MachineDetails[] createdManagementMachines)
            throws CloudProvisioningException {
    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#getExistingManagementServers()
     */
    @Override
    public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {

        try {
            final List<Server> servers = this.flexiantComputeClient.getServersByPrefix(this.serverNamePrefix);

            MachineDetails[] mds = new MachineDetails[servers.size()];
            for (int i = 0; i < servers.size(); i++) {
                mds[i] = this.createMachineDetails(this.managementMachineTemplate, servers.get(i));
            }
            return mds;

        } catch (FlexiantException e) {
            throw new CloudProvisioningException("Could not retrieve existing management servers", e);
        }

    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#stopManagementMachines()
     */
    @Override
    public void stopManagementMachines()
            throws TimeoutException, CloudProvisioningException {
        throw new UnsupportedOperationException("Method not implemented");
    }

    @Override
    public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
            throws InterruptedException,
            TimeoutException, CloudProvisioningException {
        logger.info(String.format("Deleting server with id = %s",machineIp));
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#startManagementMachines(org.cloudifysource.esc.driver.provisioning.ManagementProvisioningContext, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
                                                    final TimeUnit unit)
            throws TimeoutException,
            CloudProvisioningException {

        // check if we have any existing management machines
        if (this.getExistingManagementServers().length > 0) {
            throw new CloudProvisioningException(String.format("Found existing servers matching group %s", this.serverNamePrefix));
        }

        final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
        final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

        publishEvent(EVENT_ATTEMPT_START_MGMT_VMS);
        final MachineDetails[] createdMachines = this.doStartManagementMachines(endTime, numberOfManagementMachines);
        publishEvent(EVENT_MGMT_VMS_STARTED);

        return createdMachines;
    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#startMachine(org.cloudifysource.esc.driver.provisioning.ProvisioningContext, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public MachineDetails startMachine(final ProvisioningContext context, final long duration, final TimeUnit unit)
            throws TimeoutException, CloudProvisioningException {

        final ComputeTemplate computeTemplate =
                this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
        final long end = System.currentTimeMillis() + unit.toMillis(duration);
        final String groupName =
                serverNamePrefix + this.configuration.getServiceName() + "-" + counter.incrementAndGet();

        final MachineDetails md = this.createServer(groupName, end, computeTemplate);
        return md;
    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#validateCloudConfiguration(org.cloudifysource.esc.driver.provisioning.context.ValidationContext)
     */
    @Override
    public void validateCloudConfiguration(ValidationContext validationContext) throws CloudProvisioningException {
        super.validateCloudConfiguration(validationContext);

    }
}
