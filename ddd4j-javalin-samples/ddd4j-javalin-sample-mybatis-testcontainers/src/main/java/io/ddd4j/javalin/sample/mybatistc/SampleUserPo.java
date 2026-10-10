package io.ddd4j.javalin.sample.mybatistc;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Persistence object for the sample MyBatis-Plus mapper.
 */
@TableName("sample_user")
public class SampleUserPo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;

    /**
     * 返回用户标识。
     * @return 用户标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置标识。
     * @param id 标识值。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回用户名。
     * @return 用户名。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     * @param username 用户名。
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 返回邮箱地址。
     * @return 邮箱地址。
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱地址。
     * @param email 邮箱地址。
     */
    public void setEmail(String email) {
        this.email = email;
    }
}