package cpw.mods.fml.common;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String modid();
    String name() default "";
    String version() default "";
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)  @interface Instance  { String value() default ""; }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface Init     {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface PreInit  {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface PostInit {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface EventHandler {}
}
