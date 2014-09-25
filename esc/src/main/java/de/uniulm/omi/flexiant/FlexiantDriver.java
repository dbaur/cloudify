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
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FlexiantDriver extends BaseProvisioningDriver {

    protected Cloud cloud;
    protected ComputeTemplate managementMachineTemplate;
    protected FlexiantComputeClient flexiantComputeClient;
    protected static final String ENDPOINT_OVERRIDE = "flexiant.endpoint";
    protected static final String DISK_PRODUCT_OFFER_OVERRIDE = "diskProductOffer";

    protected final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(this.getClass().getName());

    private static ResourceBundle defaultProvisioningDriverMessageBundle = ResourceBundle.getBundle(
            "DefaultProvisioningDriverMessages", Locale.getDefault());

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#initDeployer(org.cloudifysource.domain.cloud.Cloud)
     */
    @Override
    protected void initDeployer(Cloud cloud) {

        System.setProperty("jsse.enableSNIExtension", "false");

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
    }

    /**
     * Creates a machine details object from the provided template and the server.
     *
     * @param template the template object
     * @param server   the server.
     * @return the machine details.
     */
    protected MachineDetails createMachineDetails(final ComputeTemplate template, final FlexiantServer server) {

        final MachineDetails md = this.createMachineDetailsForTemplate(template);

        md.setMachineId(server.getId());
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
            final FlexiantServer server = this.flexiantComputeClient.createServer(serverName, serverProductOffer, diskProductOffer, vdc, network, image);
            this.flexiantComputeClient.startServer(server.getId());
            logger.fine(String.format("Created new server with resource id %s", server.getId()));

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

        try {
            this.stopManagementMachines();
        } catch (TimeoutException e) {
            throw new CloudProvisioningException("A timout occured, while handling the provisioning failure", e);
        }

    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#getExistingManagementServers()
     */
    @Override
    public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {

        try {
            final List<FlexiantServer> servers = this.flexiantComputeClient.getServers(this.serverNamePrefix);

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

        //retrieve any existing management machines
        final MachineDetails[] managementMachines = this.getExistingManagementServers();

        for (final MachineDetails machineDetails : managementMachines) {
            try {
                this.flexiantComputeClient.deleteServer(machineDetails.getMachineId());
            } catch (FlexiantException e) {
                throw new CloudProvisioningException(String.format("Could not stop server with id %s", machineDetails.getMachineId()), e);
            }
        }
    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#stopMachine()
     */
    @Override
    public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
            throws InterruptedException,
            TimeoutException, CloudProvisioningException {

        FlexiantServer server = null;

        try {
            server = this.flexiantComputeClient.getServerByIp(machineIp);
        } catch (FlexiantException e) {
            throw new CloudProvisioningException(String.format("Error while retrieving server with ip %s", machineIp), e);
        }

        if (server == null) {
            throw new CloudProvisioningException(String.format("Could not find a server with ip %s", machineIp));
        }


        try {
            this.flexiantComputeClient.deleteServer(server);
        } catch (FlexiantException e) {
            throw new CloudProvisioningException(String.format("Could not delete server with ip %s", machineIp), e);
        }

        return true;
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

    protected String getFormattedMessage(final String msgName, final Object... arguments) {
        return getFormattedMessage(getDefaultProvisioningDriverMessageBundle(), msgName, arguments);
    }

    protected static ResourceBundle getDefaultProvisioningDriverMessageBundle() {
        if (defaultProvisioningDriverMessageBundle == null) {
            defaultProvisioningDriverMessageBundle = ResourceBundle.getBundle("DefaultProvisioningDriverMessages",
                    Locale.getDefault());
        }
        return defaultProvisioningDriverMessageBundle;
    }

    /**
     * @see org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#validateCloudConfiguration(org.cloudifysource.esc.driver.provisioning.context.ValidationContext)
     */
    @Override
    public void validateCloudConfiguration(ValidationContext validationContext) throws CloudProvisioningException {
        super.validateCloudConfiguration(validationContext);

        this.validateTemplates(validationContext, this.cloud.getCloudCompute().getTemplates());

    }

    protected void validateTemplates(final ValidationContext validationContext, final Map<String, ComputeTemplate> computeTemplates) throws CloudProvisioningException {

        validationContext.validationEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
                getFormattedMessage("validating_all_templates"));

        if (computeTemplates != null && !computeTemplates.isEmpty()) {
            for (final Map.Entry<String, ComputeTemplate> entry : computeTemplates.entrySet()) {
                // we validate location first, as this is needed in further validations.
                this.validateLocation(validationContext, entry.getValue(), entry.getKey());
                this.validateImage(validationContext, entry.getValue(), entry.getKey());
                this.validateHardware(validationContext, entry.getValue(), entry.getKey());
            }
        } else {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException("Could not find any compute templates");
        }

    }

    protected void validateImage(final ValidationContext validationContext, final ComputeTemplate computeTemplate, final String computeTemplateName) throws CloudProvisioningException {

        validationContext.validationOngoingEvent(
                ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
                String.format("Validating image of template %s", computeTemplateName)
        );

        // check if a image id is defined
        if (computeTemplate.getImageId() == null || computeTemplate.getImageId().isEmpty()) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "Found empty imageId in template %s",
                            computeTemplateName
                    )
            );
        }

        // check if image is defined at flexiant and can be retrieved
        FlexiantImage image = null;
        try {
            image = this.flexiantComputeClient.getImage(computeTemplate.getImageId(), computeTemplate.getLocationId());
        } catch (FlexiantException e) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "Error while retrieving image with id %s",
                            computeTemplate.getImageId()
                    ), e
            );
        }

        if (image == null) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "Could not retrieve the image with id %s.",
                            computeTemplate.getImageId()
                    )
            );
        }

        // the flexiant image needs a default user defined, otherwise we can not ssh
        if (image.getDefaultUser() == null || image.getDefaultUser().isEmpty()) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "The image %s has no default user configured",
                            image.getId()
                    )
            );
        }

        // the flexiant default user needs to equal our user
        if (!image.getDefaultUser().equals(computeTemplate.getUsername())) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "The username in the image (%s) does not equal the username in the template (%s)",
                            image.getDefaultUser(),
                            computeTemplate.getUsername()
                    )
            );
        }

        // the template should not contain a password as it will be ignored
        if (computeTemplate.getPassword() != null) {
            validationContext.validationEventEnd(ValidationResultType.WARNING);
            logger.warning(
                    "You configured a password for a computeTemplate. This is not supported by this driver."
            );
        }

        // the template should not contain a key file as it will be ignored
        if (computeTemplate.getKeyFile() != null) {
            validationContext.validationEventEnd(ValidationResultType.WARNING);
            logger.warning(
                    "You configured a keyFile for a computeTemplate. This is not supported by this driver."
            );
        }

        // the image at flexiant must have generate password enabled, otherwise we can not ssh
        if (!image.isGenPassword()) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "Auto generate password is disable in the image %s",
                            image.getId()
                    )
            );
        }

        // no errors -> validation passed
        validationContext.validationEventEnd(ValidationResultType.OK);
    }

    protected void validateHardware(ValidationContext validationContext, ComputeTemplate template, String computeTemplateName) throws CloudProvisioningException {
        validationContext.validationOngoingEvent(
                ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
                String.format("Validating hardware of template %s", computeTemplateName)
        );

        // check if we have a hardware id
        if (template.getHardwareId() == null || template.getHardwareId().isEmpty()) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    "No hardwareId configured."
            );
        }

        //we retrieve the hardware from flexiant and check if it exists
        FlexiantHardware hardware = null;
        try {
            hardware = this.flexiantComputeClient.getHardware(template.getHardwareId());
        } catch (FlexiantException e) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "Error while retrieving the hardware %s",
                            template.getHardwareId()
                    ), e
            );
        }

        if (hardware == null) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "Could not retrieve the hardware with id %s",
                            template.getHardwareId()
                    )
            );
        }

        validationContext.validationEventEnd(ValidationResultType.OK);
    }

    protected void validateLocation(ValidationContext validationContext, ComputeTemplate template, String computeTemplateName) throws CloudProvisioningException {
        validationContext.validationOngoingEvent(
                ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
                String.format("Validating location of template %s", computeTemplateName)
        );

        //check if we have a location
        if (template.getLocationId() == null || template.getLocationId().isEmpty()) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    "No locationId configured."
            );
        }

        // retrieve the location from flexiant and check it
        FlexiantLocation location = null;
        try {
            location = this.flexiantComputeClient.getLocation(template.getLocationId());
        } catch (FlexiantException e) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "Error while retrieving the location with id %s",
                            template.getLocationId()
                    ), e
            );
        }

        //check
        if (location == null) {
            validationContext.validationEventEnd(ValidationResultType.ERROR);
            throw new CloudProvisioningException(
                    String.format(
                            "Could not retrieve the location with id %s",
                            template.getLocationId()
                    )
            );
        }

        validationContext.validationEventEnd(ValidationResultType.OK);
    }
}
