/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services.kpifunctions;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author kiendt
 */
public class ExpressionLogic implements Expression {

    public Object leftExpression;
    public Object rightExpression;
    public String operator;
    public String expression;

    public HandleValueFunction handleValue;

    public ExpressionLogic() {
    }

    public ExpressionLogic(Object leftExpression, Object rightExpression, String operator) {
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "ExpressionLogic{" + "leftExpression=" + leftExpression + ", rightExpression=" + rightExpression + ", operator=" + operator + '}';
    }

    @Override
    public boolean toResult() {
        boolean result = false;
        if (operator.equals("AND")) {
            if (leftExpression instanceof ExpressionLogic && rightExpression instanceof ExpressionLogic) {
                ExpressionLogic left = (ExpressionLogic) leftExpression;
                ExpressionLogic right = (ExpressionLogic) rightExpression;
                left.handleValue = handleValue;
                right.handleValue = handleValue;
                if (left.toResult() && right.toResult()) {
                    result = true;
                } else {
                    result = false;
                }
            }
            if (leftExpression instanceof ExpressionComparator && rightExpression instanceof ExpressionComparator) {
                ExpressionComparator left = (ExpressionComparator) leftExpression;
                ExpressionComparator right = (ExpressionComparator) rightExpression;
                left.handleValue = handleValue;
                right.handleValue = handleValue;
                if (left.toResult() && right.toResult()) {
                    result = true;
                } else {
                    result = false;
                }
            }
        } else if (operator.equals("OR")) {
            if (leftExpression instanceof ExpressionLogic && rightExpression instanceof ExpressionLogic) {
                ExpressionLogic left = (ExpressionLogic) leftExpression;
                ExpressionLogic right = (ExpressionLogic) rightExpression;
                left.handleValue = handleValue;
                right.handleValue = handleValue;
                if (left.toResult() || right.toResult()) {
                    result = true;
                } else {
                    result = false;
                }
            }
            if (leftExpression instanceof ExpressionComparator && rightExpression instanceof ExpressionComparator) {
                ExpressionComparator left = (ExpressionComparator) leftExpression;
                ExpressionComparator right = (ExpressionComparator) rightExpression;
                left.handleValue = handleValue;
                right.handleValue = handleValue;
                if (left.toResult() || right.toResult()) {
                    result = true;
                } else {
                    result = false;
                }
            }
        }
        return result;
    }

    public void parse() {
        Stack st = new Stack();
//        ExpressionLogic exp = new ExpressionLogic();
        String left = null, operator = null, right = null;
        // handle left
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == '(') {
                st.push(i);
            }
            if (expression.charAt(i) == ')') {
                int fristBrace = (int) st.pop();
                if (st.isEmpty()) {
                    if (i + 1 < expression.length()) {
                        if ((expression.charAt(i + 1) == 'A' || expression.charAt(i + 1) == 'O')) {
                            left = expression.substring(fristBrace + 1, i);
                            if (validate(left)) {
                                // is express logic
                                ExpressionLogic expressLogic = new ExpressionLogic();
                                expressLogic.expression = left;
                                expressLogic.parse();
                                this.leftExpression = expressLogic;
                            } else {
                                // is compare logic
                                ExpressionComparator expressCompare = new ExpressionComparator();
                                expressCompare.expression = left;
                                expressCompare.parse();
                                this.leftExpression = expressCompare;
                            }
                            break;
                        }
                    }

                }
            }
        }

        if (left != null) {
            expression = expression.substring(expression.indexOf(left) + left.length() + 1, expression.length());
            if (expression.indexOf("OR") != -1) {
                operator = "OR";
            } else if (expression.indexOf("AND") != -1) {
                operator = "AND";
            }
            this.operator = operator;
            expression = expression.substring(expression.indexOf(operator) + operator.length(), expression.length());

            for (int i = 0; i < expression.length(); i++) {
                if (expression.charAt(i) == '(') {
                    st.push(i);
                }
                if (expression.charAt(i) == ')') {
                    int fristBrace = (int) st.pop();
                    if (st.isEmpty()) {
                        right = expression.substring(fristBrace + 1, i);
                        if (validate(right)) {
                            // is express logic
                            ExpressionLogic expressLogic = new ExpressionLogic();
                            expressLogic.expression = right;
                            expressLogic.parse();
                            this.rightExpression = expressLogic;
                        } else {
                            // is compare logic
                            ExpressionComparator expressCompare = new ExpressionComparator();
                            expressCompare.expression = right;
                            expressCompare.parse();
                            this.rightExpression = expressCompare;
                        }
                        break;

                    }
                }
            }
        }
    }

    @Override
    public boolean validate(String input) {
        List<String> validateData = Arrays.asList(new String[]{"AND", "OR"});
        for (String tmp : validateData) {
            if (input.contains(tmp)) {
                return true;
            }
        }
        return false;
    }

}
