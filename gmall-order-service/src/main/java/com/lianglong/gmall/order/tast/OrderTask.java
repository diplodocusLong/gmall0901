package com.lianglong.gmall.order.tast;

import com.lianglong.gmall.bean.OrderInfo;
import com.lianglong.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Component
public class OrderTask {
    @Autowired
    OrderService orderService;
//
//    // 5 每分钟的第五秒
//    @Scheduled(cron = "5 * * * * ?")
//    public void work() {
//        System.out.println("Thread ====== " + Thread.currentThread());
//    }
//
//    // 0/5 没隔五秒执行一次
//    @Scheduled(cron = "0/5 * * * * ?")
//    public void work1() {
//        System.out.println("Thread1 ====== " + Thread.currentThread());
//    }


    @Scheduled(cron = "0/20 * * * * ?")
    public  void checkOrder() {
        List<OrderInfo> expiredOrderList = orderService.getExpiredOrderList();

        if(expiredOrderList!=null&&expiredOrderList.size()>0){

            for (OrderInfo orderInfo : expiredOrderList) {
                orderService.execExpiredOrder(orderInfo);
            }
        }

    }


}



