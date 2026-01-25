package com.example.kloset_lab.comment.dto;

import com.example.kloset_lab.global.response.PageInfo;
import java.util.List;

public record CommentPagedResponse<T>(Long parentId, List<T> items, PageInfo pageInfo) {}
