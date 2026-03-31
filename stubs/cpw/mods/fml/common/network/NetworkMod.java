package cpw.mods.fml.common.network;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NetworkMod {
    boolean clientSideRequired() default false;
    boolean serverSideRequired() default false;
    String[] channels() default {};
    Class<?> packetHandler() default void.class;
}
