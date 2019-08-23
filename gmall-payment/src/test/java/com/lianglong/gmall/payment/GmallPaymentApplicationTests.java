package com.lianglong.gmall.payment;

import com.lianglong.gmall.config.ActiveMQUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

    @Autowired
    private ActiveMQUtil activeMQUtil;
    
    @Test
    public void testActiveMq() throws JMSException {

        Connection connection = activeMQUtil.getConnection();

        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);//不开启事务 自动提交

        //创建一个队列
        Queue queue = session.createQueue("Atguigu");
        //将队列放入提供者对象
        MessageProducer producer = session.createProducer(queue);

        //提供者对象发送消息
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();

        activeMQTextMessage.setText("你好,你叫什么名字,多大了,有对象了吗");

        producer.send(activeMQTextMessage);

        producer.close();

        session.close();

        connection.close();


    }



	@Test
	public void contextLoads() {
	}

}

