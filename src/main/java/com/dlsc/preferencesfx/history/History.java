package com.dlsc.preferencesfx.history;

import com.dlsc.preferencesfx.Setting;
import java.util.HashMap;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by François Martin on 02.12.17.
 */
public class History {
  // TODO: MultiSelection doesn't fire change events?

  private static final Logger LOGGER =
      LogManager.getLogger(History.class.getName());

  private ObservableList<Change> changes = FXCollections.observableArrayList();
  private SimpleObjectProperty<Change> currentChange = new SimpleObjectProperty<>();
  private IntegerProperty position = new SimpleIntegerProperty(-1);
  private IntegerProperty validPosition = new SimpleIntegerProperty(-1);
  private BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
  private BooleanProperty redoAvailable = new SimpleBooleanProperty(false);

  private HashMap<Setting, ChangeListener> settingChangeListenerMap = new HashMap<>();
  private HashMap<Setting, ListChangeListener> settingListChangeListenerMap = new HashMap<>();

  public History() {
    setupBindings();
  }

  private void setupBindings() {
    undoAvailable.bind(position.greaterThanOrEqualTo(0));
    redoAvailable.bind(position.lessThan(validPosition));
    currentChange.bind(Bindings.createObjectBinding(() -> {
      int index = position.get();
      if (index >= 0 && index < changes.size()) {
        LOGGER.trace("Set item");
        return changes.get(index);
      }
      return null;
    }, position));
  }

  public void attachChangeListener(Setting setting) {
    ChangeListener changeEvent = (observable, oldValue, newValue) -> {
      if (oldValue != newValue) {
        LOGGER.trace("Change detected, old: " + oldValue + " new: " + newValue);
        addChange(new Change(setting, oldValue, newValue));
      }
    };
    ChangeListener listChangeEvent = (observable, oldValue, newValue) -> {
        LOGGER.trace("List Change detected: " + oldValue);
        addChange(new Change(setting, oldValue));
      }
    };
    setting.valueProperty() instanceof SimpleListProperty

    setting.valueProperty().addListener(changeEvent);
    settingChangeListenerMap.put(setting, changeEvent);
  }

  private void addChange(Change change) {
    LOGGER.trace("addChange, before, size: " + changes.size() + " pos: " + position.get() + " validPos: " + validPosition.get());

    int lastIndex = changes.size() - 1;

    // check if change is on same setting as the last change => compounded change
    boolean compounded = changes.size() > 0 && position.get() != -1 &&
        changes.get(position.get()).getSetting().equals(change.getSetting());

    // check if the last added change has the same new and old value
    boolean redundant = changes.size() > 0 && position.get() != -1 &&
        changes.get(position.get()).isRedundant();

    // if there is an element in the next position already => overwrite it instead of adding
    boolean elementExists = position.get() < lastIndex;

    if (compounded) {
      LOGGER.trace("Compounded change");
      changes.get(position.get()).setNewValue(change.getNewValue());
    } else if (redundant) {
      LOGGER.trace("Redundant");
      changes.set(position.get(), change);
    } else if (elementExists) {
      LOGGER.trace("Element exists");
      changes.set(incrementPosition(), change);
    } else {
      LOGGER.trace("Add new");
      changes.add(change);
      incrementPosition();
    }

    lastIndex = changes.size() - 1;
    // if there are changes after the currently added item
    if (position.get() != lastIndex) {
      // invalidate all further changes in the list
      LOGGER.trace("Invalidate rest");
      changes.remove(position.get() + 1, changes.size());
    }

    // the last valid position is now equal to the current position
    validPosition.setValue(position.get());

    LOGGER.trace("addChange, after, size: " + changes.size() + " pos: " + position.get() + " validPos: " + validPosition.get());
  }

  private void addChange(ListChange change) {
    LOGGER.trace("List addChange, before, size: " + changes.size() + " pos: " + position.get() + " validPos: " + validPosition.get());

    LOGGER.trace("Add new");
    changes.add(change);
    incrementPosition();

    int lastIndex = changes.size() - 1;
    // if there are changes after the currently added item
    if (position.get() != lastIndex) {
      // invalidate all further changes in the list
      LOGGER.trace("Invalidate rest");
      changes.remove(position.get() + 1, changes.size());
    }

    // the last valid position is now equal to the current position
    validPosition.setValue(position.get());

    LOGGER.trace("List addChange, after, size: " + changes.size() + " pos: " + position.get() + " validPos: " + validPosition.get());
  }

  public void doWithoutListeners(Setting setting, Runnable action) {
    ListChangeListener listChangeListener = settingListChangeListenerMap.get(setting);
    ChangeListener changeListener = settingChangeListenerMap.get(setting);
    if (listChangeListener != null) {
      ((SimpleListProperty) setting.valueProperty()).removeListener(listChangeListener);
    } else if (changeListener != null) {
      setting.valueProperty().removeListener(changeListener);
    }
    action.run();
    if (listChangeListener != null) {
      ((SimpleListProperty) setting.valueProperty()).addListener(listChangeListener);
    } else if (changeListener != null) {
      setting.valueProperty().addListener(changeListener);
    }
  }

  public boolean undo() {
    LOGGER.trace("undo, before, size: " + changes.size() + " pos: " + position.get() + " validPos: " + validPosition.get());
    Change lastChange = prev();
    if (lastChange != null) {
      doWithoutListeners(lastChange.getSetting(), lastChange::undo);
      LOGGER.trace("undo, after, size: " + changes.size() + " pos: " + position.get() + " validPos: " + validPosition.get());
      return true;
    }
    return false;
  }

  public boolean redo() {
    LOGGER.trace("redo, before, size: " + changes.size() + " pos: " + position.get() + " validPos: " + validPosition.get());
    Change nextChange = next();
    if (nextChange != null) {
      doWithoutListeners(nextChange.getSetting(), nextChange::redo);
      LOGGER.trace("redo, after, size: " + changes.size() + " pos: " + position.get() + " validPos: " + validPosition.get());
      return true;
    }
    return false;
  }

  private Change next() {
    if (hasNext()) {
      return changes.get(incrementPosition());
    }
    return null;
  }

  private boolean hasNext() {
    return redoAvailable.get();
  }

  private Change prev() {
    if (hasPrev()) {
      return changes.get(decrementPosition());
    }
    return null;
  }

  private boolean hasPrev() {
    return undoAvailable.get();
  }

  /**
   * Equals to the same as: "return ++position" if position was an Integer.
   *
   * @return the position value before the incrementation
   */
  private int incrementPosition() {
    int positionValue = position.get() + 1;
    position.setValue(positionValue);
    return positionValue;
  }

  /**
   * Equals to the same as: "return position--" if position was an Integer.
   *
   * @return the position value before the decrementation
   */
  private int decrementPosition() {
    int positionValue = position.get();
    position.setValue(positionValue - 1);
    return positionValue;
  }

  public boolean isUndoAvailable() {
    return undoAvailable.get();
  }

  public BooleanProperty undoAvailableProperty() {
    return undoAvailable;
  }

  public boolean isRedoAvailable() {
    return redoAvailable.get();
  }

  public BooleanProperty redoAvailableProperty() {
    return redoAvailable;
  }

  public ObservableList<Change> getChanges() {
    return changes;
  }

  public ReadOnlyObjectProperty<Change> currentChangeProperty() {
    return currentChange;
  }
}
