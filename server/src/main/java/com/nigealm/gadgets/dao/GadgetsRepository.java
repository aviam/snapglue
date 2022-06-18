package com.nigealm.gadgets.dao;

import org.springframework.data.repository.CrudRepository;

import com.nigealm.gadgets.vo.GadgetVO;

public interface GadgetsRepository extends CrudRepository<GadgetVO, Long>
{

}
