package io.ddd4j.javalin.annotation.ddd;

import com.google.inject.Singleton;
import io.ddd4j.annotation.ddd.DDDAnnotation;
import io.ddd4j.javalin.annotation.web.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 独立验证器：验证 ddd4j-javalin-annotation 的 11 个 DDD 注解
 * 与 Guice @Singleton 元注解融合 + 7 个 web/ 路由参数注解。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public final class AnnotationFusionVerifier {

    private AnnotationFusionVerifier() {
    }

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("=== DDD 注解元注解融合验证 ===");
        passed += verify("DomainService", DomainService.class);
        passed += verify("DomainRepository", DomainRepository.class);
        passed += verify("ApplicationService", ApplicationService.class);
        passed += verify("QueryService", QueryService.class);
        passed += verify("CommandExecutor", CommandExecutor.class);
        passed += verify("DomainEntity", DomainEntity.class);
        passed += verify("DomainValueObject", DomainValueObject.class);
        passed += verify("DomainGateway", DomainGateway.class);
        passed += verify("DomainAssembler", DomainAssembler.class);
        passed += verify("DomainConverter", DomainConverter.class);

        System.out.println();
        System.out.println("=== @DomainEvent 不下沉验证 ===");
        try {
            Class.forName("io.ddd4j.javalin.annotation.ddd.DomainEvent");
            System.out.println("FAIL: @DomainEvent 不应在 ddd4j-javalin-annotation 中");
            failed++;
        } catch (ClassNotFoundException e) {
            System.out.println("PASS: @DomainEvent 不在 ddd4j-javalin-annotation");
            passed++;
        }

        System.out.println();
        System.out.println("=== Web 路由参数注解验证 ===");
        passed += verifyWebAnnotation("PathParam", PathParam.class);
        passed += verifyWebAnnotation("QueryParam", QueryParam.class);
        passed += verifyWebAnnotation("FormParam", FormParam.class);
        passed += verifyWebAnnotation("HeaderParam", HeaderParam.class);
        passed += verifyWebAnnotation("CookieParam", CookieParam.class);
        passed += verifyWebAnnotation("BodyParam", BodyParam.class);
        passed += verifyWebAnnotation("Context", Context.class);

        System.out.println();
        System.out.println("=== 业务代码使用模式验证 ===");
        // BusinessDomainService 直接标注了 @DomainService（来自本模块）。
        // @DomainService 本身融合了 @DDDAnnotation 和 @Singleton 作为元注解。
        // Class.getAnnotation() 不递归元注解——所以业务类只能直接拿到 @DomainService，
        // 而框架（Guice/ArchUnit）通过遍历注解的 annotationType() 上的元注解来识别。
        java.lang.annotation.Annotation domainServiceOnBiz = BusinessDomainService.class.getAnnotation(DomainService.class);
        if (domainServiceOnBiz == null) {
            System.out.println("FAIL: 业务类未标注 @DomainService");
            failed++;
        } else {
            // 通过 @DomainService 注解类型反查元注解，验证框架识别链路
            DDDAnnotation ddd = DomainService.class.getAnnotation(DDDAnnotation.class);
            Singleton singleton = DomainService.class.getAnnotation(Singleton.class);
            if (ddd != null && singleton != null) {
                System.out.println("PASS: 业务代码 @DomainService -> 元注解链路完整（@DDDAnnotation + @Singleton 可被框架识别）");
                passed++;
            } else {
                System.out.println("FAIL: @DomainService 元注解缺失");
                failed++;
            }
        }

        System.out.println();
        System.out.println("========================================");
        int total = passed + failed;
        System.out.println("总计: " + total + " | 通过: " + passed + " | 失败: " + failed);
        System.out.println("========================================");

        if (failed > 0) {
            System.err.println("VERIFICATION FAILED");
            System.exit(1);
        } else {
            System.out.println("ALL PASSED! ddd4j-javalin-annotation 符合架构设计");
        }
    }

    private static int verify(
            String name,
            Class<? extends java.lang.annotation.Annotation> dddAnnotation) {

        DDDAnnotation ddd = dddAnnotation.getAnnotation(DDDAnnotation.class);
        if (ddd == null) {
            System.out.println("FAIL " + name + ": 缺少 @DDDAnnotation");
            return 0;
        }
        Singleton singleton = dddAnnotation.getAnnotation(Singleton.class);
        if (singleton == null) {
            System.out.println("FAIL " + name + ": 缺少 @Singleton");
            return 0;
        }
        System.out.println("PASS " + name + ": @DDDAnnotation + @Singleton");
        return 1;
    }

    private static int verifyWebAnnotation(
            String name,
            Class<? extends java.lang.annotation.Annotation> annotation) {
        Target target = annotation.getAnnotation(Target.class);
        if (target == null || target.value().length == 0
                || target.value()[0] != ElementType.PARAMETER) {
            System.out.println("FAIL " + name + ": @Target 必须为 PARAMETER");
            return 0;
        }
        Retention retention = annotation.getAnnotation(Retention.class);
        if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
            System.out.println("FAIL " + name + ": @Retention 必须为 RUNTIME");
            return 0;
        }
        System.out.println("PASS " + name + ": @Target PARAMETER + @Retention RUNTIME");
        return 1;
    }
}

/**
 * 业务代码风格示例：只用 @DomainService 一个注解。
 * 独立顶层类（package-private），避免 javac 内部类注解处理差异。
 */
@DomainService
class BusinessDomainService {
    public String hello() {
        return "hello";
    }
}
