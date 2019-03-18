package fr.devlogic;

import com.sun.tools.attach.VirtualMachine;
import sun.management.ConnectorAddressLink;

import javax.management.*;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;


public class JmxExplore {
    public static void main(String[] args) throws IOException {
        String pid = args[0];
        startManagementAgent(pid);
        String url = ConnectorAddressLink.importFrom(Integer.parseInt(pid));

        JMXServiceURL jmxURL = new JMXServiceURL(url);
        MBeanServerConnection mBeanServerConnection = JMXConnectorFactory.connect(jmxURL).getMBeanServerConnection();
        String[] domains = mBeanServerConnection.getDomains();
        System.out.println(Arrays.toString(domains));
        Set<ObjectInstance> objectInstances = mBeanServerConnection.queryMBeans(null, null);

        objectInstances.forEach(n -> {
            System.out.println("------------");
            final ObjectName objectName = n.getObjectName();
            System.out.println(n.getClassName() + " -> " + objectName.toString() + " " + objectName.getDomain());
            System.out.println("properties: ");
            objectName.getKeyPropertyList().forEach((k, v) -> {
                System.out.println(k + "=" + v);
            });

            try {

                MBeanInfo mBeanInfo = mBeanServerConnection.getMBeanInfo(objectName);
                MBeanAttributeInfo[] attributes1 = mBeanInfo.getAttributes();
                System.out.println("attributes: ");
                Stream.of(attributes1).forEach(System.out::println);

                MBeanOperationInfo[] operations = mBeanInfo.getOperations();
                System.out.println("operations: " );
                Stream.of(operations).forEach(System.out::println);

            } catch (InstanceNotFoundException e) {
                e.printStackTrace();
            } catch (ReflectionException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IntrospectionException e) {
                e.printStackTrace();
            }
        });
    }

    private static void startManagementAgent(String pid) throws IOException {
        /*
         * JAR file normally in ${java.home}/jre/lib but may be in ${java.home}/lib
         * with development/non-images builds
         */
        String home = System.getProperty("java.home");
        String agent = home + File.separator + "jre" + File.separator + "lib"
                + File.separator + "management-agent.jar";
        File f = new File(agent);
        if (!f.exists()) {
            agent = home + File.separator + "lib" + File.separator +
                    "management-agent.jar";
            f = new File(agent);
            if (!f.exists()) {
                throw new RuntimeException("management-agent.jar missing");
            }
        }
        agent = f.getCanonicalPath();

        System.out.println("Loading " + agent + " into target VM ...");

        try {
            VirtualMachine.attach(pid).loadAgent(agent);
        } catch (Exception x) {
            throw new IOException(x.getMessage());
        }
    }
}
