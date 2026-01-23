package com.example.kloset_lab.feed.repository;

import com.example.kloset_lab.feed.entity.FeedImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

    List<FeedImage> findByFeedIdOrderByDisplayOrderAsc(Long feedId);
}
