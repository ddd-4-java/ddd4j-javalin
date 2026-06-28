package io.ddd4j.javalin.auth.security;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.auth.security.subject.SecuritySubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;

/**
 * ddd4j-javalin + Spring Security Guice 整合模块。
 *
 * <p>注册 SecuritySubjectProvider 到 Guice 容器并写回 SubjectKit。
 *
 * <p>注意：Spring Security 依赖 Spring 生态，在 Javalin 环境下仅作为兼容选项。
 * 推荐 Javalin 项目使用 sa-token（{@code ddd4j-javalin-auth-satoken}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jSecurityJavalinModule extends AbstractModule {

    @Provides
    @Singleton
    public SubjectProvider subjectProvider() {
        SecuritySubjectProvider provider = new SecuritySubjectProvider();
        SubjectKit.register(provider);
        return provider;
    }

}
