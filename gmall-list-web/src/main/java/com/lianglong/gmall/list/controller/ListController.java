package com.lianglong.gmall.list.controller;
import com.alibaba.dubbo.config.annotation.Reference;
import com.lianglong.gmall.bean.BaseAttrInfo;
import com.lianglong.gmall.bean.BaseAttrValue;
import com.lianglong.gmall.bean.SkuLsParams;
import com.lianglong.gmall.bean.SkuLsResult;
import com.lianglong.gmall.service.ListService;
import com.lianglong.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    private ListService listService;
    @Reference
    private ManageService manageService;
    @RequestMapping("list.html")
    public String getList(SkuLsParams skuLsParams, Model model){
        // 设置每页显示的条数
        skuLsParams.setPageSize(2);
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        //sku集合给页面
        model.addAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());

        //通过属性值集合查出所有  属性    属性值
        List<String> ids= skuLsResult.getAttrValueIdList();

        List<BaseAttrInfo> attrList = manageService.getAttrList(ids);
        //用来做面包绡
        List<BaseAttrValue> baseAttrValuesList = new ArrayList<>();
        //过滤属性是否已经存在
        // itco
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {

            BaseAttrInfo baseAttrInfo =  iterator.next();

            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

            for (BaseAttrValue baseAttrValue : attrValueList) {

                if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){

                    for (String valueId : skuLsParams.getValueId()) {

                        //选中的属性值 和 查询结果的属性值
                        if(valueId.equals(baseAttrValue.getId())){

                            iterator.remove();

                            BaseAttrValue baseAttrValueSelected = new BaseAttrValue();

                            baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());

                            // 去除重复数据
                            String urlParams = makeUrlParam(skuLsParams, valueId);

                            baseAttrValueSelected.setUrlParam(urlParams);

                            baseAttrValuesList.add(baseAttrValueSelected);

                        }
                    }
                }
            }
        }

        model.addAttribute("baseAttrValuesList",baseAttrValuesList);

        model.addAttribute("keyword",   skuLsParams.getKeyword());

        model.addAttribute("attrList",attrList);
        //填加urlparam信息
        String urlParam = makeUrlParam(skuLsParams);

        model.addAttribute("urlParam",urlParam);

        int totalPages = (int) ((skuLsResult.getTotal() + skuLsParams.getPageSize()-1)/skuLsParams.getPageSize());
// skuLsResult.setTotalPages(totalPages);
        model.addAttribute("totalPages",totalPages);

        model.addAttribute("pageNo",skuLsParams.getPageNo());

        return "list";
    }
    //在controller中添加方法拼接条件方法
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds){
          String urlParam="";

        String keyword = skuLsParams.getKeyword();

        String catalog3Id = skuLsParams.getCatalog3Id();

        String[] valueIds = skuLsParams.getValueId();

        if(keyword!=null&&keyword.length()>0){
            urlParam+="keyword="+keyword;
        }
        if(catalog3Id!=null&&catalog3Id.length()>0){
            if(urlParam.length()!=0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+catalog3Id;
        }
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环拼接
            for (String valueId : skuLsParams.getValueId()) {
                // 可变长数组不能为空！
                if (excludeValueIds!=null && excludeValueIds.length>0){
                    // 获取平台属性值Id
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)){
                        continue;
                    }
                }

                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }

        return urlParam;

    }




}
