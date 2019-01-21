package com.lianglong.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.lianglong.gmall.bean.SkuInfo;
import com.lianglong.gmall.bean.SkuSaleAttrValue;
import com.lianglong.gmall.bean.SpuSaleAttr;
import com.lianglong.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {
    @Reference
    private ManageService manageService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable("skuId") String skuId, Map<String, Object> map) {

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        map.put("skuInfo", skuInfo);

        List<SpuSaleAttr> saleAttrList = manageService.selectSpuSaleAttrListCheckBySku(skuInfo);
        map.put("saleAttrList", saleAttrList);
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        String valueIdsKey="";
        Map<String,String> valuesSkuMap=new HashMap<>();
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            if (valueIdsKey.length() != 0) {
                valueIdsKey += "|";
            }
            valueIdsKey += skuSaleAttrValue.getSaleAttrValueId();
            if ((i + 1) == skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i + 1).getSkuId())) {

                valuesSkuMap.put(valueIdsKey, skuSaleAttrValue.getSkuId());
                valueIdsKey = "";
            }
        }
        String s = JSON.toJSONString(valuesSkuMap);
        map.put("valuesSkuJson",s);
        return "item";
    }
}
