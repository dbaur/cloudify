package de.uniulm.omi.flexiant;

import com.extl.jade.user.*;

/**
 * Created by daniel on 28.04.14.
 */
public class FlexiantComputeClient extends FlexiantBaseClient {

    public FlexiantComputeClient(String endpoint, String apiUserName, String password) {
        super(endpoint, apiUserName, password);
    }

    public Server createServer(String serverName, String serverProductOffer, String diskProductOffer, String vdc, String network, String image) throws FlexiantException {
        com.extl.jade.user.Server server = new com.extl.jade.user.Server();
        server.setResourceName(serverName);
        server.setCustomerUUID(this.getCustomerUUID());
        server.setProductOfferUUID(serverProductOffer);
        server.setVdcUUID(vdc);
        server.setImageUUID(image);

        Disk disk = new Disk();
        disk.setProductOfferUUID(diskProductOffer);
        disk.setIndex(0);

        server.getDisks().add(disk);

        Nic nic = new Nic();
        nic.setNetworkUUID(network);

        server.getNics().add(nic);

        try {
            Job serverJob = this.getService().createServer(server, null, null);
            this.getService().waitForJob(serverJob.getResourceUUID(), true);
            return this.getServer(serverJob.getItemUUID());
        } catch (ExtilityException e) {
            throw new FlexiantException("Could not create server", e);
        }
    }

    public void deleteServer(String server) throws FlexiantException {
        try {
            Job job = this.getService().deleteResource(server, true, null);
            this.getService().waitForJob(job.getResourceUUID(), true);
        } catch (ExtilityException e) {
            throw new FlexiantException("Could not delete server", e);
        }
    }

    public void startServer(String serverUUID) throws FlexiantException {
        this.changeServerStatus(serverUUID, ServerStatus.RUNNING);
    }

    public void stopServer(String serverUUID) throws FlexiantException {
        this.changeServerStatus(serverUUID, ServerStatus.STOPPED);
    }

    protected void changeServerStatus(String serverUUID, ServerStatus status) throws FlexiantException {
        try {
            Job job = this.getService().changeServerStatus(serverUUID, status, true, null, null);
            this.getService().waitForJob(job.getResourceUUID(), true);
        } catch (ExtilityException e) {
            throw new FlexiantException("Could not start server", e);
        }
    }

    public Server getServer(String serverUUID) throws FlexiantException {

        SearchFilter sf = new SearchFilter();
        FilterCondition fc = new FilterCondition();

        fc.setCondition(Condition.IS_EQUAL_TO);
        fc.setField("resourceUUID");
        fc.getValue().add(serverUUID);
        sf.getFilterConditions().add(fc);

        try {
            ListResult result = this.getService().listResources(sf, null, ResourceType.SERVER);

            if (result.getList().size() != 1) {
                throw new FlexiantException(String.format("Could not retrieve server %s", serverUUID));
            }

            com.extl.jade.user.Server server = (com.extl.jade.user.Server) result.getList().get(0);

            Server myServer = new Server();
            myServer.setServerId(server.getResourceUUID());
            myServer.setServerName(server.getResourceName());
            for (Nic nic : server.getNics()) {
                if (nic.getNetworkType().equals(NetworkType.IP)) {
                    for (Ip ip : nic.getIpAddresses()) {
                        if (ip.getType().equals(IpType.IPV_4)) {
                            myServer.setPublicIpAddress(ip.getIpAddress());
                            myServer.setPrivateIpAddress(ip.getIpAddress());
                        }
                    }
                }
            }
            myServer.setInitialUser(server.getInitialUser());
            myServer.setInitialPassword(server.getInitialPassword());

            return myServer;
        } catch (ExtilityException e) {
            throw new FlexiantException(String.format("Error while creating server %s", serverUUID), e);
        }
    }


}
