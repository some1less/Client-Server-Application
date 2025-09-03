package common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class PlaceHolder extends JTextField {
    private String placeholder;
    private Color placeholderColor = Color.GRAY; // Customize placeholder text color

    public PlaceHolder(String placeholder) {
        this.placeholder = placeholder;
        setForeground(Color.BLACK); // Regular text color
        setPlaceholderBehavior();
    }

    private void setPlaceholderBehavior() {
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (getText().equals(placeholder)) {
                    setText("");
                    setForeground(Color.BLACK); // Change to input text color
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    setPlaceholderText();
                }
            }
        });
        setPlaceholderText(); // Initialize placeholder on creation
    }

    private void setPlaceholderText() {
        setText(placeholder);
        setForeground(placeholderColor); // Use placeholder color
    }
}
