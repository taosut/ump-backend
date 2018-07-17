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
public class HandleIfFunction extends BaseHandlerKpi {

    public static final String TEMPLATE = "=IF[(](.*?)[;](.*?)[;](.*?)[)]";

    public HandleIfFunction(KpiInput kpiInput) {
        super(TEMPLATE, kpiInput);
    }

    @Override
    protected Object generateValue() {
        Pattern pattern = Pattern.compile(TEMPLATE);
        Matcher matcher = pattern.matcher(kpiInput.kpiFunction);
        if (matcher.find()) {
            String condition = matcher.group(1);
            String resultIfOk = matcher.group(2);
            String resultIfNOk = matcher.group(3);
            Object expressionType = getTypeExpress(condition);
            System.out.println("condition:" + condition);
            System.out.println("resultIfOk:" + resultIfOk);
            System.out.println("resultIfNOk:" + resultIfNOk);
            if (expressionType instanceof ExpressionComparator) {
                ExpressionComparator expressionComparator = (ExpressionComparator) expressionType;
                expressionComparator.expression = condition;
                expressionComparator.handleValue = new HandleValueFunction(kpiInput);
                expressionComparator.parse();
                if (expressionComparator.toResult()) {
                    return resultIfOk;
                } else {
                    return resultIfNOk;
                }
            } else if (expressionType instanceof ExpressionLogic) {
                ExpressionLogic expressionLogic = (ExpressionLogic) expressionType;
                expressionLogic.expression = condition;
                expressionLogic.handleValue = new HandleValueFunction(kpiInput);
                expressionLogic.parse();
                if (expressionLogic.toResult()) {
                    return resultIfOk;
                } else {
                    return resultIfNOk;
                }
            }
        }
        return null;
    }

    public Object getTypeExpress(String expression) {
        ExpressionLogic expLogic = new ExpressionLogic();
        expLogic.expression = expression;
        ExpressionComparator expCompartor = new ExpressionComparator();
        expCompartor.expression = expression;
        if (expLogic.validate(expression)) {
            return expLogic;
        }
        if (expCompartor.validate(expression)) {
            return expCompartor;
        }
        return null;
    }

}
