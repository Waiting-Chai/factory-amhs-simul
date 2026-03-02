package com.semi.simlogistics.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redisson client properties.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@ConfigurationProperties(prefix = "sim.redisson")
public class RedissonProperties {

    private String address = "redis://127.0.0.1:6379";
    private String password;
    private int database = 0;
    private boolean lazyInitialization = true;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public boolean isLazyInitialization() {
        return lazyInitialization;
    }

    public void setLazyInitialization(boolean lazyInitialization) {
        this.lazyInitialization = lazyInitialization;
    }
}
