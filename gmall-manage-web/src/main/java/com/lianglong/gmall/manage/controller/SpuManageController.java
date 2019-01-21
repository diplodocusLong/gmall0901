package com.lianglong.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.lianglong.gmall.bean.*;
import com.lianglong.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SpuManageController {
    @Reference
    ManageService manageService;
    @RequestMapping("spuListPage")
    public String getSpuListPage(){
        return "spuListPage";
    }


    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> getSpuList(String catalog3Id){

        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
     return    manageService.getSpuInfoList(spuInfo);
    }


    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> getSpuImageList(String spuId){
        return   manageService.getSpuImageList(spuId);
    }


    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId){

        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);

        for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
            List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
            Map map=new HashMap();
            map.put("total",spuSaleAttrValueList.size());
            map.put("rows",spuSaleAttrValueList);
             String spuSaleAttrValueJson = JSON.toJSONString(map);
            spuSaleAttr.setSpuSaleAttrValueJson(map);
        }


        return spuSaleAttrList;

    }

    @RequestMapping("spuSaleAttrValueList")
    @ResponseBody
    public List<SpuSaleAttrValue> getSpuSaleAttrValueList(HttpServletRequest httpServletRequest){
        String spuId = httpServletRequest.getParameter("spuId");
        String saleAttrId = httpServletRequest.getParameter("saleAttrId");
        SpuSaleAttrValue spuSaleAttrValue=new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuId);
        spuSaleAttrValue.setSaleAttrId(saleAttrId);
        List<SpuSaleAttrValue> spuSaleAttrValueList = manageService.getSpuSaleAttrValueList(spuSaleAttrValue);
        return spuSaleAttrValueList;

    }
    @RequestMapping(value = "saveSpuInfo",method = RequestMethod.POST)
    @ResponseBody
    public String saveSpuInfo(SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return  "success";
    }

    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr>  getBaseSaleAttr(){
        List<BaseSaleAttr> baseSaleAttr = manageService.getBaseSaleAttrList();
        return baseSaleAttr;

    }

}


