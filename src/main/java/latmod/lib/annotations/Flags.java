package latmod.lib.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by LatvianModder on 26.03.2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface Flags {

    byte[] value();

    /**
     * Will be synced with client
     */
    byte SYNC = 0;

    /**
     * Will be hidden from config gui
     */
    byte HIDDEN = 1;

    /**
     * Will be visible in config gui, but uneditable
     */
    byte CANT_EDIT = 2;

    /**
     * Can add new config entries
     */
    byte CAN_ADD = 3;

    /**
     * Will be excluded from writing / reading from files
     */
    byte EXCLUDED = 4;

    /**
     * Used when info is present
     */
    byte HAS_INFO = 5;
}
