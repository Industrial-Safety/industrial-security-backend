package com.industrial.safety.course_service.model.component;

import com.industrial.safety.course_service.model.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource
{
    private String id;
    private String title;
    private ResourceType resourceType;
    private String url;
    private String fileSize;
}
