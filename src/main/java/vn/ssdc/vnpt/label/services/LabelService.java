package vn.ssdc.vnpt.label.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.label.model.Label;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class LabelService extends SsdcCrudService<Long, Label> {

    private static final Logger logger = LoggerFactory.getLogger(LabelService.class);

    @Autowired
    public LabelService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Label.class);
    }

    public List<Label> loadLabelTreeByNode(String parentId) {
        return this.repository.search(" parent_id in ( " + parentId + ")");
    }

    public int checkName(String nameLabel, String parentLabel) {
        String whereExp = "name=? and parent_id=?";
        return this.repository.search(whereExp, nameLabel, parentLabel).size();
    }

    public boolean isParent(Long labelId) {
        String whereExp = "parent_id=?";
        List<Label> labels = this.repository.search(whereExp, labelId);
        return labels == null || labels.isEmpty() ? false : true;
    }

    public Label findUngroupLabel() {
        String whereExp = "name=?";
        List<Label> labels = this.repository.search(whereExp, "Ungroup");
        if (!labels.isEmpty()) {
            return labels.get(0);
        }
        return null;
    }

}
