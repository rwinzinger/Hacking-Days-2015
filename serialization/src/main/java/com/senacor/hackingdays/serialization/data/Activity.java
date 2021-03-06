package com.senacor.hackingdays.serialization.data;

import com.senacor.hackingdays.serialization.data.unsafe.BufferTooSmallException;
import com.senacor.hackingdays.serialization.data.unsafe.UnsafeMemory;
import com.senacor.hackingdays.serialization.data.unsafe.UnsafeSerializable;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public class Activity implements Serializable, UnsafeSerializable {

    private static final long serialVersionUID = 1;

    private final Date lastLogin;
    private final int loginCount;

    public Activity(
            @JsonProperty("lastLogin") Date lastLogin,
            @JsonProperty("loginCount") int loginCount) {
        this.lastLogin = lastLogin;
        this.loginCount = loginCount;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public int getLoginCount() {
        return loginCount;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "lastLogin=" + lastLogin +
                ", loginCount=" + loginCount +
                '}';
    }

  @Override
  public void serializeUnsafe(UnsafeMemory memory) throws BufferTooSmallException {
    memory.putLong(lastLogin.getTime());
    memory.putInt(loginCount);
  }

  public static Activity deserializeUnsafe(UnsafeMemory memory) {
    final long lastLoginUnixTime = memory.getLong();
    final Date lastLogin = new Date(lastLoginUnixTime);
    final int loginCount = memory.getInt();
    return new Activity(lastLogin, loginCount);
  }
}
