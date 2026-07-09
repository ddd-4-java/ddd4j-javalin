package io.ddd4j.javalin.data.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 测试用 MyBatis-Plus 实体（PO）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Data
@TableName("test_user")
public class TestUserPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;
}
