package com.industrial.safety.exam_service.ranking.mapper;

import com.industrial.safety.exam_service.ranking.dto.RankingEntryResponse;
import com.industrial.safety.exam_service.ranking.model.WorkerScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RankingMapper {

    @Mapping(target = "position", source = "position")
    @Mapping(target = "userId", source = "score.userId")
    @Mapping(target = "userName", source = "score.userName")
    @Mapping(target = "totalPoints", source = "score.totalPoints")
    RankingEntryResponse toResponse(WorkerScore score, int position);
}
