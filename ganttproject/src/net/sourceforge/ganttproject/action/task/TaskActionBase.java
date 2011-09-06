/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.action.task;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

abstract class TaskActionBase extends GPAction implements TaskSelectionManager.Listener {
    private final TaskManager myTaskManager;
    private List<Task> mySelection;
    private final UIFacade myUIFacade;
    private final TaskSelectionManager mySelectionManager;
    private final GanttTree2 myTree;

    protected TaskActionBase(String name, TaskManager taskManager, TaskSelectionManager selectionManager,
            UIFacade uiFacade, GanttTree2 tree) {
        super(name);
        myTaskManager = taskManager;
        mySelectionManager = selectionManager;
        myUIFacade = uiFacade;
        myTree = tree;
        selectionManager.addSelectionListener(this);
        selectionChanged(selectionManager.getSelectedTasks());
    }

    public void actionPerformed(ActionEvent e) {
        final List<Task> selection = new ArrayList<Task>(mySelection);
        if(askUserPermission(selection)) {
            myUIFacade.getUndoManager().undoableEdit(getLocalizedDescription(), new Runnable() {
                public void run() {
                    try {
                        TaskActionBase.this.run(selection);
                    } catch (Exception e) {
                        getUIFacade().showErrorDialog(e);
                    }
                }
            });
        }
    }

    /**
     * @param selection of tasks for which permission is required 
     * @return true if the operation is accepted by the user */
    protected boolean askUserPermission(List<Task> selection) {
        // Accept operation by default
        return true;
    }

    public void selectionChanged(List<Task> currentSelection) {
        setEnabled(isEnabled(currentSelection));
        mySelection = currentSelection;
    }

    public void userInputConsumerChanged(Object newConsumer) {
    }

    protected TaskManager getTaskManager() {
        return myTaskManager;
    }

    protected TaskSelectionManager getSelectionManager() {
        return mySelectionManager;
    }

    protected UIFacade getUIFacade() {
        return myUIFacade;
    }
    
    protected GanttTree2 getTree() {
        return myTree;
    }

    protected void forwardScheduling() throws TaskDependencyException {
        myTaskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
    }

    protected abstract boolean isEnabled(List<Task> selection);
    protected abstract void run(List<Task> selection) throws Exception ;
}
