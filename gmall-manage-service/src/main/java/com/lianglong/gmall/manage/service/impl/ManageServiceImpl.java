package com.lianglong.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.lianglong.gmall.bean.*;
import com.lianglong.gmall.config.RedisUtil;
import com.lianglong.gmall.manage.constant.ManageConst;
import com.lianglong.gmall.manage.mapper.*;
import com.lianglong.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Override
    public List<BaseCatalog1> getCatalog1() {


        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectAttrInfoList(Long.parseLong(catalog3Id));

        return baseAttrInfoList;

    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        if(baseAttrInfo.getId()!=""&&baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        }else{
            baseAttrInfo.setId(null);
            baseAttrInfoMapper.insert(baseAttrInfo);

        }
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);

        if(baseAttrInfo.getAttrValueList()!=null&&baseAttrInfo.getAttrValueList().size()>0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                //防止主键被赋上一个空字符串
                if(attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }




    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        //属性对象
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        //枸建属性值List 封装到属性对象里

        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
        baseAttrInfo.setAttrValueList(baseAttrValueList);

        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存主表 通过主键存在判断是修改 还是新增
        if(spuInfo.getId()==null||spuInfo.getId().length()==0){
            //确保主键为空
            spuInfo.setId(null);
            spuInfoMapper.insertSelective(spuInfo);
        }else{
            spuInfoMapper.updateByPrimaryKey(spuInfo);
        }

        //保存图片信息 先删除 再插入
        Example spuImageExample=new Example(SpuImage.class);
        spuImageExample.createCriteria().andEqualTo("spuId",spuInfo.getId());
        spuImageMapper.deleteByExample(spuImageExample);

        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(spuImageList!=null&&spuImageList.size()>0) {
            for (SpuImage spuImage : spuImageList) {
                if(spuImage.getId()!=null&&spuImage.getId().length()==0){
                    spuImage.setId(null);
                }
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }

        //保存销售属性信息  先删除 再插入
        Example spuSaleAttrExample=new Example(SpuSaleAttr.class);
        spuSaleAttrExample.createCriteria().andEqualTo("spuId",spuInfo.getId());
        spuSaleAttrMapper.deleteByExample(spuSaleAttrExample);

        //保存销售属性值信息  先删除 再插入
        Example spuSaleAttrValueExample=new Example(SpuSaleAttrValue.class);
        spuSaleAttrValueExample.createCriteria().andEqualTo("spuId",spuInfo.getId());
        spuSaleAttrValueMapper.deleteByExample(spuSaleAttrValueExample);

        //保存销售属性和属性值信息
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(spuSaleAttrList!=null&&spuSaleAttrList.size()>0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                if(spuSaleAttr.getId()!=null&&spuSaleAttr.getId().length()==0){
                    spuSaleAttr.setId(null);
                }
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                    if(spuSaleAttrValue.getId()!=null&&spuSaleAttrValue.getId().length()==0){
                        spuSaleAttrValue.setId(null);
                    }
                    spuSaleAttrValue.setSpuId(spuInfo.getId());
                    spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                }
            }

        }


    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(Long.parseLong(spuId));
        return spuSaleAttrList;

    }

    @Override
    public List<SpuSaleAttrValue> getSpuSaleAttrValueList(SpuSaleAttrValue spuSaleAttrValue) {
        List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttrValueMapper.select(spuSaleAttrValue);

        return spuSaleAttrValueList;
    }
    /**
      保存商品信息

     */
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
           if(skuInfo.getId()==null||skuInfo.getId().length()==0){
               skuInfo.setId(null);
               skuInfoMapper.insert(skuInfo);
           }else{

               skuInfoMapper.updateByPrimaryKey(skuInfo);
           }

           //先删除skuimage信息
           Example skuImageExample = new Example(SkuImage.class);

           skuImageExample.createCriteria().andEqualTo("skuId",skuInfo.getId());

           skuImageMapper.deleteByExample(skuImageExample);
        //保存商品 图片信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();

        for (SkuImage skuImage : skuImageList) {

            skuImage.setSkuId(skuInfo.getId());
            //排除空串id
            if(skuImage.getId()!=null&&skuImage.getId().length()==0) {
                skuImage.setId(null);
            }
            skuImageMapper.insertSelective(skuImage);

        }

        //删除sku_attr_value 表中 指定skuid的相关信息

        Example skuAttrValueExample = new Example(SkuAttrValue.class);
        skuAttrValueExample.createCriteria().andEqualTo("skuId",skuInfo.getId());
        skuAttrValueMapper.deleteByExample(skuAttrValueExample);

        //保存sku_attr_value信息
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                //排除空串
              skuAttrValue.setSkuId(skuInfo.getId());
              if(skuAttrValue.getId()!=null&&skuAttrValue.getId().length()==0){
                  skuAttrValue.setId(null);
              }
              skuAttrValueMapper.insertSelective(skuAttrValue);

        }

        //删除sku_sale_attr_value中 skuid相关数据
        Example skuSaleAttrValueExample = new Example(SkuSaleAttrValue.class);

        skuSaleAttrValueExample.createCriteria().andEqualTo("skuId",skuInfo.getId());

        skuSaleAttrValueMapper.deleteByExample(skuSaleAttrValueExample);

        //保存数据到sku_sale_attr_value

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();

        for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {

            saleAttrValue.setSkuId(skuInfo.getId());
            if(saleAttrValue.getId()!=null&&saleAttrValue.getId().length()==0){
                saleAttrValue.setId(null);
            }
            skuSaleAttrValueMapper.insertSelective(saleAttrValue);

        }

    }

    @Override
    public List<SkuInfo> getSkuInfoListBySpu(String spuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);
        return skuInfoMapper.select(skuInfo);
    }

/*    @Override
    public SkuInfo getSkuInfo(String skuId) {
        SkuInfo skuInfo=null;

        String skuInfoKey= ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if(jedis.exists(skuInfoKey)){
                String s = jedis.get(skuInfoKey);
                if (s!=null && s.length()!=0){
                    skuInfo = JSON.parseObject(s, SkuInfo.class);
                    return skuInfo;
                }


            }else{
               skuInfo =  getSkuInfoDB(skuId);
                 if(skuInfo!=null){
                     String s = JSON.toJSONString(skuInfo);
                     jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,s);
                 }

            }
            jedis.close();
        } catch (Exception e) {
            e.printStackTrace();
            skuInfo=getSkuInfoDB(skuId);
            return skuInfo;
        }


        return skuInfo;


    }*/

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        SkuInfo skuInfo=null;
        String skuInfoKey= ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
        } catch (Exception e) {
            e.printStackTrace();
            return getSkuInfoDB(skuId);
        }
        String s = jedis.get(skuInfoKey);
        if(s==null||s.length()==0){
            //说明当前查巡没有命中缓存,上锁,查数据库
            //生成锁
            String skuInfoLock=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
            String lockKey  = jedis.set(skuInfoLock, "OK", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
            if("OK".equals(lockKey)){
                //说明或得锁
                skuInfo= getSkuInfoDB(skuId);
                if(skuInfo!=null){
                    String skuInfoValue = JSON.toJSONString(skuInfo);
                    jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,skuInfoValue);
                }
                jedis.close();
                return skuInfo;
            }else{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 自旋
                return getSkuInfo(skuId);
            }
        }else{
             skuInfo = JSON.parseObject(s, SkuInfo.class);
              jedis.close();
              return skuInfo;
        }
    }
    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        SkuImage skuImage = new SkuImage();

        skuImage.setSkuId(skuId);

        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);

        skuInfo.setSkuImageList(skuImageList);

        SkuAttrValue skuAttrValue = new SkuAttrValue();

        skuAttrValue.setSkuId(skuId);

        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);

        skuInfo.setSkuAttrValueList(skuAttrValueList);

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {

        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());

        return spuSaleAttrList;
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);

        return skuSaleAttrValueList;

    }
}
