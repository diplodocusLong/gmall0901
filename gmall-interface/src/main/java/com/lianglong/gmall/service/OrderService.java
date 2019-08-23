package com.lianglong.gmall.service;

import com.lianglong.gmall.bean.OrderInfo;
import com.lianglong.gmall.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {

    public  String  saveOrder(OrderInfo orderInfo);

    boolean checkTradeCode(String userId, String tradeNo);

    String getTradeNo(String userId);

    boolean checkStock(String skuId, Integer skuNum);

    void delTradeCode(String userId);


    OrderInfo getOrderInfo(String orderId);

    void updateOrderStatus(String orderId, ProcessStatus paid);

    void sendOrderStatus(String orderId);

    /**
     * 查找过期的订单
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期订单
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /**
     * orderInfo对象转化为map
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);
}
