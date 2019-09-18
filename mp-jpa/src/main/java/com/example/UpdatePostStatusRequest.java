package com.example;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

public class UpdatePostStatusRequest implements Serializable {

    public static UpdatePostStatusRequest of(String status) {
        UpdatePostStatusRequest req = new UpdatePostStatusRequest();
        req.setStatus(status);

        return req;
    }

    @NotEmpty
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
