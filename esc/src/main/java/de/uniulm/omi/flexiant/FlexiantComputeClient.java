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

import java.util.ArrayList;
import java.util.List;

import net.flexiant.extility.Condition;
import net.flexiant.extility.Disk;
import net.flexiant.extility.ExtilityException;
import net.flexiant.extility.FilterCondition;
import net.flexiant.extility.Image;
import net.flexiant.extility.Job;
import net.flexiant.extility.ListResult;
import net.flexiant.extility.Nic;
import net.flexiant.extility.ProductOffer;
import net.flexiant.extility.ResourceType;
import net.flexiant.extility.SearchFilter;
import net.flexiant.extility.Server;
import net.flexiant.extility.ServerStatus;
import net.flexiant.extility.Vdc;

/**
 * Client for calling compute operations on flexiants extility api.
 */
public class FlexiantComputeClient extends FlexiantBaseClient {

    /**
     * @see de.uniulm.omi.flexiant.FlexiantComputeClient#FlexiantComputeClient(String, String, String)
     */
    public FlexiantComputeClient(final String endpoint, final String apiUserName, final String password) {
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
	public List<FlexiantServer> getServers(final String prefix) throws FlexiantException {
    	List<FlexiantServer> servers = new ArrayList<FlexiantServer>();
    	for(Object o : this.getResources(prefix, "resourceName", ResourceType.SERVER)) {
    		servers.add(new FlexiantServer((Server) o));
    	}
    	return servers;
    }
    
    /**
     * Returns all servers.
     * 
     * @return all servers.
     * 
     * @throws FlexiantException
     */
	public List<FlexiantServer> getServers() throws FlexiantException {
		List<FlexiantServer> servers = new ArrayList<FlexiantServer>();
    	for(Object o : this.getResources(ResourceType.SERVER)) {
    		servers.add(new FlexiantServer((Server) o));
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
    public FlexiantServer getServerByIp(String ip) throws FlexiantException {
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
    public FlexiantServer getServerByIp(String ip, String filter) throws FlexiantException {
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
    protected FlexiantServer searchByIp(List<FlexiantServer> servers, String ip) {
    	for(FlexiantServer server : servers) {
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
    public FlexiantServer createServer(String serverName, String serverProductOffer, String diskProductOffer, String vdc, String network, String image) throws FlexiantException {
        Server server = new Server();
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
    public void startServer(FlexiantServer server) throws FlexiantException {
        if(server == null) {
            throw new IllegalArgumentException("The given server must not be null.");
        }

        this.startServer(server.getId());
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
    public void stopServer(FlexiantServer server) throws FlexiantException {
        if(server == null) {
            throw new IllegalArgumentException("The given server must not be null.");
        }

        this.stopServer(server.getId());
    }
    
    /**
     * Deletes the given server.
     * 
     * @param server the server to be deleted.
     * 
     * @throws FlexiantException
     */
    public void deleteServer(final FlexiantServer server) throws FlexiantException {
    	this.deleteServer(server.getId());
    }
    
    /**
     * Deletes the server identified by the given id.
     * 
     * @param server id of the server
     * 
     * @throws FlexiantException
     */
    public void deleteServer(final String serverUUID) throws FlexiantException {
    	this.deleteResource(serverUUID);
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
     * Returns information about the given server.
     *
     * @param serverUUID the id of the server.
     *
     * @return a server object containing the information about the server if any, otherwise null.
     *
     * @throws FlexiantException
     */
    public FlexiantServer getServer(final String serverUUID) throws FlexiantException {
        final Server server = (Server) this.getResource(serverUUID, ResourceType.SERVER);
        if(server == null) {
        	return null;
        }
        return new FlexiantServer(server);
    }
    
    /**
     * Retrieves the image identified by the given uuid.
     * 
     * @param imageUUID the id of the image
     * 
     * @return information about the image if any, otherwise null.
     * 
     * @throws FlexiantException
     */
    public FlexiantImage getImage(final String imageUUID) throws FlexiantException {
    	final Image image = (Image) this.getResource(imageUUID, ResourceType.IMAGE);
    	if(image == null) {
        	return null;
        }
    	return new FlexiantImage(image);
    }
    
    /**
     * Retrieves the hardware identified by the given uuid.
     *
     * @param hardwareUUID the id of the hardware
     * 
     * @return information about the hardware if any, otherwise null.
     * 
     * @throws FlexiantException
     */
    public FlexiantHardware getHardware(final String hardwareUUID) throws FlexiantException {
    	final ProductOffer productOffer = (ProductOffer) this.getResource(hardwareUUID, ResourceType.PRODUCTOFFER);
    	if(productOffer == null) {
        	return null;
        }
    	return new FlexiantHardware(productOffer);
    }
    
    /**
     * Retrieves the location identified by the given uuid.
     * 
     * @param locationUUID the id of the location.
     * 
     * @return an object representing the location if any, otherwise null.
     * 
     * @throws FlexiantException
     */
    public FlexiantLocation getLocation(final String locationUUID) throws FlexiantException {
    	final Vdc vdc = (Vdc) this.getResource(locationUUID, ResourceType.VDC);
    	if(vdc == null) {
    		return null;
    	}
    	return new FlexiantLocation(vdc);
    }

    /**
     * Retrieves a ressource from the flexiant api, which is identified by the given resource UUID.
     * 
     * @param resourceUUID the uuid of the resource.
     * @param type the type of the resource.
     * 
     * @return an object representing the resource if any, otherwise null.
     * 
     * @throws FlexiantException if an error occurred during the call to the api.
     */
    protected Object getResource(final String resourceUUID, final ResourceType type) throws FlexiantException {
    	
    	SearchFilter sf = new SearchFilter();
        FilterCondition fc = new FilterCondition();

        fc.setCondition(Condition.IS_EQUAL_TO);
        fc.setField("resourceUUID");
        fc.getValue().add(resourceUUID);
        sf.getFilterConditions().add(fc);
        
        try {
            ListResult result = this.getService().listResources(sf, null, type);

            if (result.getList().size() > 1) {
                throw new FlexiantException(String.format("Found multiple resource with the uuid %s", resourceUUID));
            }
            
            if(result.getList().isEmpty()) {
            	return null;
            }

            return result.getList().get(0);

        } catch (ExtilityException e) {
            throw new FlexiantException(String.format("Error while retrieving ressource %s", resourceUUID), e);
        }
    }
    
    /**
     * Retrieves a list of resources matching the given prefix on the given attribute
     * which are of the given type.
     * 
     * @param prefix the prefix to match.
     * @param attribute the attribute where the prefix should match.
     * @param type the type of the resource.
     * 
     * @return a list containing all resources matching the request.
     * 
     * @throws FlexiantException if an error occurs while contacting the api.
     */
    protected List<? extends Object> getResources(final String prefix, final String attribute, final ResourceType type) throws FlexiantException {
    	
    	SearchFilter sf = new SearchFilter();
        FilterCondition fc = new FilterCondition();

        fc.setCondition(Condition.STARTS_WITH);
        fc.setField(attribute);

        fc.getValue().add(prefix);

        sf.getFilterConditions().add(fc);

        try {
			return this.getService().listResources(sf, null, type).getList();
		} catch (ExtilityException e) {
			throw new FlexiantException(String.format("Error while retrieving resource with prefix %s on attribute %s of type %s",prefix, attribute, type),e);
		}
    }
    
    /**
     * Returns all resources of the given type.
     * 
     * @param type the resource type.
     * 
     * @return a list of all resources of the given type.
     * 
     * @throws FlexiantException
     */
    protected List<? extends Object> getResources(final ResourceType type) throws FlexiantException{
    	try {
			return this.getService().listResources(null, null, type).getList();
		} catch (ExtilityException e) {
			throw new FlexiantException(String.format("Error while retrieving resources of type %s.", type),e);
		}
    }
}
