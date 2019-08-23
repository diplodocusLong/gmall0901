package com.lianglong.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.lianglong.gmall.bean.CartInfo;
import com.lianglong.gmall.bean.SkuInfo;
import com.lianglong.gmall.cart.cartconst.CartConst;
import com.lianglong.gmall.cart.mapper.CartInfoMapper;
import com.lianglong.gmall.config.RedisUtil;
import com.lianglong.gmall.service.CartService;
import com.lianglong.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;
@Service
public class CartServiceImpl  implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Reference
    private ManageService manageService;

    @Autowired
    RedisUtil redisUtil;



    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //先查是否已有该商品
        CartInfo cartInfo = new CartInfo();

        cartInfo.setSkuId(skuId);

        cartInfo.setUserId(userId);

        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);

        if(cartInfoExist!=null){
              //构物车已有  合并
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);

            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

        }else{
            //没有 那就直接加入,持久化到mysql
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            //userid skuid 已经set到对象里了
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuNum(skuNum);
            // 插入数据库
            cartInfoMapper.insertSelective(cartInfo);
            //把全数据的cartInfo 给cartInfoExist  这样cartInfoExist 肯定有数据了
            cartInfoExist = cartInfo;


        }

        //把数据同步到redis
        Jedis jedis = redisUtil.getJedis();

        String userCartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;


        jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfoExist));

        //更新构物车时间
        String userInfoKey=CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        //查出该用户登录状态时间
        Long ttl = jedis.ttl(userCartKey);

        //购物车时间与登录状态保持时间   保持一至

        jedis.expire(userCartKey,ttl.intValue());

        jedis.close();


    }

    @Override
    public List<CartInfo> getCartList(String userId) {


        //首先在redis中查找所有当前用户的购物信息 key是公共的
        Jedis jedis = redisUtil.getJedis();

        String userCartKey=CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        List<String> cartJsonList= jedis.hvals(userCartKey);

           if(cartJsonList!=null&&cartJsonList.size()>0){
               List<CartInfo> cartInfoList=new ArrayList<>();
               for (String cartJosn : cartJsonList) {
                   CartInfo cartInfo = JSON.parseObject(cartJosn, CartInfo.class);
                   cartInfoList.add(cartInfo);
               }

                //排序  用到了lamda表达示的简化写法
               cartInfoList.sort(Comparator.comparing(CartInfo::getId));
               //拿到数据直接返回
               return cartInfoList;
           }
        //如果上边没条件没进入或没有return 继续下边代码
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId) {
        List<CartInfo> cartListDB= cartInfoMapper.selectCartListWithCurPrice(userId);

        //数据库中数据比 cookie多  外层循环,貌似是一回事

        //遍历时要判断有没有数据 避免空指针

        //判断cookie中 是否所有数据都添加了


                for (CartInfo cartInfo : cartList) {
                    String skuId = cartInfo.getSkuId();
                    boolean logFlag=true;
                    if (cartListDB != null && cartListDB.size() > 0) {
                        for (CartInfo cartInfoDB : cartListDB) {
                            if (skuId.equals(cartInfoDB.getSkuId())) {
                                // 数量相加
                                cartInfoDB.setSkuNum(cartInfo.getSkuNum() + cartInfoDB.getSkuNum());
                                // 更新数据库
                                cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                                //cookie中的数据 添加到后删除

                                //cookie和db中 sku都是单例的  已经找到匹配的  不需要再继续遍历cookie中数据了
                                //同时这么做也能避免删除list可能会出现的问题
                                logFlag=false;
                                break;
                            }

                        }

                    }

                    if(logFlag){
                        //说明 cookie中的 数据没有与数据库中同一个skuid的  需要添加到数据库
                        cartInfo.setUserId(userId);
                        cartInfoMapper.insertSelective(cartInfo);
                    }
                }

        //再查一次数据啼并更新redis
        List<CartInfo> cartInfoList = loadCartCache(userId);

        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCK : cartList) {
                // 判断条件，skuId 相等
                if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
                    // cookie 中选中状态为1
                    if ("1".equals(cartInfoCK.getIsChecked())){
                        // 将数据库中的商品变为1
                        cartInfoDB.setIsChecked(cartInfoCK.getIsChecked());
                        // 将redis 中被选中的商品进行选中！
                        checkCart(cartInfoCK.getSkuId(),cartInfoCK.getIsChecked(),userId);
                    }
                }
            }
        }



        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        //用户可能会频繁操作  存在redis中就可以

        Jedis jedis = redisUtil.getJedis();

        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        String cartInfoJson = jedis.hget(userCartKey, skuId);

        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);

        cartInfo.setIsChecked(isChecked);

        String jsonString = JSON.toJSONString(cartInfo);
        jedis.hset(userCartKey,skuId,jsonString );
        //新增到已选中的购物车
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

        if("1".equals(isChecked)){
            jedis.hset(userCheckedKey,skuId,jsonString);
        }else{
            jedis.hdel(userCheckedKey,skuId);
        }

        jedis.close();
    }

    private List<CartInfo> loadCartCache(String userId) {
        //先查出来数据

        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        //把数据存到redis
        if (cartInfoList==null && cartInfoList.size()==0){
            return null;
        }

        String userCartKey=CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

            Jedis jedis = redisUtil.getJedis();
        Map<String,String> map = new HashMap<>(cartInfoList.size());
            for (CartInfo info : cartInfoList) {
                String infoJson = JSON.toJSONString(info);
                // key 都是同一个，值会产生重复覆盖！
                map.put(info.getSkuId(),infoJson);

            }

        //将数据保存到redis中
        jedis.hmset(userCartKey,map);

        String userInfoKey=CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        //查出该用户登录状态时间
        Long ttl = jedis.ttl(userInfoKey);

        //购物车时间与登录状态保持时间   保持一至

        jedis.expire(userCartKey,ttl.intValue());

        jedis.close();

        return  cartInfoList;

    }

    // 得到选中购物车列表
    public  List<CartInfo> getCartCheckedList(String userId){
        // 获得redis中的key
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        List<String> cartCheckedList = jedis.hvals(userCheckedKey);
        List<CartInfo> newCartList = new ArrayList<>();
        for (String cartJson : cartCheckedList) {
            CartInfo cartInfo = JSON.parseObject(cartJson,CartInfo.class);
            newCartList.add(cartInfo);
        }
        return newCartList;
    }


}
