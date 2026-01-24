package com.example.kloset_lab.feed.repository;

import com.example.kloset_lab.feed.entity.FeedLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    Optional<FeedLike> findByFeedIdAndUserId(Long feedId, Long userId);

    void deleteByFeedIdAndUserId(Long feedId, Long userId);

    List<FeedLike> findByFeedIdInAndUserId(List<Long> feedIds, Long userId);
}
