package com.industrial.safety.course_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lecture
{
    private String id;
    private String title;
    private String duration;
    private String type;
    private String urlVideo;
    private Boolean isPreview;
    private List<Resource> resourceList;
}
