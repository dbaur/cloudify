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

    @Override
    protected void handleProvisioningFailure(int numberOfManagementMachines,
                                             int numberOfErrors, Exception firstCreationException,
                                             MachineDetails[] createdManagementMachines)
            throws CloudProvisioningException {
    }

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

    @Override
    public void validateCloudConfiguration(ValidationContext validationContext) throws CloudProvisioningException {
        super.validateCloudConfiguration(validationContext);

    }
}
