package com.yang.yangbi.manager;

import com.yang.yangbi.common.ErrorCode;
import com.yang.yangbi.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AiManager {

    @Resource
    private YuCongMingClient yuCongMingClient;

    public String doChat(long modelId,String msg ){
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(msg);
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
//        System.out.println(response.getData());
        if (response == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"ai响应错误");
        }
        return response.getData().getContent();

    }
}
