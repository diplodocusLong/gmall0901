package com.lianglong.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.lianglong.gmall.bean.SkuInfo;
import com.lianglong.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SkuManageController {
    @Reference
    ManageService managerService;
     @RequestMapping("saveSku")
     @ResponseBody
     public String saveSkuInfo(SkuInfo skuInfo){
         managerService.saveSkuInfo(skuInfo);
         return  "ok";
     }

     @RequestMapping("skuInfoListBySpu")
    @ResponseBody
    public List<SkuInfo> getSkuInfoListBySpu(String spuId){

         return managerService.getSkuInfoListBySpu(spuId);
     }
}
