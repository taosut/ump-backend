package vn.ssdc.vnpt.common.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.common.model.Configuration;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;


@Service
public class ConfigurationService extends SsdcCrudService<String, Configuration> {
    @Autowired
    public ConfigurationService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Configuration.class);
    }
}
