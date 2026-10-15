package org.l2jmobius.gameserver.mechanics.events.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.l2jmobius.gameserver.mechanics.events.EventType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RegisterEvent {
   EventType value();
}
