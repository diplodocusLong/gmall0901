package com.lianglong.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.lianglong.gmall.bean.CartInfo;
import com.lianglong.gmall.bean.OrderDetail;
import com.lianglong.gmall.bean.OrderInfo;
import com.lianglong.gmall.bean.UserAddress;
import com.lianglong.gmall.config.LoginRequire;
import com.lianglong.gmall.service.CartService;
import com.lianglong.gmall.service.OrderService;
import com.lianglong.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    // 调用根据用户Id 查询用户的地址
//    @Autowired
    @Reference
    private UserService userService;
    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

//    @RequestMapping("trade")
//    @ResponseBody
//    public List<UserAddress> trade(String userId){
//        return userService.findUserAddressByUserId(userId);
//    }

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request) {
        // 根据用户Id 得到用户的地址
        String userId = (String) request.getAttribute("userId");
        // 调用方法
        List<UserAddress> userAddressList = userService.findUserAddressByUserId(userId);

        // 先获取购物车被选中的商品列表
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        // 声明一个集合 订单明细的集合
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        // 得到商品列表，将其循环将里面的值赋给orderDetail 。
        if (cartInfoList != null && cartInfoList.size() > 0) {
            // 赋值
            for (CartInfo cartInfo : cartInfoList) {
                // 创建订单明细对象
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetailList.add(orderDetail);
            }
        }
        // 总价格
        OrderInfo orderInfo = new OrderInfo();
        // 将订单明细集合赋值给orderInfo
        orderInfo.setOrderDetailList(orderDetailList);
        // 会将计算的结果赋值给orderInfo.totalAmount
        orderInfo.sumTotalAmount();
        // 保存总价格
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());
        // 保存订单明细集合对象
        request.setAttribute("orderDetailList", orderDetailList);
        // 保存集合
        request.setAttribute("userAddressList", userAddressList);

        // 生成一个流水号：将userId 作为一个key 来存储编号。
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo", tradeNo);
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo, HttpServletRequest request) {
        // 获取userId
        String userId = (String) request.getAttribute("userId");
        // 获取tradeNo
        String tradeNo = request.getParameter("tradeNo");

        // 调用方法进行比较
        boolean result = orderService.checkTradeCode(userId, tradeNo);
        // 比较失败！
        if (!result) {
            request.setAttribute("errMsg", "不能重复提交订单，请重新下单");
            return "tradeFail";
        }
        // 验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                // 验证每一个商品是否有足够的库存
                boolean flag = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!flag) {
                    request.setAttribute("errMsg", "库存不足，请重新下单");
                    return "tradeFail";
                }
            }
        }
        // 验价：验证当前商品的价格，是否与此时的商品价格一致！


        orderInfo.setUserId(userId);
        // 计算总金额
        orderInfo.sumTotalAmount();
        // 将总金额放入对象
        orderInfo.setTotalAmount(orderInfo.getTotalAmount());

        String orderId = orderService.saveOrder(orderInfo);

        // 删除redis 中的流水号
        orderService.delTradeCode(userId);
        return "redirect://payment.gmall.com/index?orderId=" + orderId;
    }

    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request) {

        String orderId = request.getParameter("orderId");

        String wareSkuMap = request.getParameter("wareSkuMap");

        //定义订单集合
        List<OrderInfo> subOrderInfoList = orderService.splitOrder(orderId,wareSkuMap);

        List<Map> wareMapList = new ArrayList<>();

        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);

            wareMapList.add(map);
        }

        return JSON.toJSONString(wareMapList);
    }
}