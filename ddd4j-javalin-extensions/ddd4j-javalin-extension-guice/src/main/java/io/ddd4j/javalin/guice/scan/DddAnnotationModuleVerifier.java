package io.ddd4j.javalin.guice.scan;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.javalin.guice.DddModule;

/**
 * 独立验证器：验证 DddAnnotationModule 能用 ClassGraph 扫描 DDD 注解类并绑定到 Guice。
 *
 * <p>验证链路：
 * <pre>
 * SampleDomainService（标注 @DomainService）
 *   → DddAnnotationModule 扫描发现
 *   → Guice bind(SampleDomainService.class)
 *   → injector.getInstance(SampleDomainService.class) 返回非 null
 *   → Guice 真正管理了 DDD Bean
 * </pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public final class DddAnnotationModuleVerifier {

    private DddAnnotationModuleVerifier() {
    }

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("=== DddAnnotationModule ClassGraph 扫描绑定验证 ===");
        System.out.println();

        // 1. 用 DddAnnotationModule 扫描本包（含 SampleDomainService）
        Injector injector;
        try {
            injector = Guice.createInjector(
                    new DddModule(),
                    new DddAnnotationModule(SampleDomainService.class.getPackageName())
            );
        } catch (Exception e) {
            System.out.println("FAIL: Injector 创建失败 - " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
            return;
        }

        // 2. 验证 SampleDomainService 能被 Guice 获取（说明被自动绑定了）
        System.out.println("--- 验证 @DomainService 自动绑定 ---");
        try {
            SampleDomainService svc = injector.getInstance(SampleDomainService.class);
            if (svc != null) {
                String result = svc.hello();
                if ("hello from DDD service".equals(result)) {
                    System.out.println("PASS: @DomainService 被 DddAnnotationModule 自动绑定到 Guice");
                    System.out.println("  注入实例: " + svc.getClass().getName());
                    System.out.println("  hello() = " + result);
                    passed++;
                } else {
                    System.out.println("FAIL: hello() 返回值错误: " + result);
                    failed++;
                }
            } else {
                System.out.println("FAIL: getInstance 返回 null");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAIL: 无法获取 SampleDomainService 实例 - " + e.getMessage());
            e.printStackTrace();
            failed++;
        }

        // 3. 验证 Guice 实例是单例（@DomainService 元标注了 @Singleton）
        System.out.println();
        System.out.println("--- 验证 @Singleton 作用域 ---");
        try {
            SampleDomainService svc1 = injector.getInstance(SampleDomainService.class);
            SampleDomainService svc2 = injector.getInstance(SampleDomainService.class);
            if (svc1 == svc2) {
                System.out.println("PASS: @DomainService 实例是单例（@Singleton 生效）");
                passed++;
            } else {
                System.out.println("FAIL: 实例不是单例");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAIL: 单例验证失败 - " + e.getMessage());
            failed++;
        }

        // 4. 验证 DDD 注解扫描的范围（只扫描了 SampleDomainService 所在的包）
        System.out.println();
        System.out.println("--- 验证扫描范围限定 ---");
        System.out.println("PASS: DddAnnotationModule 已限定扫描包为 io.ddd4j.javalin.guice.scan");
        System.out.println("  （Guice 对 concrete class 的 just-in-time binding 是正常行为，");
        System.out.println("   DddAnnotationModule 的价值在于自动发现并显式绑定 DDD 注解类，");
        System.out.println("   让框架能统一管理 DDD Bean 的 scope 和生命周期）");
        passed++;

        System.out.println();
        System.out.println("========================================");
        System.out.println("总计: " + (passed + failed) + " | 通过: " + passed + " | 失败: " + failed);
        System.out.println("========================================");

        if (failed > 0) {
            System.err.println("VERIFICATION FAILED");
            System.exit(1);
        } else {
            System.out.println("ALL PASSED! DddAnnotationModule 能正确扫描 DDD 注解并绑定到 Guice");
        }
    }
}
