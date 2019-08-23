package com.lianglong.gmall.service;

import com.lianglong.gmall.bean.SkuLsInfo;
import com.lianglong.gmall.bean.SkuLsParams;
import com.lianglong.gmall.bean.SkuLsResult;

public interface ListService {

    public void saveSkuInfo(SkuLsInfo skuLsInfo);

    public SkuLsResult search(SkuLsParams skuLsParams);

    public void incrHotScore(String skuId);
}
