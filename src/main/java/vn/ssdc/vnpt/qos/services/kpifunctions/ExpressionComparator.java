/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services.kpifunctions;

/**
 *
 *
 * @author kiendt
 */
public class ExpressionComparator implements Expression {

    public String leftExpression;
    public String rightExpression;
    public String operator;
    public String expression;

    public HandleValueFunction handleValue;

    public ExpressionComparator(String leftExpression, String rightExpression, String operator) {
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
        this.operator = operator;
    }

    public ExpressionComparator() {
    }

    /**
     * Hien tại chi support phep toan so sanh dang x ><= y
     */
    public void parse() {
        for (int i = 0; i < this.expression.length(); i++) {
            if (this.expression.charAt(i) == '<' || this.expression.charAt(i) == '>' || this.expression.charAt(i) == '=') {
                this.leftExpression = this.expression.substring(0, i);
            }
        }

        if (this.expression.indexOf(">") != -1) {
            this.operator = ">";
        } else if (this.expression.indexOf("<") != -1) {
            this.operator = "<";
        } else if (this.expression.indexOf("=") != -1) {
            this.operator = "=";
        } else if (this.expression.indexOf(">=") != -1) {
            this.operator = ">=";
        } else if (this.expression.indexOf(">=") != -1) {
            this.operator = ">=";
        }
        int x = this.expression.indexOf(this.operator) + this.operator.length();
        this.rightExpression = this.expression.substring(this.expression.indexOf(this.operator) + this.operator.length(), this.expression.length());
    }

    private boolean compareStringNumberic(String s1, String s2, String operator) {
        if (operator.equals(">")) {
            try {
                if (Integer.parseInt(s1) > Integer.parseInt(s2)) {
                    return true;
                }
            } catch (NumberFormatException e) {
                try {
                    if (Float.parseFloat(s1) > Float.parseFloat(s2)) {
                        return true;
                    }
                } catch (NumberFormatException e1) {
                    if (Long.parseLong(s1) > Long.parseLong(s2)) {
                        return true;
                    }
                }
            }

        } else if (operator.equals("<")) {
            try {
                if (Integer.parseInt(s1) < Integer.parseInt(s2)) {
                    return true;
                }
            } catch (NumberFormatException e) {
                try {
                    if (Float.parseFloat(s1) < Float.parseFloat(s2)) {
                        return true;
                    }
                } catch (NumberFormatException e1) {
                    if (Long.parseLong(s1) < Long.parseLong(s2)) {
                        return true;
                    }
                }
            }

        } else if (operator.equals(">=")) {
            try {
                if (Integer.parseInt(s1) >= Integer.parseInt(s2)) {
                    return true;
                }
            } catch (NumberFormatException e) {
                try {
                    if (Float.parseFloat(s1) >= Float.parseFloat(s2)) {
                        return true;
                    }
                } catch (NumberFormatException e1) {
                    if (Long.parseLong(s1) >= Long.parseLong(s2)) {
                        return true;
                    }
                }
            }

        } else if (operator.equals("<=")) {
            try {
                if (Integer.parseInt(s1) <= Integer.parseInt(s2)) {
                    return true;
                }
            } catch (NumberFormatException e) {
                try {
                    if (Float.parseFloat(s1) <= Float.parseFloat(s2)) {
                        return true;
                    }
                } catch (NumberFormatException e1) {
                    if (Long.parseLong(s1) <= Long.parseLong(s2)) {
                        return true;
                    }
                }
            }

        } else if (operator.equals("=")) {
            try {
                if (Integer.parseInt(s1) == Integer.parseInt(s2)) {
                    return true;
                }
            } catch (NumberFormatException e) {
                try {
                    if (Float.parseFloat(s1) == Float.parseFloat(s2)) {
                        return true;
                    }
                } catch (NumberFormatException e1) {
                    if (Long.parseLong(s1) == Long.parseLong(s2)) {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    @Override
    public boolean toResult() {
        String leftValue = handleValue.kpiInput.kpiData.get(leftExpression) == null ? leftExpression : String.valueOf(Float.valueOf(handleValue.kpiInput.kpiData.get(leftExpression)).longValue());
        String rightValue = handleValue.kpiInput.kpiData.get(rightExpression) == null ? rightExpression : String.valueOf(Float.valueOf(handleValue.kpiInput.kpiData.get(rightExpression)).longValue());
        return compareStringNumberic(leftValue, rightValue, operator);
    }

    @Override
    public boolean validate(String input) {
        if (input.contains("AND") || input.contains("OR")) {
            return false;
        }
        return true;
    }

}
