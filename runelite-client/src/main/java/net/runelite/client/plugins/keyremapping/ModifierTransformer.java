package net.runelite.client.plugins.keyremapping;

import lombok.experimental.UtilityClass;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@UtilityClass
public class ModifierTransformer {
    public static int transform(int modifierCode) {
        if (modifierCode == InputEvent.SHIFT_DOWN_MASK) {
            return KeyEvent.VK_SHIFT;
        } else if (modifierCode == InputEvent.CTRL_DOWN_MASK) {
            return KeyEvent.VK_CONTROL;
        } else if (modifierCode == InputEvent.ALT_DOWN_MASK) {
            return KeyEvent.VK_ALT;
        } else {
            return modifierCode;
        }
    }
}
