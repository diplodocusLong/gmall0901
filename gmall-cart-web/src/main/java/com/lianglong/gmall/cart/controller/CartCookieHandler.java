package com.lianglong.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.lianglong.gmall.bean.CartInfo;
import com.lianglong.gmall.bean.SkuInfo;
import com.lianglong.gmall.config.CookieUtil;
import com.lianglong.gmall.service.ManageService;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {
    //设置cookie名字
    private String COOKIECARTNAME = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;
    @Reference
    private ManageService manageService;



    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {

        String cartJson  = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);
        List<CartInfo> cartInfoList=new ArrayList<>();
        boolean logFlag=true;
        if(cartJson!=null){
              //购物车已有商品
              //此时又分两种情况 已有商品 只需要更新数量  没有商品要执行填加 为了避免重复代码定义一个变量
              //这个变量要定义在最外层if之外

            cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
            //先循环遍历一次  把已经存在在cookie在的商品数量更新
            for (CartInfo cartInfo : cartInfoList) {
                  if(cartInfo.getSkuId().equals(skuId)){
                        //要保存的商品id cookie中保存的list已经有了  更新数量
                      cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                        //再把实时价格赋值
                      cartInfo.setSkuPrice(cartInfo.getCartPrice());
                        //数量更新完,后边不需要在遍历了 一次添加一种商品

                      //释放信号商品已经保存
                      logFlag=false;
                      break;

                  }
            }
        }
        //假如logFlag还是true
        if(logFlag){
            //把商品信息取出来，新增到购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo=new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            //添加到list中
            cartInfoList.add(cartInfo);
        }

        //最后把更新过后的 carinfoList 替换掉原来的
        CookieUtil.setCookie(request,response,COOKIECARTNAME,JSON.toJSONString(cartInfoList),COOKIE_CART_MAXAGE,true);

    }

    /**
     * 通过cookie查购物车中的数据
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request) {

        String cartJson = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);
        if(cartJson!=null){
            List<CartInfo> cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
            return cartInfoList;
        }


        return null;
    }

    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,COOKIECARTNAME);
    }

    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {

        //  取出购物车中的商品
        List<CartInfo> cartList = getCartList(request);
        // 循环比较
        for (CartInfo cartInfo : cartList) {
            if (cartInfo.getSkuId().equals(skuId)){
                cartInfo.setIsChecked(isChecked);
            }
        }
        // 保存到cookie
        String newCartJson = JSON.toJSONString(cartList);
        CookieUtil.setCookie(request,response,COOKIECARTNAME,newCartJson,COOKIE_CART_MAXAGE,true);
    }
}
