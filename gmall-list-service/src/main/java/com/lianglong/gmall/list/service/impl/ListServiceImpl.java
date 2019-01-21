package com.lianglong.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.lianglong.gmall.bean.SkuLsInfo;
import com.lianglong.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";



    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        //保存数据到es
        //上架 将最新的产品新增到es上
        //下架 将商品从es中删除
        Index index=new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            jestClient.execute(index);//执行保存
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
