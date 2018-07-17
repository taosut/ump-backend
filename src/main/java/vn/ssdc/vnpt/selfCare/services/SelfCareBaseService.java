/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import java.util.List;

/**
 *
 * @author kiendt
 */
public abstract class SelfCareBaseService<T, SearchForm> {

    public abstract List<T> search(SearchForm searchForm);

    public abstract int count(SearchForm searchForm);
}
