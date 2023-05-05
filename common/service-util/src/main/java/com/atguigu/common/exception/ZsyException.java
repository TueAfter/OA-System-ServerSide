package com.atguigu.common.exception;

import com.atguigu.common.result.ResultCodeEnum;
import io.swagger.models.auth.In;
import lombok.Data;

@Data
public class ZsyException extends RuntimeException{
    Integer code;
    String msg;

    public ZsyException(Integer code,String msg){
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public ZsyException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
        this.msg = resultCodeEnum.getMessage();
    }

    @Override
    public String toString() {
        return "GuliException{" +
                "code=" + code +
                ", message=" + this.getMessage() +
                '}';
    }
}
