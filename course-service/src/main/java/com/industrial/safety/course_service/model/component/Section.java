package com.industrial.safety.course_service.model.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section
{
    private String id;
    private String title;
    private List<Lecture> lectureList;
}
