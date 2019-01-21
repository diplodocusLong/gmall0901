package com.lianglong.gmall.manage.mapper;

import com.lianglong.gmall.bean.BaseAttrValue;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrValueMapper extends Mapper<BaseAttrValue> {
   @Select("select * from base_attr_value where attr_id=#{attrId}")
   public List<BaseAttrValue> selectValueListByAttrId(String attrId);
}
