package com.campus.courier.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MybatisFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject meta) {
        this.strictInsertFill(meta, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(meta, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject meta) {
        this.strictUpdateFill(meta, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
