package de.uniulm.omi.flexiant;

import com.extl.jade.user.UserAPI;
import com.extl.jade.user.UserService;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.net.URL;

/**
 * Created by daniel on 28.04.14.
 */
public class FlexiantBaseClient {

    private UserService service;
    private String customerUUID;

    public FlexiantBaseClient(String endpoint, String apiUserName, String password) {

        this.customerUUID = apiUserName.split("/")[0];

        // Get the service WSDL from the client jar
        URL url = ClassLoader.getSystemClassLoader().getResource(
                "UserAPI.wsdl");

        // Get the UserAPI
        UserAPI api = new UserAPI(url,
                new QName("http://extility.flexiant.net", "UserAPI"));

        // and set the service port on the service
        service = api.getUserServicePort();

        // Get the binding provider
        BindingProvider portBP = (BindingProvider) service;

        // and set the service endpoint
        portBP.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpoint);

        // and the caller's authentication details and password
        portBP.getRequestContext().put(BindingProvider.USERNAME_PROPERTY,
                apiUserName);
        portBP.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY,
                password);
    }

    protected UserService getService() {
        return this.service;
    }

    protected String getCustomerUUID() {
        return this.customerUUID;
    }

}
