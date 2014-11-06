package de.uniulm.omi.plugins;

import org.apache.commons.lang.math.NumberUtils;
import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.usm.Plugin;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.dsl.ServiceConfiguration;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.monitors.MonitorException;
import org.hyperic.sigar.Sigar;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by daniel on 05.11.14.
 */
public class CPUUsageProbe implements Plugin, Monitor {


    @Override
    public void setServiceContext(ServiceContext context) {
        //ignored
    }

    @Override
    public void setConfig(Map<String, Object> config) {
        //ignored
    }

    @Override
    public Map<String, Number> getMonitorValues(UniversalServiceManagerBean usm, ServiceConfiguration config) throws MonitorException {

        Map<String, Number> results = new HashMap<String, Number>();
        Sigar sigar = new Sigar();
        try {
            Long total = sigar.getCpu().getTotal();
            results.put("Total System CPU Time", NumberUtils.createNumber(String.valueOf(total)));
        } catch (Throwable t) {
            throw new MonitorException(t);
        }
        return results;
    }
}
