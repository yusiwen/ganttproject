/*
 * This code is provided under the terms of GPL version 3.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import biz.ganttproject.core.chart.canvas.GraphicPrimitiveContainer;
import biz.ganttproject.core.chart.canvas.GraphicPrimitiveContainer.Rectangle;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.TimeUnitStack;

import net.sourceforge.ganttproject.GanttPreviousStateTask;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.chart.item.TimelineLabelChartItem;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * Controls painting of the Gantt chart
 */
public class ChartModelImpl extends ChartModelBase {

  private List<Task> myVisibleTasks;

  private final TaskRendererImpl2 myTaskRendererImpl;

  private TaskManager taskManager;

  // private boolean isPreviousState = false;

  private int rowHeight = 20;

  private final ColorOption myTaskDefaultColorOption;

  private final GPOptionGroup myTaskDefaultsOptions;

  private Set<Task> myHiddenTasks;

  private List<GanttPreviousStateTask> myBaseline;

  public ChartModelImpl(TaskManager taskManager, TimeUnitStack timeUnitStack, final UIConfiguration projectConfig) {
    super(taskManager, timeUnitStack, projectConfig);
    this.taskManager = taskManager;
    myTaskRendererImpl = new TaskRendererImpl2(this);
    getRenderers().add(myTaskRendererImpl);

    class NewTaskColorOption extends DefaultColorOption implements GP1XOptionConverter {
      private NewTaskColorOption() {
        super("taskDefaultColor", new Color(140, 182, 206));
      }

      @Override
      public String getTagName() {
        return "colors";
      }

      @Override
      public String getAttributeName() {
        return "tasks";
      }

      @Override
      public void loadValue(String legacyValue) {
        loadPersistentValue(legacyValue);
        commit();
      }
    }

    myTaskDefaultColorOption = new NewTaskColorOption();
    myTaskDefaultsOptions = new GPOptionGroup("ganttChartDefaults",
        new GPOption[] { taskManager.getTaskNamePrefixOption(), myTaskDefaultColorOption,
            getTaskManager().getDependencyHardnessOption() });
    myTaskDefaultsOptions.setI18Nkey(
        new OptionsPageBuilder.I18N().getCanonicalOptionLabelKey(getTaskManager().getDependencyHardnessOption()),
        "hardness");
    myTaskDefaultsOptions.setI18Nkey(OptionsPageBuilder.I18N.getCanonicalOptionValueLabelKey("Strong"),
        "hardness.strong");
    myTaskDefaultsOptions.setI18Nkey(OptionsPageBuilder.I18N.getCanonicalOptionValueLabelKey("Rubber"),
        "hardness.rubber");

  }

  @Override
  public void setVisibleTasks(List<Task> visibleTasks) {
    myVisibleTasks = visibleTasks;
  }

  public void setExplicitlyHiddenTasks(Set<Task> hiddenTasks) {
    myHiddenTasks = hiddenTasks;
  }

  @Override
  public ChartItem getChartItemWithCoordinates(int x, int y) {
    ChartItem result = findTaskProgressItem(x, y);
    if (result == null) {
      result = findTaskBoundaryItem(x, y);
    }
    if (result == null) {
      result = super.getChartItemWithCoordinates(x, y);
    }
    return result;
  }

  private ChartItem findTaskProgressItem(int x, int y) {
    ChartItem result = null;
    GraphicPrimitiveContainer.GraphicPrimitive primitive = myTaskRendererImpl.getPrimitiveContainer().getLayer(0).getPrimitive(
        x, 4, y/* - getChartUIConfiguration().getHeaderHeight() */, 0);
    if (primitive instanceof GraphicPrimitiveContainer.Rectangle) {
      GraphicPrimitiveContainer.Rectangle rect = (GraphicPrimitiveContainer.Rectangle) primitive;
      if ("task.progress.end".equals(primitive.getStyle()) && rect.getRightX() >= x - 4 && rect.getRightX() <= x + 4) {
        result = new TaskProgressChartItem((Task) primitive.getModelObject());
      }
    }
    return result;
  }

  public GraphicPrimitiveContainer.GraphicPrimitive getGraphicPrimitive(Object modelObject) {
    for (ChartRendererBase renderer : getRenderers()) {
      GraphicPrimitiveContainer.GraphicPrimitive result = renderer.getPrimitiveContainer().getPrimitive(modelObject);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private ChartItem findTaskBoundaryItem(int x, int y) {
    ChartItem result = null;
    GraphicPrimitiveContainer.GraphicPrimitive primitive = myTaskRendererImpl.getPrimitiveContainer().getPrimitive(x, y);
    if (primitive == null) {
      primitive = myTaskRendererImpl.getPrimitiveContainer().getLayer(1).getPrimitive(x, y);
    }
    if (primitive instanceof GraphicPrimitiveContainer.Rectangle) {
      GraphicPrimitiveContainer.Rectangle rect = (Rectangle) primitive;
      TaskActivity activity = (TaskActivity) primitive.getModelObject();
      if (activity != null) {
        if (activity.isFirst() && rect.myLeftX - 2 <= x && rect.myLeftX + 2 >= x) {
          result = new TaskBoundaryChartItem(activity.getTask(), true);
        }
        if (result == null && activity.isLast() && rect.myLeftX + rect.myWidth - 2 <= x
            && rect.myLeftX + rect.myWidth + 2 >= x) {
          result = new TaskBoundaryChartItem(activity.getTask(), false);
        }
        if (result == null) {
          result = new TaskRegularAreaChartItem(activity.getTask());
        }
      }
    }
    return result;
  }

  // public java.awt.Rectangle getBoundingRectangle(Task task) {
  // java.awt.Rectangle result = null;
  // TaskActivity[] activities = task.getActivities();
  // for (int i = 0; i < activities.length; i++) {
  // GraphicPrimitiveContainer.Rectangle nextRectangle = myTaskRendererImpl
  // .getPrimitive(activities[i]);
  // if (nextRectangle != null) {
  // java.awt.Rectangle nextAwtRectangle = new java.awt.Rectangle(
  // nextRectangle.myLeftX, nextRectangle.myTopY,
  // nextRectangle.myWidth, nextRectangle.myHeight);
  // if (result == null) {
  // result = nextAwtRectangle;
  // } else {
  // result = result.union(nextAwtRectangle);
  // }
  // }
  // }
  // return result;
  // }

  // GraphicPrimitiveContainer.Rectangle[] getTaskActivityRectangles(Task task)
  // {
  // List<Rectangle> result = new ArrayList<Rectangle>();
  // TaskActivity[] activities = task.getActivities();
  // for (int i = 0; i < activities.length; i++) {
  // GraphicPrimitiveContainer.Rectangle nextRectangle = myTaskRendererImpl
  // .getPrimitive(activities[i]);
  // if (nextRectangle!=null) {
  // result.add(nextRectangle);
  // }
  // }
  // return result.toArray(new GraphicPrimitiveContainer.Rectangle[0]);
  // }

  List<Task> getVisibleTasks() {
    return myVisibleTasks == null ? Collections.<Task> emptyList() : myVisibleTasks;
  }

  TaskContainmentHierarchyFacade getTaskContainment() {
    return myTaskManager.getTaskHierarchy();
  }

  @Override
  public int calculateRowHeight() {
    rowHeight = myTaskRendererImpl.calculateRowHeight();
    if (myBaseline != null) {
      rowHeight = rowHeight + 8;
    }
    return rowHeight;
  }

  // @Override
  // protected int getRowCount() {
  // return getTaskManager().getTaskCount();
  // }

  @Override
  public TaskManager getTaskManager() {
    return taskManager;
  }

  int getRowHeight() {
    return rowHeight;
  }

  @Override
  public GPOptionGroup[] getChartOptionGroups() {
    GPOptionGroup[] superGroups = super.getChartOptionGroups();
    GPOptionGroup[] rendererGroups = myTaskRendererImpl.getOptionGroups();
    List<GPOptionGroup> result = new ArrayList<GPOptionGroup>();
    result.add(myTaskDefaultsOptions);
    result.addAll(Arrays.asList(superGroups));
    result.addAll(Arrays.asList(rendererGroups));
    return result.toArray(new GPOptionGroup[result.size()]);
  }

  public ColorOption getTaskDefaultColorOption() {
    return myTaskDefaultColorOption;
  }

  public int setBaseline(List<GanttPreviousStateTask> tasks) {
    myBaseline = tasks;
    return (calculateRowHeight());
  }

  List<GanttPreviousStateTask> getBaseline() {
    return myBaseline;
  }

  @Override
  public ChartModelBase createCopy() {
    ChartModelImpl result = new ChartModelImpl(getTaskManager(), getTimeUnitStack(), getProjectConfig());
    super.setupCopy(result);
    result.setVisibleTasks(getVisibleTasks());
    result.setBaseline(getBaseline());
    return result;
  }

  public boolean isExplicitlyHidden(Task task) {
    return myHiddenTasks == null ? false : myHiddenTasks.contains(task);
  }

  public EnumerationOption getDependencyHardnessOption() {
    return getTaskManager().getDependencyHardnessOption();
  }
}