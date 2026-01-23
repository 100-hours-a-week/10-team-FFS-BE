package com.example.kloset_lab.feed.repository;

import com.example.kloset_lab.feed.entity.FeedClothesMapping;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedClothesMappingRepository extends JpaRepository<FeedClothesMapping, Long> {

    List<FeedClothesMapping> findByFeedId(Long feedId);
}
