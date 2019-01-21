package com.lianglong.gmall.manage.mapper;

import com.lianglong.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
  List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}
