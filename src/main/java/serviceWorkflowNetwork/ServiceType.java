package serviceWorkflowNetwork;

import java.io.Serializable;

public enum ServiceType implements Serializable{
    WSDL, BIOMOBY, SOAPLAB, REST, SADI, ARBITRARYGT4, LOCAL;

    public static ServiceType findServiceType(String serviceTypeStr) {
        serviceTypeStr = serviceTypeStr.toLowerCase().trim();
        if (serviceTypeStr.contains("soaplab"))
            return SOAPLAB;
        if (serviceTypeStr.contains("biomoby"))
            return BIOMOBY;
        if (serviceTypeStr.contains("rest"))
            return REST;
        if (serviceTypeStr.contains("sadi"))
            return SADI;
        if (serviceTypeStr.contains("arbitrarygt4"))
            return ARBITRARYGT4;
        if (serviceTypeStr.contains("wsdl"))
            return WSDL;
        return null;
    }
}