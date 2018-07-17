package vn.ssdc.vnpt.selfCare.model;

import java.util.Set;
import vn.ssdc.vnpt.mapping.model.AccountMapping;
import vn.ssdc.vnpt.mapping.model.IpMapping;
import vn.ssdc.vnpt.label.model.Label;

/**
 * Created by THANHLX on 11/30/2017.
 */
public class SCLabel {

    public Long id;
    public String name;
    public Long parentId;

    public String description;
    public String parentName;
    public Set<IpMapping> ipMapping;
    public Long deviceGroupId;

    public Set<AccountMapping> accountMapping;

    public SCLabel(Label label) {
        this.id = label.id;
        this.name = label.name;
        this.parentId = Long.valueOf(label.parentId);
    }

    public SCLabel() {
    }
}
