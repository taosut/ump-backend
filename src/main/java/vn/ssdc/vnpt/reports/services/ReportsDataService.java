package vn.ssdc.vnpt.reports.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.reports.model.ReportsData;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

@Service
public class ReportsDataService extends SsdcCrudService<Long, ReportsData> {

    @Autowired
    public ReportsDataService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(ReportsData.class);
    }

    public void createDeviceReports(ReportsData reportsData) {
            this.create(reportsData);
    }
}
