package mvc.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)//作用在参数上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {
        String value() default "";
}
