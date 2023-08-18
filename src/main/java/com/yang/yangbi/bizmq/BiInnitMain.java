package com.yang.yangbi.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;
public class BiInnitMain {


    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            //设置rabbitmq基本信息
            //服务地址
//            factory.setHost("192.168.226.130");
            factory.setHost("47.115.208.19");
            //账号
//            factory.setUsername("admin");
            factory.setUsername("yang");
            //密码
//            factory.setPassword("123456");
            factory.setPassword("594188hh");
            //端口号
            factory.setPort(5672);

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(BiMqConstant.BI_EXCHANGE_NAME,"direct");
            channel.exchangeDeclare(BiMqConstant.BI_DEATH_EXCHANGE_NAME,"direct");
//            创建死信队列
            String deathQueueName = BiMqConstant.BI_DEATH_QUEUE_NAME;
            channel.queueDeclare(deathQueueName,true,false,false,null);
            channel.queueBind(BiMqConstant.BI_DEATH_QUEUE_NAME,BiMqConstant.BI_DEATH_EXCHANGE_NAME,BiMqConstant.BI_DEATH_ROUTING_KEY);
            Map<String,Object> args1 = new HashMap<>();
            args1.put("x-dead-letter-exchange",BiMqConstant.BI_DEATH_EXCHANGE_NAME);
            args1.put("x-dead-letter-routing-key",BiMqConstant.BI_DEATH_ROUTING_KEY);
            String queueName = BiMqConstant.BI_QUEUE_NAME;
            channel.queueDeclare(queueName,true,false,false, args1);
            channel.queueBind(queueName,BiMqConstant.BI_EXCHANGE_NAME,BiMqConstant.BI_ROUTING_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
