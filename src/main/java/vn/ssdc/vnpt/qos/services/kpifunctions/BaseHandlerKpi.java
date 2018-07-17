/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services.kpifunctions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static jdk.nashorn.tools.ShellFunctions.INPUT;

/**
 *
 * @author kiendt
 */
public abstract class BaseHandlerKpi {

    protected String template;
    protected KpiInput kpiInput;

    public BaseHandlerKpi(String template, KpiInput kpiInput) {
        this.template = template;
        this.kpiInput = kpiInput;
    }

    public boolean validateTemplate() {
        // do validate template
        Pattern p = Pattern.compile(template);
        Matcher m = p.matcher(kpiInput.kpiFunction);   // get a matcher object
        return m.matches();
    }

    public Object process() throws Exception {
        if (!validateTemplate()) {
            throw new Exception("error_validate_not_match");
        }
        return generateValue();
    }

    abstract protected Object generateValue();
}
