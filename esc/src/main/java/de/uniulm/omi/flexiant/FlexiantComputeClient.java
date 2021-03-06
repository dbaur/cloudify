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

import net.flexiant.extility.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for calling compute operations on flexiants extility api.
 */
public class FlexiantComputeClient extends FlexiantBaseClient {

    /**
     * @see de.uniulm.omi.flexiant.FlexiantComputeClient#FlexiantComputeClient(String, String, String)
     */
    public FlexiantComputeClient(String endpoint, String apiUserName, String password) {
        super(endpoint, apiUserName, password);
    }

    /**
     * Returns all servers whose names are matching the given prefix.
     *
     * @param prefix the prefix the server names should match.
     *
     * @return a list of servers matching the prefix
     *
     * @throws FlexiantException
     */
    public List<Server> getServers(String prefix) throws FlexiantException {

        SearchFilter sf = new SearchFilter();
        FilterCondition fc = new FilterCondition();

        fc.setCondition(Condition.STARTS_WITH);
        fc.setField("resourceName");

        fc.getValue().add(prefix);

        sf.getFilterConditions().add(fc);

        try {
            return this.getServers(sf);
        } catch (ExtilityException e) {
            throw new FlexiantException("Could not retrieve list of servers", e);
        }

    }
    
    /**
     * Returns all servers.
     * 
     * @return all servers.
     * 
     * @throws FlexiantException
     */
    public List<Server> getServers() throws FlexiantException {
    	try{
    		SearchFilter sf = null;
    		return this.getServers(sf);
    	} catch (ExtilityException e) {
    		throw new FlexiantException("Could not retrieve list of servers", e);
    	}
    }
    
    /**
     * Returns all servers matching search filter.
     * 
     * @param sf the search filter.
     * 
     * @return all servers matching the given search filter.
     * 
     * @throws ExtilityException
     */
    protected List<Server> getServers(SearchFilter sf) throws ExtilityException{
    	
    	ListResult result = this.getService().listResources(sf, null, ResourceType.SERVER);
    	
        java.util.ArrayList<Server> servers = new ArrayList<Server>();
        
        for (Object server : result.getList()) {
            servers.add(this.mapServer((net.flexiant.extility.Server) server));
        }
        
        return servers;
    }
           
    /**
     * Retrieves the server having the given ip.
     * 
     * It seems that flexiant does not allow to query by ip. Therefore, this
     * query loops through all servers, finding the ip.
     * 
     * @param ip the ip of the server.
     * 
     * @return the server having the given ip.
     *  
     * @throws FlexiantException
     */
    public Server getServerByIp(String ip) throws FlexiantException {
    	return this.searchByIp(this.getServers(), ip);
    }
    
    /**
     * Retrieves the server having the ip and matching the given name filter.
     * 
     * As we need to loop through all existing servers, the name filter can increase
     * the speed. 
     * 
     * @see FlexiantComputeClient#getServerByIp(String)
     * 
     * @param ip the ip of the server.
     * @param filter prefix to filter list of servers
     * 
     * @return the server with having the given ip
     * 
     * @throws FlexiantException
     */
    public Server getServerByIp(String ip, String filter) throws FlexiantException {
    	return this.searchByIp(this.getServers(filter), ip);
    }
    
    /**
     * Searches for the given ip, in the given list of servers.
     * 
     * @param servers list of servers to search in.
     * @param ip ip to search for.
     * 
     * @return the server matching the ip or null
     */
    protected Server searchByIp(List<Server> servers, String ip) {
    	for(Server server : servers) {
    		if(server.getPublicIpAddress().equals(ip) || server.getPrivateIpAddress().equals(ip)) {
    			return server;
    		}
    	}
    	return null;
    }
    
    /**
     * Creates a server with the given properties.
     *
     * @param serverName name of the server.
     * @param serverProductOffer the product offer to use.
     * @param diskProductOffer the product offer to use for the disk.
     * @param vdc the virtual data center in which the server is created.
     * @param network the network the node will be attached.
     * @param image the os image to use for the server.
     *
     * @return the created server.
     *
     * @throws FlexiantException
     */
    public Server createServer(String serverName, String serverProductOffer, String diskProductOffer, String vdc, String network, String image) throws FlexiantException {
        net.flexiant.extility.Server server = new net.flexiant.extility.Server();
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

    /**
     * Starts the given server.
     *
     * @param serverUUID the uuid of the server
     *
     * @throws FlexiantException
     */
    public void startServer(String serverUUID) throws FlexiantException {
        this.changeServerStatus(serverUUID, ServerStatus.RUNNING);
    }

    /**
     * Starts the given server
     *
     * @see de.uniulm.omi.flexiant.FlexiantComputeClient#startServer(String)
     *
     * @param server the server to start.
     *
     * @throws FlexiantException
     */
    public void startServer(Server server) throws FlexiantException {
        if(server == null) {
            throw new IllegalArgumentException("The given server must not be null.");
        }

        this.startServer(server.getServerId());
    }

    /**
     * Stops the given server.
     *
     * @param serverUUID the uuid of the server to stop.
     *
     * @throws FlexiantException
     */
    public void stopServer(String serverUUID) throws FlexiantException {
        this.changeServerStatus(serverUUID, ServerStatus.STOPPED);
    }

    /**
     * Stops the given server.
     *
     * @see de.uniulm.omi.flexiant.FlexiantComputeClient#stopServer(String)
     *
     * @param server the server to stop.
     *
     * @throws FlexiantException
     */
    public void stopServer(Server server) throws FlexiantException {
        if(server == null) {
            throw new IllegalArgumentException("The given server must not be null.");
        }

        this.stopServer(server.getServerId());
    }
    
    /**
     * Deletes the given server.
     * 
     * @param server the server to be deleted.
     * 
     * @throws FlexiantException
     */
    public void deleteServer(final Server server) throws FlexiantException {
    	this.deleteServer(server.getServerId());
    }
    
    /**
     * Deletes the server identified by the given id.
     * 
     * @param server id of the server
     * 
     * @throws FlexiantException
     */
    public void deleteServer(final String server) throws FlexiantException {
    	this.deleteResource(server);
    }
     
    /**
     * Deletes a resource (and all related entities) identified by the given uuid.
     * 
     * @param uuid of the resource.
     * 
     * @throws FlexiantException if the resource can not be deleted.
     */
    protected void deleteResource(final String uuid) throws FlexiantException {
    	try {
			final Job job = this.getService().deleteResource(uuid, true, null);
			this.getService().waitForJob(job.getResourceUUID(), true);
		} catch (ExtilityException e) {
			throw new FlexiantException("Could not delete resource",e);
		}
    }

    /**
     * Changes the server status to the given status.
     *
     * @param serverUUID the id of the server.
     * @param status the status the server should change to.
     *
     * @throws FlexiantException
     */
    protected void changeServerStatus(String serverUUID, ServerStatus status) throws FlexiantException {
        try {
            Job job = this.getService().changeServerStatus(serverUUID, status, true, null, null);
            this.getService().waitForJob(job.getResourceUUID(), true);
        } catch (ExtilityException e) {
            throw new FlexiantException("Could not start server", e);
        }
    }

    /**
     * Maps the given flexiant server to an local server.
     *
     * @see net.flexiant.extility.Server
     * @see de.uniulm.omi.flexiant.Server
     *
     * @param server the flexiant server
     *
     * @return the local server.
     */
    protected Server mapServer(net.flexiant.extility.Server server) {
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
    }

    /**
     * Returns information about the given server.
     *
     * @param serverUUID the id of the server.
     *
     * @return a server object containing the information about the server.
     *
     * @throws FlexiantException
     */
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

            net.flexiant.extility.Server server = (net.flexiant.extility.Server) result.getList().get(0);

            return this.mapServer(server);

        } catch (ExtilityException e) {
            throw new FlexiantException(String.format("Error while creating server %s", serverUUID), e);
        }
    }


}
