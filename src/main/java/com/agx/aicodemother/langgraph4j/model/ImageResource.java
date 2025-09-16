package com.agx.aicodemother.langgraph4j.model;

import com.agx.aicodemother.langgraph4j.enums.ImageCategoryEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图片资源对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResource implements Serializable {

    /**
     * 图片类别
     */
    private ImageCategoryEnum category;

    /**
     * 图片描述
     */
    private String description;

    /**
     * 图片地址
     */
    private String url;

    private static final long serialVersionUID = 1L;
}
