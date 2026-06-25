package io.javalin.sample.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.javalin.sample.db.write.tables.User;
import io.javalin.sample.db.write.tables.pojos.UserPo;
import io.ddd4j.javalin.autoconfigure.util.AppUtil;
import io.ddd4j.javalin.autoconfigure.Const;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import java.util.List;

import static io.javalin.sample.db.write.Tables.USER;

@Slf4j
@Singleton
public class UserDao {
    @Inject
    @Named(Const.READ)
    private DSLContext readDb;

    private final User tUser = USER;

    @Nullable
    public UserPo oneUser(Long userId) {
        return readDb.selectFrom(USER).where(USER.ID.eq(userId)).fetchOneInto(UserPo.class);
    }

    public List<UserPo> firstUserInCountry(Long fromTs) {
        var sql = AppUtil.loadSql("firstUserInCountry");

        var cond = DSL.condition("1 = 1");
        if(fromTs != null && fromTs > 0) {
            cond = tUser.ID.in(DSL.select(tUser.ID).from(tUser).where(tUser.CREATED_AT.ge(fromTs)));
        }

        return readDb
            .fetch(sql, tUser, cond)
            .into(UserPo.class);
    }
}
