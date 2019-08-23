package com.lianglong.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {

    //psvm
    public static void main(String[] args) throws JMSException {
         //创建链接工厂
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.84.100:61616");
        //获取链接
        Connection connection = activeMQConnectionFactory.createConnection();
        //打开链接
        connection.start();

        //创建会话 1,是否开启事务2.根据第一个参数 手动或自动签收
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);//不开启事务 自动提交

        //创建一个队列
        Queue queue = session.createQueue("atguigu");
        //将队列放入提供者对象
        MessageProducer producer = session.createProducer(queue);

        //提供者对象发送消息
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();

        activeMQTextMessage.setText("你好,你叫什么名字,多大了");

        producer.send(activeMQTextMessage);

        producer.close();

        session.close();

        connection.close();
    }
}
