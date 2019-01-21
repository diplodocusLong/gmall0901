package com.lianglong.gmall.manage.mapper;

import com.lianglong.gmall.bean.BaseAttrInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    public List<BaseAttrInfo> selectAttrInfoList(long catalog3Id);
}

