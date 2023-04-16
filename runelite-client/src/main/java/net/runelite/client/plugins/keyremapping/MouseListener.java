package net.runelite.client.plugins.keyremapping;

import lombok.SneakyThrows;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class MouseListener implements net.runelite.client.input.MouseListener {
    public static final int MOUSE_BUTTON_4_CODE = 4;
    public static final int MOUSE_BUTTON_5_CODE = 5;

    @Inject
    private KeyRemappingConfig config;

    @Override
    public MouseEvent mouseClicked(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @SneakyThrows
    @Override
    public MouseEvent mousePressed(MouseEvent mouseEvent) {
        if (!config.mouseRemap()) {
            return mouseEvent;
        }

        Robot robot = new Robot();
        if (mouseEvent.getButton() == MOUSE_BUTTON_4_CODE) {
            var keyToPress = config.forward().getKeyCode() != 0
                    ? config.forward().getKeyCode()
                    : config.forward().getModifiers();
            robot.keyPress(ModifierTransformer.transform(keyToPress));
            return null;
        } else if (mouseEvent.getButton() == MOUSE_BUTTON_5_CODE) {
            var keyToPress = config.back().getKeyCode() != 0
                    ? config.forward().getKeyCode()
                    : config.forward().getModifiers();
            robot.keyPress(ModifierTransformer.transform(keyToPress));
            return null;
        } else {
            return mouseEvent;
        }
    }

    @SneakyThrows
    @Override
    public MouseEvent mouseReleased(MouseEvent mouseEvent) {
        if (!config.mouseRemap()) {
            return mouseEvent;
        }

        Robot robot = new Robot();
        if (mouseEvent.getButton() == MOUSE_BUTTON_4_CODE) {
            var keyToPress = config.forward().getKeyCode() != 0
                    ? config.forward().getKeyCode()
                    : config.forward().getModifiers();
            robot.keyRelease(ModifierTransformer.transform(keyToPress));
            return null;
        } else if (mouseEvent.getButton() == MOUSE_BUTTON_5_CODE) {
            var keyToPress = config.back().getKeyCode() != 0
                    ? config.forward().getKeyCode()
                    : config.forward().getModifiers();
            robot.keyRelease(ModifierTransformer.transform(keyToPress));
            return null;
        } else {
            return mouseEvent;
        }
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent mouseEvent) {
        return mouseEvent;
    }
}
