package mvc.annotation;
import java.lang.annotation.*;

@Target(ElementType.FIELD)//作用在字段上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    String value() default "";
}
