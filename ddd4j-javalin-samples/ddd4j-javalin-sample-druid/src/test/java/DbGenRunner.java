import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import lombok.SneakyThrows;
import org.jooq.SQLDialect;
import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.Definition;
import org.jooq.meta.jaxb.*;

import java.io.File;
import java.util.List;
import java.util.Map;

public class DbGenRunner {

    private static final Map<SQLDialect, Map<String, String>> sqlDialectMap = Map.of(
        SQLDialect.H2, Map.of(
            "driver", org.h2.Driver.class.getName(),
            "name", org.jooq.meta.h2.H2Database.class.getName()
        ),
        SQLDialect.MYSQL, Map.of(
            "driver", com.mysql.cj.jdbc.Driver.class.getName(),
            "name", org.jooq.meta.mysql.MySQLDatabase.class.getName()
        )
    );

    public static void main(String[] args) throws Exception {
        var config = FileUtil.readUtf8String(new File(args[0]));
        var json = JSONUtil.parseObj(config);
        var dbMap = json.getJSONObject("databases");
        var dbsToGen = List.of(
            "write"
        );

        for (var dbAlias : dbsToGen) {
            var c = dbMap.getJSONObject(dbAlias);
            var url = c.getStr("url");
            var user = c.getStr("user");
            var pass = c.getStr("pass");
            var schema = c.getStr("schema");
            var dialectName = c.get("dialect", SQLDialect.class);

            if (! sqlDialectMap.containsKey(dialectName)) {
                throw new RuntimeException("不支持的SQL方言: " + dialectName);
            }

            var dialect = sqlDialectMap.get(dialectName);

            genrate(url, user, pass, schema, dbAlias, dialect.get("driver"), dialect.get("name"));
        }

    }

    @SneakyThrows
    private static void genrate(String jdbcUrl, String jdbcUser, String jdbcPassword, String schema, String alias, String driver, String dbName) {
        var conf = new Configuration()
            .withJdbc(new Jdbc()
                .withDriver(driver)
                .withUrl(jdbcUrl)
                .withUser(jdbcUser)
                .withPassword(jdbcPassword))
            .withGenerator(new Generator()
                .withName(org.jooq.codegen.JavaGenerator.class.getName())
                .withGenerate(new Generate()
                    .withPojos(true)
                    .withDaos(false))
                .withStrategy(new Strategy()
                    .withName(JooqGenConfig.class.getName()))
                .withTarget(new Target()
                    .withDirectory("base/src/main/java")
                    .withPackageName(DbGenRunner.class.getPackageName()+".db."+alias))
                .withDatabase(new Database()
                    .withName(dbName)
                    .withIncludes(".*")
                    .withExcludes("")
                    .withInputSchema(schema)
                    .withOutputSchemaToDefault(true)
                    .withForcedTypes(new ForcedType()
                        .withName("BOOLEAN")
                        .withIncludeTypes("(?i:TINYINT\\(1\\))"))));

        GenerationTool.generate(conf);
    }

    public static class JooqGenConfig extends DefaultGeneratorStrategy {
        @Override
        public String getJavaClassName(Definition definition, Mode mode) {
            var name = super.getJavaClassName(definition, mode);

            if(definition.getName().startsWith("t_")) {
                name = name.substring(1);
            }

            if(mode.equals(Mode.POJO)) {
                name += "Po";
            }

            return name;
        }


        @Override
        public String getJavaIdentifier(Definition definition) {
            var id = super.getJavaIdentifier(definition);

            if(id.startsWith("T_")) {
                id = id.substring(2);
            }

            return id;
        }
    }
}
