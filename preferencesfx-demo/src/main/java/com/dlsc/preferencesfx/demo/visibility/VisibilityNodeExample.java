package com.dlsc.preferencesfx.demo.visibility;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.dlsc.preferencesfx.util.VisibilityProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.StackPane;

public class VisibilityNodeExample extends StackPane {

  public PreferencesFx preferencesFx;

  IntegerProperty brightness = new SimpleIntegerProperty(50);

  IntegerProperty scale = new SimpleIntegerProperty(50);

  BooleanProperty nightMode = new SimpleBooleanProperty(true);

  public VisibilityNodeExample() {
    preferencesFx = createPreferences();
    getChildren().add(new VisibilityNodeView(preferencesFx, this));
  }

  private PreferencesFx createPreferences() {
    return PreferencesFx.of(VisibilityNodeExample.class,
        Category.of("General",
            Group.of("Display",
                Setting.of("Brightness", brightness),
                Setting.of("Night mode", nightMode, VisibilityProperty.of(brightness, (newValue) -> newValue.intValue() > 50)),
                Setting.of("Scale", scale, VisibilityProperty.of(nightMode, (newValue) -> newValue))
            )
        ),
        Category.of("HiddenCategory",
                VisibilityProperty.of(brightness, (newValue) -> newValue.intValue() > 50),
            Group.of("Display",
                Setting.of("Scale", scale, VisibilityProperty.of(nightMode, (newValue) -> newValue))
            )

        )
    ).persistWindowState(false).saveSettings(true).debugHistoryMode(false).buttonsVisibility(true);
  }
}
