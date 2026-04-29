package com.industrial.safety.course_service.model;

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
    private ResourceType typeUrl;
    private String fileSize;
}
