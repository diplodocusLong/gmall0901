package com.lianglong.gmall.service;

import com.lianglong.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    /**
     * 商品持久化
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public  void  addToCart(String skuId,String userId,Integer skuNum);

    List<CartInfo> getCartList(String userId);

    List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId);

    void checkCart(String skuId, String isChecked, String userId);

    public  List<CartInfo> getCartCheckedList(String userId);
}
