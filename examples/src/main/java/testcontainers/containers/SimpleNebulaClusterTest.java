package testcontainers.containers;

import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author 梦境迷离
 * @version 1.0, 2023/9/20
 */
public class SimpleNebulaClusterTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleNebulaClusterTest.class);

    public static void main(String[] args) {
        testNebulaContainerCluster();
    }


    public static void testNebulaContainerCluster() {

        try (NebulaClusterContainer cluster = new NebulaSimpleClusterContainer("v3.6.0", Optional.empty())) {
            cluster.start();
            GenericContainer<?> meta = cluster.getMetadList().get(0);
            GenericContainer<?> storage = cluster.getStoragedList().get(0);
            GenericContainer<?> graph = cluster.getGraphdList().get(0);

            useClientToCreateSpaceTag();
            int instanceIndex = 1;

            assert (meta.getContainerName().startsWith(Nebula.MetadName() + instanceIndex));
            assert (storage.getContainerName().startsWith(Nebula.StoragedName() + instanceIndex));
            assert (graph.getContainerName().startsWith(Nebula.GraphdName() + instanceIndex));

            assert cluster.getGraphdPortList().get(0) == Nebula.GraphdExposedPort();
            assert cluster.getMetadPortList().get(0) == Nebula.MetadExposedPort();
            assert cluster.getStoragedPortList().get(0) == Nebula.StoragedExposedPort();

            assert cluster.storagedList().head().getDependencies().size() == 1;
            assert cluster.metadList().head().getDependencies().isEmpty();
            assert cluster.graphdList().head().getDependencies().size() == 1;

            if (cluster.existsRunningContainer()) {
                cluster.stop();
            }
        }

    }

    private static void useClientToCreateSpaceTag() {
        NebulaPool pool = new NebulaPool();
        Session session;
        NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
        nebulaPoolConfig.setMaxConnSize(100);
        List<HostAddress> addresses = Arrays.asList(new HostAddress("127.0.0.1", 9669));
        try {
            boolean initResult = pool.init(addresses, nebulaPoolConfig);
            if (!initResult) {
                log.error("pool init failed.");
                return;
            }

            session = pool.getSession("root", "nebula", false);
            session.execute("CREATE SPACE IF NOT EXISTS test(vid_type=fixed_string(20));" + "USE test;" + "CREATE TAG IF NOT EXISTS player(name string, age int);");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            pool.close();
        }

    }
}
