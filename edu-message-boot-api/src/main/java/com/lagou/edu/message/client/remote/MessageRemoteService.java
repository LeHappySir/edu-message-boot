package com.lagou.edu.message.client.remote;

import com.lagou.edu.common.page.DataGrid;
import com.lagou.edu.common.response.ResponseDTO;
import com.lagou.edu.message.client.dto.MessageDTO;
import com.lagou.edu.message.client.dto.MessageQueryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author: ma wei long
 * @date:   2020年6月28日 下午2:01:27
*/
@FeignClient(name = "edu-message-boot", path = "/message")
public interface MessageRemoteService {

	/**
	 * @author: ma wei long
	 * @date:   2020年6月28日 下午2:02:35   
	 */
	@PostMapping("/getMessageList")
	ResponseDTO<DataGrid<MessageDTO>> getMessageList(@RequestBody MessageQueryDTO messageQueryDTO);
	
	/**
	 * @author: ma wei long
	 * @date:   2020年6月29日 上午11:13:18   
	*/
	@PostMapping("/updateReadStatus")
	ResponseDTO<Boolean> updateReadStatus(@RequestParam("userId") Integer userId);
	
	/**
	 * @author: ma wei long
	 * @date:   2020年7月13日 下午8:01:32   
	*/
	@GetMapping("/getUnReadMessageFlag")
	ResponseDTO<Boolean> getUnReadMessageFlag(@RequestParam("userId") Integer userId);
}
