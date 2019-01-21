package com.lianglong.gmall.service;

import com.lianglong.gmall.bean.*;

import java.util.List;

public interface ManageService {
    /**
     * 查询所有一级分类
     * @return
     */
    public List<BaseCatalog1> getCatalog1();
    /**
     * 根据一级分类Id 查询二级分类
     * @param catalog1Id
     * @return
     */
    public List<BaseCatalog2> getCatalog2(String catalog1Id);
    /**
     * 根据二级分类Id 查询三级分类
     * @param catalog2Id
     * @return
     */
    public List<BaseCatalog3> getCatalog3(String catalog2Id);
    /**
     * 快速跳转到实现类 ctrl+alt+b
     * 根据三级分类Id 查询平台属性集合
     * @param catalog3Id
     * @return
     */
    public List<BaseAttrInfo> getAttrList(String catalog3Id);
    /**
     * 保存平台属性--平台属性值
     * @param baseAttrInfo
     */
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo);
    /**
     * 根据attrId 查询baseAttrInfo
     * @param attrId
     * @return
     */
    public BaseAttrInfo   getAttrInfo(String attrId);
    /**
     *  主要利用三级分类Id 查询商品集合
     * @param spuInfo
     * @return
     */
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);
    /**
     * 根据attrId 查询该集合List<BaseAttrValue>
     * @param attrId
     * @return
     */
    public List<BaseSaleAttr> getBaseSaleAttrList();
    /**
     * 保存spuInfo
     * @param spuInfo
     */
    public void saveSpuInfo(SpuInfo spuInfo);
    /**
     * 根据spuId 查询spuImage
     * @param spuId
     * @return
     */
    public List<SpuImage> getSpuImageList(String spuId);
    /**
     * 根据spuId查询销售属性集合
     * @param spuId
     * @return
     */
    public  List<SpuSaleAttr> getSpuSaleAttrList(String spuId);
     /**
      *
      * 查巡spu属性结果集
      */
    public  List<SpuSaleAttrValue> getSpuSaleAttrValueList(SpuSaleAttrValue spuSaleAttrValue);

    /**
     *  保存商品数据
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    public List<SkuInfo> getSkuInfoListBySpu(String spuId);

    /**
     * 查巡skuinfo
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 跟據 skuInfo中的 skuid  spuId查出 sku所有可選擇的 spu銷售屬性 有對應sku的 isChecked=1,否則是0
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo);


    /**
     * 查出spu下 sku  销售属性组合与 skuid的id  为组合关系做数据准备
     * @param spuId
     * @return
     */
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);
}
