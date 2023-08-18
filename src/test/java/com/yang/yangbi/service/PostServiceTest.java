package com.yang.yangbi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.yangbi.model.dto.post.PostQueryRequest;
import com.yang.yangbi.model.entity.Post;
import javax.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 帖子服务测试
 *
 * @autho yangyang
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@SpringBootTest
class PostServiceTest {

    @Resource
    private PostService postService;

    @Test
    void searchFromEs() {
        PostQueryRequest postQueryRequest = new PostQueryRequest();
        postQueryRequest.setUserId(1L);
        Page<Post> postPage = postService.searchFromEs(postQueryRequest);
        Assertions.assertNotNull(postPage);
    }

}