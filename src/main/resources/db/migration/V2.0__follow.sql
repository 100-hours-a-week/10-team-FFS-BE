CREATE TABLE follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_follow_follower_followee UNIQUE (follower_id, followee_id),
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES user(id),
    CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES user(id)
);

ALTER TABLE user_profile ADD COLUMN follower_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_profile ADD COLUMN following_count BIGINT NOT NULL DEFAULT 0;