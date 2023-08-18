package com.yang.yangbi.bizmq;

import com.rabbitmq.client.Channel;
import com.yang.yangbi.common.ErrorCode;
import com.yang.yangbi.exception.BusinessException;
import com.yang.yangbi.manager.AiManager;
import com.yang.yangbi.model.entity.Chart;
import com.yang.yangbi.service.ChartService;
import com.yang.yangbi.utils.ExcelUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME} , ackMode = "MANUAL")
    public void receiverMessage(String message, Channel channel , @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
//        if (StringUtils.isBlank(message)){
//            //long deliveryTag, deliveryTag
//            // boolean multiple,是否全部拒绝
//            // boolean requeue,是否要重新放入队列
//            //消息拒绝
//            channel.basicNack(deliveryTag,false,false);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
//        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"图表为空");
        }

        //先将图表任务改为“执行中”，等待执行成功后，修改为已完成，保存执行结果。执行失败后，状态修改为失败，记录任务失败信息
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean result1 = chartService.updateById(updateChart);
        if (!result1){
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(updateChart.getId(),"图表状态更新失败");
            return;
        }
        long biModelId = 1659171950288818178l;
        //发送给AI并且返回结果
        String res = aiManager.doChat(biModelId, buildUserInput(chart));
        String[] split = res.split("【【【【【");
        if (split.length < 3){
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(),"AI生成错误");
            return;
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setGenResult(genResult);
        updateChartResult.setGenChart(genChart);
        updateChartResult.setStatus("succeed");
        updateChartResult.setId(chart.getId());
        boolean b = chartService.updateById(updateChartResult);
        if (!b){
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(updateChartResult.getId(),"更新图表成功状态失败");
            return;
        }
        //手动执行ACK
        channel.basicAck(deliveryTag,false);
    }


    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_DEATH_QUEUE_NAME} , ackMode = "MANUAL")
    public void rejectMessage(String message, Channel channel , @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        chart.setStatus("failed");
        channel.basicAck(deliveryTag,false);
    }



    /**
     * 构造用户输入
     * @param chart
     */

    private String buildUserInput(Chart chart){

        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        //构造用户输入用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += "请使用：" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据").append("\n");

        //读取用户上传的excel文件,压缩
        userInput.append("数据:").append(csvData).append("\n");
        return userInput.toString();
    }

    private void handleChartUpdateError(Long chartId,String exeMessage){
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExeMessage("exeMessage");
        boolean b = chartService.updateById(updateChartResult);
        if (!b){
            log.error("更新图表失败状态失败" );
        }
    }
}
