package com.lagou.edu.message.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author: ma wei long
 * @date:   2020年6月28日 下午6:56:33
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SocketMessageDTO implements Serializable{
    /**
    */
	private static final long serialVersionUID = -1921689979632252341L;
	public String message;
    public String date;
}