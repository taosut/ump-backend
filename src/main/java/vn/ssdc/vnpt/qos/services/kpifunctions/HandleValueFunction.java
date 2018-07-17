/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services.kpifunctions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author kiendt
 */
public class HandleValueFunction extends BaseHandlerKpi {

    public static final String TEMPLATE = "=VALUE[(](.*?)[)]";

    public HandleValueFunction(KpiInput kpiInput) {
        super(TEMPLATE, kpiInput);
    }

    @Override
    protected Object generateValue() {
        Pattern pattern = Pattern.compile(TEMPLATE);
        Matcher matcher = pattern.matcher(kpiInput.kpiFunction);
        if (matcher.find()) {
            String parameter = matcher.group(1);
            return Float.valueOf(kpiInput.kpiData.get(parameter)).longValue();
        }
        return null;
    }

}
