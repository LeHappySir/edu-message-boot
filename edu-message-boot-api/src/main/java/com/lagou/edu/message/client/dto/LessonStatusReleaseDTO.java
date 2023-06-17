package com.lagou.edu.message.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author: ma wei long
 * @date:   2020年6月29日 下午11:48:54
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LessonStatusReleaseDTO implements Serializable {

	/**
	 */
	private static final long serialVersionUID = 4667691442836548033L;
	
	//课时id
	private Integer lessonId;
   
}
