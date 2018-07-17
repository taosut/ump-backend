package vn.ssdc.vnpt.test.ipMapping;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.mapping.model.IpMapping;
import vn.ssdc.vnpt.mapping.services.IpMappingService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.List;

/**
 * Created by Lamborgini on 2/11/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestIPMappingService {
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void search() {
        IpMappingService ipMappingService = new IpMappingService(repositoryFactory);
        IpMapping ipMapping = new IpMapping();
        ipMapping.id = 1L;
        ipMapping.ipMappings = "10.84.22.0/24";
        ipMapping.label = "'description root'";
        ipMapping.labelId = "2,";
        ipMapping.startIp = "10.84.22.1";
        ipMapping.endIp = "10.84.22.254";
        ipMappingService.create(ipMapping);
        List<IpMapping> ipMappingList = ipMappingService.search("20","1","");
        Assert.assertNotNull(ipMappingList.size());
    }

    @Test
    public void count() {
        IpMappingService ipMappingService = new IpMappingService(repositoryFactory);
        IpMapping ipMapping = new IpMapping();
        ipMapping.id = 3L;
        ipMapping.ipMappings = "10.84.22.0/24";
        ipMapping.label = "'description root'";
        ipMapping.labelId = "2,";
        ipMapping.startIp = "10.84.22.1";
        ipMapping.endIp = "10.84.22.254";
        ipMappingService.create(ipMapping);
        int count = ipMappingService.count("20","1","");
        Assert.assertNotNull(count);
    }

    @Test
    public void getIPByParentTree() {
        IpMappingService ipMappingService = new IpMappingService(repositoryFactory);
        IpMapping ipMapping = new IpMapping();
        ipMapping.id = 2L;
        ipMapping.ipMappings = "10.84.22.0/24";
        ipMapping.label = "'description root'";
        ipMapping.labelId = "2,";
        ipMapping.startIp = "10.84.22.1";
        ipMapping.endIp = "10.84.22.254";
        ipMappingService.create(ipMapping);
        List<IpMapping> ipMappingList = ipMappingService.getIPByParentTree("2,");
        Assert.assertNotNull(ipMappingList.size());
    }
}
