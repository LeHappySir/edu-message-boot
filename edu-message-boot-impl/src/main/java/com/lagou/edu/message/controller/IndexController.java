package com.lagou.edu.message.controller;

import com.lagou.edu.common.response.ResponseDTO;
import com.lagou.edu.message.client.dto.Message;
import com.lagou.edu.message.server.PushServer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author: ma wei long
 * @date:   2020年6月28日 下午1:46:09
 */
@Controller
public class IndexController {

	
    @GetMapping("/index")
    public String getCourseOrderByOrderNo() {
        return "index";
    }
    
    @GetMapping("/sendMessage")
    @ResponseBody
    public ResponseDTO<?> sendMessage() {
    	Message message = new Message();
    	message.setContent("test");
    	message.setUserId(100029966);
    	PushServer.pushServer.push(message);
        return ResponseDTO.success();
    }
}
