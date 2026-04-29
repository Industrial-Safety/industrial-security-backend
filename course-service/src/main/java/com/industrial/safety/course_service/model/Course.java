package com.industrial.safety.course_service.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import java.util.List;

@Document(value = "course")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Course
{
    @Id
    private String id;
    private String title;
    private String subtitle;
    private Teacher teacher;
    private Details details;
    private List<String> requirements;
    private List<String> learningOutcomes;
    private List<Section> sectionList;
    private Review reviews;
}
