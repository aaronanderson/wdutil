package wdutil.wdjws;

public class Environment<T> {

    protected String id;
    protected String name;
    protected String uiHost;
    protected String svcHost;
    protected T envType;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getUIHost() {
        return uiHost;
    }

    public void setUIHost(String value) {
        this.uiHost = value;
    }

    public String getSVCHost() {
        return svcHost;
    }

    public void setSVCHost(String value) {
        this.svcHost = value;
    }

    public T getEnvType() {
        return envType;
    }

    public void setEnvType(T value) {
        this.envType = value;
    }

    public Environment<T> withUIHost(String value) {
        setUIHost(value);
        return this;
    }

    public Environment<T> withSVCHost(String value) {
        setSVCHost(value);
        return this;
    }

    public Environment<T> withEnvType(T value) {
        setEnvType(value);
        return this;
    }

    public Environment<T> withId(String value) {
        setId(value);
        return this;
    }

    public Environment<T> withName(String value) {
        setName(value);
        return this;
    }

}
