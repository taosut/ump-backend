package vn.ssdc.vnpt.test.label;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.List;

/**
 * Created by Lamborgini on 2/11/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestLabelService {
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void loadLabelTreeByNode() {
        LabelService labelService = new LabelService(repositoryFactory);
        Label label = new Label();
        label.id = 2L;
        label.name = "root";
        label.description = "'description root'";
        label.parentId = "0";
        labelService.create(label);
        List<Label> alarmList = labelService.loadLabelTreeByNode("'0'");
        Assert.assertNotNull(alarmList.size());
    }

    @Test
    public void checkName() {
        LabelService labelService = new LabelService(repositoryFactory);
        Label label = new Label();
        label.id = 1L;
        label.name = "root";
        label.description = "'description root'";
        label.parentId = "0";
        labelService.create(label);
        int check = labelService.checkName("root","0");
        Assert.assertNotNull(check);
    }
}
