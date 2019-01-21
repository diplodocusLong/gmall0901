package com.lianglong.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.lianglong.gmall.bean.*;
import com.lianglong.gmall.service.ListService;
import com.lianglong.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class AttrManageController {
    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("attrListPage")
    public String getAttrListPage() {
        return "attrListPage";
    }

    /***
     * 获得一级分类
     * @return
     */
    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1() {
        List<BaseCatalog1> catalog1List = manageService.getCatalog1();
        return catalog1List;
    }

    /**
     * 获得二级分类
     *
     *
     * @return
     */
    @RequestMapping("getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {

        List<BaseCatalog2> catalog2List = manageService.getCatalog2(catalog1Id);
        return catalog2List;
    }

    /***
     * 获得三级分类
     *
     * @return
     */
    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        List<BaseCatalog3> catalog3List = manageService.getCatalog3(catalog2Id);
        return catalog3List;
    }

    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        return manageService.getAttrList(catalog3Id);
    }
    @RequestMapping("attrInfoListForSku")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoListForSku(String catalog3Id){
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);

        List<BaseAttrInfo> attrList = manageService.getAttrList(catalog3Id);

        return attrList;
    }

    @PostMapping(value = "saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return "success";
    }

    @RequestMapping(value = "getAttrValueList")
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(String attrId){
        BaseAttrInfo attrInfo = manageService.getAttrInfo(attrId);
        return attrInfo.getAttrValueList();
    }


    @RequestMapping(value ="onSale",method = RequestMethod.GET)
    @ResponseBody
    public void onSale(String skuId){

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        SkuLsInfo skuLsInfo = new SkuLsInfo();


        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        listService.saveSkuInfo(skuLsInfo);
    }


}
