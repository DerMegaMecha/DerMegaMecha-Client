package com.dermegamecha.launcher.auth;

/** Minecraft session data. The access token is intentionally excluded from toString(). */
public final class SignedInAccount {
    private final String name;
    private final String id;
    private final String accessToken;

    public SignedInAccount(String name, String id, String accessToken) {
        this.name = name;
        this.id = id;
        this.accessToken = accessToken;
    }

    public String name() { return name; }
    public String id() { return id; }
    public String accessToken() { return accessToken; }

    @Override public String toString() { return "SignedInAccount[name=" + name + ", id=" + id + "]"; }
}
