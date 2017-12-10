package com.dlsc.preferencesfx.formsfx.view.renderer;

import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.view.util.ViewMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class PreferencesFormRenderer extends GridPane implements ViewMixin {

  private Form form;
  private List<PreferencesGroupRenderer> groups = new ArrayList<>();

  /**
   * This is the constructor to pass over data.
   *
   * @param form The form which gets rendered.
   */
  public PreferencesFormRenderer(Form form) {
    this.form = form;
    init();
  }

  @Override
  public String getUserAgentStylesheet() {
    return PreferencesFormRenderer.class.getResource("style.css").toExternalForm();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initializeParts() {
    groups = form.getGroups().stream().map(
        g -> new PreferencesGroupRenderer((PreferencesGroup) g, this)).collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void layoutParts() {
    // Outer Padding of Category Pane
    setPadding(new Insets(10));
    getChildren().addAll(groups);
  }
}
