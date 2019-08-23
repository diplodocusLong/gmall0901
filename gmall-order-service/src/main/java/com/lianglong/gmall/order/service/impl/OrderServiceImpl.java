package com.lianglong.gmall.order.service.impl;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.lianglong.gmall.bean.OrderDetail;
import com.lianglong.gmall.bean.OrderInfo;
import com.lianglong.gmall.bean.enums.ProcessStatus;
import com.lianglong.gmall.config.ActiveMQUtil;
import com.lianglong.gmall.config.RedisUtil;
import com.lianglong.gmall.order.mapper.OrderDetailMapper;
import com.lianglong.gmall.order.mapper.OrderInfoMapper;
import com.lianglong.gmall.service.OrderService;
import com.lianglong.gmall.service.PaymentService;
import com.lianglong.gmall.utils.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Reference
    PaymentService paymentService;

    @Override
    public String saveOrder(OrderInfo orderInfo) {
        // 设置创建时间
        orderInfo.setCreateTime(new Date());
        // 设置失效时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 生成第三方支付编号
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfoMapper.insertSelective(orderInfo);

        // 插入订单详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
// 为了跳转到支付页面使用。支付会根据订单id进行支付。
        String orderId = orderInfo.getId();
        return orderId;

    }

    // 生成流水号
    public  String getTradeNo(String userId){
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            return  true;
        }else {
            return  false;
        }
    }

    // 验证流水号
    public  boolean checkTradeCode(String userId,String tradeCodeNo){
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:"+userId+":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();
        if (tradeCode!=null && tradeCode.equals(tradeCodeNo)){
            return  true;
        }else{
            return false;
        }
    }
    // 删除流水号
    public void  delTradeCode(String userId){
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey =  "user:"+userId+":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        Example example = new Example(OrderDetail.class);
        example.createCriteria().andEqualTo("orderId",orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectByExample(example);
        orderInfo.setOrderDetailList(orderDetails);
        return  orderInfo;

    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(paid);
        orderInfo.setOrderStatus(paid.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        String orderJson = initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(order_result_queue);
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(orderJson);
            producer.send(activeMQTextMessage);
            session.commit();
            session.close();
            producer.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }
    }



    @Override
    public List<OrderInfo> getExpiredOrderList() {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("processStatus",ProcessStatus.UNPAID).andLessThan("expireTime",new Date());
        List<OrderInfo> unpaidOrderInfoList = orderInfoMapper.selectByExample(example);
        return  unpaidOrderInfoList;
    }

    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 订单信息
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 付款信息
        paymentService.closePayment(orderInfo.getId());

    }

    private String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);

    }

    public Map initWareOrder(OrderInfo orderInfo) {
        Map<String,Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","测试");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());

        List detailList = new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(orderDetailList!=null&&orderDetailList.size()>0){

            for (OrderDetail orderDetail : orderDetailList) {
                Map detailMap = new HashMap();
                detailMap.put("skuId",orderDetail.getSkuId());
                detailMap.put("skuName",orderDetail.getSkuName());
                detailMap.put("skuNum",orderDetail.getSkuNum());
                detailList.add(detailMap);

            }
            map.put("details",detailList);
        }

       return map;
    }

    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        //1 先查巡原始订单
        OrderInfo orderInfoOrigin=getOrderInfo(orderId);
        //2 wareSkuMap 反序列化
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        //3 遍历拆单 方案
        for (Map map : maps) {
           String wareId= (String) map.get("wareId");
            List<String> skuIds = (List<String>) map.get("skuIds");
            //4 生成订单主表,从原始订单复制,新的订单号,父单号
            OrderInfo subOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderInfoOrigin.getId());
            subOrderInfo.setWareId(wareId);

            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            //子单的商品详情
            List<OrderDetail> subOrderDetailList = new ArrayList<>();

            for (OrderDetail orderDetail : orderDetailList) {
                for (String skuId : skuIds) {
                    if(skuId.equals(orderDetail.getSkuId())){
                        orderDetail.setId(null);
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }

            subOrderInfo.setOrderDetailList(subOrderDetailList);
            subOrderInfo.sumTotalAmount();
            //保存到数据库中
            saveOrder(subOrderInfo);
            subOrderInfoList.add(subOrderInfo);
        }
          //更新原订单状态为已拆单
         updateOrderStatus(orderId,ProcessStatus.SPLIT);
        //返回一个新生成的子订单列表
        return subOrderInfoList;
    }


}
