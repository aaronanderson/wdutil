package wdutil.wdjws;

import java.security.cert.X509Certificate;
import java.util.function.Supplier;

public class TenantInfo {
    final String name;
    final String tenantId;
    final Environment<?> env;
    final String version;
    final String userId;
    final String password;
    final X509Certificate cert;
    final byte[] secretKey;
    final Supplier<String> tokenProvider;
    int batchSize = 999;
    int timeout = 90000;
    boolean trace = false;

    public TenantInfo(String name, String tenantId, Environment<?> env, String version, String userId, String password) {
        this.name = name;
        this.tenantId = tenantId;
        this.env = env;
        this.version = version;
        this.userId = userId;
        this.password = password;
        this.cert = null;
        this.secretKey = null;
        this.tokenProvider = null;
    }

    public TenantInfo(String name, String tenantId, String userId, Environment<?> env, String version, X509Certificate cert, byte[] secretKey) {
        this.name = name;
        this.tenantId = tenantId;
        this.env = env;
        this.version = version;
        this.userId = userId;
        this.password = null;
        this.cert = cert;
        this.secretKey = secretKey;
        this.tokenProvider = null;
    }

    public TenantInfo(String name, String tenantId, Environment<?> env, String version, Supplier<String> tokenProvider) {
        this.name = name;
        this.tenantId = tenantId;
        this.env = env;
        this.version = version;
        this.userId = null;
        this.password = null;
        this.cert = null;
        this.secretKey = null;
        this.tokenProvider = tokenProvider;

    }

    public String getName() {
        return name;
    }

    public String getTenantId() {
        return tenantId;
    }

    @SuppressWarnings("unchecked")
    public <T> Environment<T> getEnv() {
        return (Environment<T>) env;
    }

    public String getVersion() {
        return version;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }       

    public Supplier<String> getOauthTokenProvider() {
        return tokenProvider;
    }

    public X509Certificate getCert() {
        return cert;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isTrace() {
        return trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

}
