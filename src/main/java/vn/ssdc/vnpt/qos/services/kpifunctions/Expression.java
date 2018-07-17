/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services.kpifunctions;

/**
 *
 * @author kiendt
 */
public interface Expression {

    boolean toResult();

    boolean validate(String input);

}
